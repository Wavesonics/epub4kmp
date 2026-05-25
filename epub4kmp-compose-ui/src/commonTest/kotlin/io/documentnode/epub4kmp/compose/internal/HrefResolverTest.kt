package io.documentnode.epub4kmp.compose.internal

import io.documentnode.epub4kmp.domain.Book
import io.documentnode.epub4kmp.domain.MediaTypes
import io.documentnode.epub4kmp.domain.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

class HrefResolverTest {

  private fun book(vararg paths: String): Pair<Book, Map<String, Resource>> {
    val book = Book()
    val byPath = mutableMapOf<String, Resource>()
    for ((i, path) in paths.withIndex()) {
      val r = Resource("r$i", "<html/>".encodeToByteArray(), path).apply {
        mediaType = MediaTypes.XHTML
      }
      book.resources.add(r)
      byPath[path] = r
    }
    return book to byPath
  }

  // -- Percent-decoding (the bug fix) -----------------------------------------

  @Test fun resolvesPercentEncodedRelativeHref() {
    val (b, byPath) = book("Text/Chapter 1.xhtml", "Text/Chapter 2.xhtml")
    val base = byPath["Text/Chapter 1.xhtml"]!!
    // Chapter 1 links to "Chapter%202.xhtml" — what getAttribute('href')
    // returns when the source is `<a href="Chapter%202.xhtml">`.
    val link = resolveLink(b, "Chapter%202.xhtml", base)
    assertSame(byPath["Text/Chapter 2.xhtml"], link.resource)
    assertNull(link.fragmentId)
  }

  @Test fun resolvesPercentEncodedNonAsciiHref() {
    // e.g. a Spanish or Japanese chapter title: "Capítulo.xhtml"
    val (b, byPath) = book("Capítulo.xhtml")
    // %C3%AD is UTF-8 for 'í'.
    val link = resolveLink(b, "Cap%C3%ADtulo.xhtml")
    assertSame(byPath["Capítulo.xhtml"], link.resource)
  }

  @Test fun decodesFragmentToo() {
    val (b, _) = book("ch.xhtml")
    val link = resolveLink(b, "ch.xhtml#sec%20one")
    assertNotNull(link.resource)
    assertEquals("sec one", link.fragmentId)
  }

  @Test fun percentDecodeLeavesPlainHrefUntouched() {
    val (b, byPath) = book("ch.xhtml")
    val link = resolveLink(b, "ch.xhtml")
    assertSame(byPath["ch.xhtml"], link.resource)
  }

  @Test fun malformedPercentSequenceIsTreatedAsLiteral() {
    // "%ZZ" can't be decoded — should fall through to a literal lookup that
    // doesn't match (rather than throw).
    val (b, _) = book("ch.xhtml")
    val link = resolveLink(b, "%ZZbad.xhtml")
    assertNull(link.resource)
  }

  // -- Existing behaviors we don't want to regress ----------------------------

  @Test fun resolvesRelativeHrefAgainstChapterDir() {
    val (b, byPath) = book("Text/ch1.xhtml", "Text/ch2.xhtml")
    val base = byPath["Text/ch1.xhtml"]!!
    val link = resolveLink(b, "ch2.xhtml", base)
    assertSame(byPath["Text/ch2.xhtml"], link.resource)
  }

  @Test fun resolvesParentRelativeHref() {
    val (b, byPath) = book("OEBPS/Text/ch1.xhtml", "OEBPS/css/book.css")
    val base = byPath["OEBPS/Text/ch1.xhtml"]!!
    val link = resolveLink(b, "../css/book.css", base)
    assertSame(byPath["OEBPS/css/book.css"], link.resource)
  }

  @Test fun pureFragmentStaysOnCurrentChapter() {
    val (b, byPath) = book("ch.xhtml")
    val base = byPath["ch.xhtml"]!!
    val link = resolveLink(b, "#sec2", base)
    assertSame(base, link.resource)
    assertEquals("sec2", link.fragmentId)
  }

  @Test fun externalHttpReturnsNullResource() {
    val (b, _) = book("ch.xhtml")
    val link = resolveLink(b, "https://example.com/foo")
    assertNull(link.resource)
    assertNull(link.fragmentId)
  }

  @Test fun mailtoReturnsNullResource() {
    val (b, _) = book("ch.xhtml")
    val link = resolveLink(b, "mailto:author@example.com")
    assertNull(link.resource)
  }

  @Test fun unresolvableInBookHrefReturnsNullResource() {
    val (b, _) = book("ch1.xhtml")
    val link = resolveLink(b, "missing.xhtml")
    assertNull(link.resource)
  }
}
