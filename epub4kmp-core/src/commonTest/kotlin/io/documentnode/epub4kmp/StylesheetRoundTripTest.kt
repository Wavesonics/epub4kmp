package io.documentnode.epub4kmp

import io.documentnode.epub4kmp.domain.*
import io.documentnode.epub4kmp.epub.EpubReader
import io.documentnode.epub4kmp.epub.EpubWriter
import okio.Buffer
import kotlin.test.*

class StylesheetRoundTripTest {

    @Test fun stylesheetIsWrittenAndLinkedFromPages() {
        val chapterHtml = """
            <html><head><title>Chapter 1</title></head>
            <body><p>Hi.</p></body></html>
        """.trimIndent().encodeToByteArray()

        val original = Book().apply {
            metadata.addTitle("Styled")
            metadata.language = "en"
            addStylesheet(stylesheet {
                body { fontFamily("serif"); lineHeight(1.5) }
            })
            val ch = Resource("ch1", chapterHtml, "ch1.xhtml").apply {
                mediaType = MediaTypes.XHTML
            }
            addSection("Chapter 1", ch)
        }

        val buffer = Buffer()
        EpubWriter().write(original, buffer)
        val read = EpubReader().readEpub(Buffer().apply { write(buffer.readByteArray()) })

        // CSS resource survived the round-trip.
        val sheet = read.resources.getByHref("styles/book.css")
        assertNotNull(sheet, "stylesheet should be in the read-back book")
        assertEquals(MediaTypes.CSS, sheet.mediaType)
        assertTrue(sheet.asString().contains("font-family: serif"))

        // Chapter now has a <link> pointing at the sheet.
        val chapter = read.resources.getByHref("ch1.xhtml")
        assertNotNull(chapter)
        val chapterStr = chapter.asString()
        assertTrue(
            chapterStr.contains("""href="styles/book.css""""),
            "chapter should have a link tag for the stylesheet, got: $chapterStr",
        )
    }

    @Test fun addStylesheetSameInstanceIsIdempotent() {
        val book = Book()
        val sheet = stylesheet { body { color("black") } }
        val first = book.addStylesheet(sheet)
        val second = book.addStylesheet(sheet)
        assertSame(first, second)
        assertEquals(1, book.stylesheets.size, "href should be tracked once")
    }

    @Test fun addStylesheetConflictingHrefThrows() {
        val book = Book()
        book.addStylesheet(stylesheet { body { color("red") } })
        // Same default href, different instance / different CSS → throw.
        val ex = assertFailsWith<IllegalArgumentException> {
            book.addStylesheet(stylesheet { body { color("blue") } })
        }
        assertTrue(
            ex.message!!.contains("styles/book.css"),
            "error should name the conflicting href, got: ${ex.message}",
        )
    }

    @Test fun defaultReaderPresetWorksEndToEnd() {
        val original = Book().apply {
            metadata.addTitle("Preset")
            metadata.language = "en"
            addStylesheet(Stylesheets.defaultReader())
            val ch = Resource(
                "ch1",
                "<html><head/><body><p>x</p></body></html>".encodeToByteArray(),
                "ch1.xhtml",
            ).apply { mediaType = MediaTypes.XHTML }
            addSection("Chapter 1", ch)
        }
        val buffer = Buffer()
        EpubWriter().write(original, buffer)
        val read = EpubReader().readEpub(Buffer().apply { write(buffer.readByteArray()) })

        val sheet = read.resources.getByHref("styles/book.css")
        assertNotNull(sheet)
        assertTrue(sheet.asString().contains("Georgia"))
    }
}
