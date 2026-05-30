# epub4kmp

[![Maven Central](https://img.shields.io/maven-central/v/com.darkrockstudios/epub4kmp-core.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/com.darkrockstudios/epub4kmp-core)

A Kotlin Multiplatform library for reading, writing, and manipulating EPUB files.

A KMP fork of [epub4j-kotlin](https://github.com/documentnode/epub4j) (which itself
forked [epublib](https://github.com/psiegman/epublib)). All Java/JVM-only code in
the fork has been replaced with multiplatform equivalents:

- **Streams** — [okio](https://square.github.io/okio/) `Source` / `Sink`
- **ZIP** — [no.synth:kmp-zip](https://github.com/henrik242/kmp-zip) for writing,
  [okio](https://square.github.io/okio/)'s `FileSystem.openZip()` for reading
- **XML** — [xmlutil](https://github.com/pdvrieze/xmlutil) (DOM2 reads, streaming `XmlWriter` writes)
- **Date/UUID** — [kotlinx-datetime](https://github.com/Kotlin/kotlinx-datetime), `kotlin.uuid.Uuid`

## Try the demo
You can try the eReader sample [in your browser](https://wavesonics.github.io/epub4kmp/).

You can download sample EPUBs [here](https://idpf.github.io/epub3-samples/30/samples.html).

## Supported targets

| Target | Status |
|---|---|
| JVM (incl. Android) | ✅ Tested on Linux, macOS, and Windows |
| iosArm64, iosSimulatorArm64 | ✅ Compiles; simulator tests run on a macOS host |
| macosArm64 | ✅ Compiles |
| linuxX64, linuxArm64 | ✅ Compiles |
| mingwX64 (Windows native) | ✅ Compiles |
| JS / wasmJs / Android native | Not yet enabled — depends on the libraries above |

## Install

```kotlin
// in your shared module's build.gradle.kts
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("com.darkrockstudios:epub4kmp-core:0.1.0")
        }
    }
}
```

> **Pre-release.** The API is stabilizing; expect breaking changes before 1.0.

## Quick start

### Read an EPUB

```kotlin
import io.documentnode.epub4kmp.epub.EpubReader
import okio.FileSystem
import okio.Path.Companion.toPath

val book = EpubReader().readEpub(FileSystem.SYSTEM, "my-book.epub".toPath())

println(book.metadata.firstTitle)
for (author in book.metadata.getAuthors()) {
    println("${author.firstname} ${author.lastname}")
}
for (resource in book.spine.getSpineReferences()) {
    println(resource.resource?.href)
}
```

You can also read from any okio `Source` (e.g. an in-memory `Buffer` or a network
stream):

```kotlin
val book = EpubReader().readEpub(buffer /* okio.Source */)
```

### Write an EPUB

```kotlin
import io.documentnode.epub4kmp.domain.Author
import io.documentnode.epub4kmp.domain.Book
import io.documentnode.epub4kmp.domain.MediaTypes
import io.documentnode.epub4kmp.domain.Resource
import io.documentnode.epub4kmp.epub.EpubWriter
import okio.FileSystem
import okio.Path.Companion.toPath

val book = Book().apply {
    metadata.addTitle("My Book")
    metadata.addAuthor(Author("Ada", "Lovelace"))
    metadata.language = "en"

    val ch1 = Resource(
        id = "ch1",
        data = """
            <html><head><title>Chapter 1</title></head>
            <body><h1>Hello</h1></body></html>
        """.trimIndent().encodeToByteArray(),
        href = "ch1.xhtml",
    ).apply { mediaType = MediaTypes.XHTML }

    addSection("Chapter 1", ch1)
}

FileSystem.SYSTEM.write("output.epub".toPath()) {
    EpubWriter().write(book, this)
}
```

## Styling with CSS

Stylesheets registered via `Book.addStylesheet` are written into the EPUB *and*
auto-linked from every XHTML page at write time — you don't have to inject
`<link>` tags yourself.

### Use a built-in preset

```kotlin
import io.documentnode.epub4kmp.domain.Stylesheets

book.addStylesheet(Stylesheets.defaultReader())
```

`defaultReader()` is an opinionated serif reading style: indented paragraphs
(except the first in each section), generous line-height, chapter breaks
before `<h1>`.

### Build a stylesheet with the DSL

A narrow typed builder covers the common ebook properties (typography,
spacing, page breaks) with a `property(...)` / `raw(...)` escape hatch for
anything else:

```kotlin
import io.documentnode.epub4kmp.domain.stylesheet

book.addStylesheet(stylesheet {
    body {
        fontFamily("Georgia, \"Times New Roman\", serif")
        lineHeight(1.5)
        margin("1em")
    }
    paragraph { textIndent("1.5em"); margin("0") }
    firstParagraph { textIndent("0") }                 // p:first-of-type
    paragraphFirstLine { fontVariant("small-caps") }   // p::first-line
    heading(1) { fontSize("1.8em"); pageBreakBefore("always") }
    blockquote { margin("1em 2em"); fontStyle("italic") }
    raw("@font-face { font-family: 'X'; src: url('fonts/X.ttf'); }")
})
```

### Or pass raw CSS

```kotlin
import io.documentnode.epub4kmp.domain.Stylesheet

book.addStylesheet(Stylesheet(
    // Omit `href` to use Stylesheet.DEFAULT_HREF, a namespaced path that
    // won't collide with stylesheets a real EPUB might already ship at
    // common paths like `styles/book.css`.
    css = """
        body { font-family: serif; line-height: 1.5; }
        p { text-indent: 1.5em; margin: 0; }
    """.trimIndent(),
))
```

### Opting out of auto-linking

The default `EpubWriter` runs a `BookProcessorPipeline` that includes
`StylesheetLinker`. To skip the auto-link step (e.g. you've hand-authored
your own `<link>` tags), pass your own processor:

```kotlin
import io.documentnode.epub4kmp.epub.BookProcessor

EpubWriter(BookProcessor.IDENTITY_BOOKPROCESSOR).write(book, sink)
```

## Rendering EPUBs in Compose (`epub4kmp-compose-ui`)

A sibling module ships a Compose Multiplatform UI layer for displaying an
already-loaded `Book` in a real reader. Targets are a subset of core:
**JVM/Desktop, Android, iOS arm64 + simulator** — native and wasmJs are not
supported. Rendering is delegated to a platform WebView on each target via
[ComposeNativeWebview](https://github.com/kdroidFilter/composeNativeWebView),
so chapter CSS, embedded fonts, and inline images all render at full fidelity.

```kotlin
commonMain.dependencies {
  implementation("com.darkrockstudios:epub4kmp-core:0.1.0")
  implementation("com.darkrockstudios:epub4kmp-compose-ui:0.1.0")
}
```

The batteries-included `EpubReader` composable wires a TOC drawer, prev/next
controls, and a styled chapter surface around a `Book`:

```kotlin
import io.documentnode.epub4kmp.compose.EpubReader
import io.documentnode.epub4kmp.domain.Stylesheets

@Composable
fun ReaderScreen(book: Book) {
  // Optional — gives the rendered chapters a readable serif default.
  remember(book) { book.addStylesheet(Stylesheets.defaultReader()) }
  EpubReader(book = book, modifier = Modifier.fillMaxSize())
}
```

If you want to lay out the chrome yourself, the building blocks (`EpubContent`,
`TableOfContents`, `CoverImage`, `MetadataCard`) are public; `EpubReader` is
just one way to wire them together.

## Lazy loading large books

Pass a list of `MediaType`s to `readEpub` to keep those resources unloaded until
their bytes are first accessed — useful for large books with many images:

```kotlin
val book = EpubReader().readEpub(
    fileSystem = FileSystem.SYSTEM,
    zipPath = "huge-book.epub".toPath(),
    lazyLoadedTypes = listOf(MediaTypes.JPG, MediaTypes.PNG),
)
```

## Building

```bash
./gradlew build         # all targets, runs JVM tests
./gradlew jvmTest       # JVM smoke test
./gradlew iosSimulatorArm64Test   # macOS host only
```

iOS Kotlin/Native compilation works on every host; running the iOS tests
requires a macOS host with Xcode.

## License

Apache License 2.0 — see [LICENSE](LICENSE) and [CREDITS](CREDITS).
