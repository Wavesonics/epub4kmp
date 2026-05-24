package io.documentnode.epub4kmp

import io.documentnode.epub4kmp.domain.Author
import io.documentnode.epub4kmp.domain.Book
import io.documentnode.epub4kmp.domain.MediaTypes
import io.documentnode.epub4kmp.domain.Resource
import io.documentnode.epub4kmp.epub.EpubReader
import io.documentnode.epub4kmp.epub.EpubWriter
import okio.Buffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class RoundTripTest {

    /**
     * Builds a minimal book in memory, writes it as EPUB to an okio [Buffer],
     * reads it back, and asserts equality on title, author, and resource bytes.
     */
    @Test
    fun roundTripMinimalBook() {
        val chapterHtml = """
            <html><head><title>Chapter 1</title></head>
            <body><h1>Hello, world!</h1><p>From a smoke test.</p></body></html>
        """.trimIndent().encodeToByteArray()

        val original = Book().apply {
            metadata.addTitle("My Test Book")
            metadata.addAuthor(Author("Ada", "Lovelace"))
            metadata.language = "en"
            val ch = Resource("ch1", chapterHtml, "ch1.xhtml")
            ch.mediaType = MediaTypes.XHTML
            addSection("Chapter 1", ch)
        }

        // Write to an okio Buffer (in-memory Sink).
        val buffer = Buffer()
        EpubWriter().write(original, buffer)
        val epubBytes = buffer.readByteArray()

        // Read it back from a fresh Source over the same bytes.
        val readBack = EpubReader().readEpub(Buffer().apply { write(epubBytes) })

        assertEquals(
            "My Test Book",
            readBack.metadata.getTitles().firstOrNull(),
            "title round-trips"
        )
        val authors = readBack.metadata.getAuthors()
        assertEquals(1, authors.size, "exactly one author")
        assertEquals("Ada", authors[0].firstname)
        assertEquals("Lovelace", authors[0].lastname)

        val chapter = readBack.resources.getByHref("ch1.xhtml")
        assertNotNull(chapter, "chapter resource is present")
        assertEquals(MediaTypes.XHTML, chapter.mediaType)
        assertEquals(chapterHtml.decodeToString(), chapter.bytes().decodeToString())

        // Spine should reference the chapter.
        assertEquals(1, readBack.spine.size, "spine has one item")
        assertEquals("ch1.xhtml", readBack.spine.getResource(0)?.href)
    }
}
