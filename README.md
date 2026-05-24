# epub4kmp

A Kotlin Multiplatform library for reading, writing, and manipulating EPUB files.

A KMP fork of [epub4j-kotlin](https://github.com/documentnode/epub4j) (which itself
forked [epublib](https://github.com/psiegman/epublib)). All Java/JVM-only code in
the fork has been replaced with multiplatform equivalents:

- **Streams** — [okio](https://square.github.io/okio/) `Source` / `Sink`
- **ZIP** — [no.synth:kmp-zip](https://github.com/henrik242/kmp-zip) for writing,
  [okio](https://square.github.io/okio/)'s `FileSystem.openZip()` for reading
- **XML** — [xmlutil](https://github.com/pdvrieze/xmlutil) (DOM2 reads, streaming `XmlWriter` writes)
- **Date/UUID** — [kotlinx-datetime](https://github.com/Kotlin/kotlinx-datetime), `kotlin.uuid.Uuid`

## Supported targets

| Target | Status |
|---|---|
| JVM (incl. Android) | ✅ Tested |
| iosArm64, iosSimulatorArm64 | ✅ Compiles; tests run on a macOS host |
| macOS / JS / wasmJs / Android | Possible — depends on the dependencies above |

## Install

```kotlin
// in your shared module's build.gradle.kts
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("com.darkrockstudios:epub4kmp-core:0.1.0-SNAPSHOT")
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
