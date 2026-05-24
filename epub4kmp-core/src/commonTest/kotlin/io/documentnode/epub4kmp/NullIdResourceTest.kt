package io.documentnode.epub4kmp

import io.documentnode.epub4kmp.domain.Book
import io.documentnode.epub4kmp.domain.MediaTypes
import io.documentnode.epub4kmp.domain.Resource
import io.documentnode.epub4kmp.epub.EpubReader
import io.documentnode.epub4kmp.epub.EpubWriter
import okio.Buffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Regression: a Resource added without an id used to be silently dropped from
 * the OPF manifest, so it survived in the ZIP but couldn't be read back. After
 * the Resources.fixResourceId fix, the id is derived from the href and the
 * resource round-trips intact.
 */
class NullIdResourceTest {

    @Test fun resourceWithoutIdGetsAutoAssignedIdAtAdd() {
        val book = Book()
        val r = Resource(data = "x".encodeToByteArray(), href = "ch1.xhtml").apply {
            mediaType = MediaTypes.XHTML
        }
        book.resources.add(r)
        assertNotNull(r.id, "id should be auto-assigned")
        assertTrue(r.id!!.isNotBlank(), "id should not be blank")
    }

    @Test fun nullIdResourceSurvivesRoundTrip() {
        // Note: passing data + href but NO id — used to be silently dropped.
        val chapterBytes = "<html><head/><body><p>hi</p></body></html>".encodeToByteArray()
        val original = Book().apply {
            metadata.addTitle("No-id round trip")
            metadata.language = "en"
            val ch = Resource(data = chapterBytes, href = "ch1.xhtml").apply {
                mediaType = MediaTypes.XHTML
            }
            addSection("Chapter 1", ch)
        }

        val buffer = Buffer()
        EpubWriter().write(original, buffer)
        val read = EpubReader().readEpub(Buffer().apply { write(buffer.readByteArray()) })

        val chapter = read.resources.getByHref("ch1.xhtml")
        assertNotNull(chapter, "null-id chapter should survive round-trip")
        assertEquals(chapterBytes.decodeToString(), chapter.bytes().decodeToString())
        assertEquals(1, read.spine.size, "spine should reference the chapter")
    }
}
