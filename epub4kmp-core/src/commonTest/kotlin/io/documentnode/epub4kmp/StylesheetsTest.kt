package io.documentnode.epub4kmp

import io.documentnode.epub4kmp.domain.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StylesheetsTest {

	/**
	 * Regression for the v0.1 paper cut where the obvious one-liner
	 * `book.addStylesheet(Stylesheets.defaultReader())` crashed on any real
	 * EPUB that happened to have its own `styles/book.css`.
	 */
	@Test
	fun defaultReaderDoesNotCollideWithCommonStylesPath() {
		val book = Book().apply {
			resources.add(
				Resource(
					id = "book-css",
					data = "body { color: red; }".encodeToByteArray(),
					href = "styles/book.css",
					mediaType = MediaTypes.CSS,
				)
			)
		}
		// Must not throw.
		book.addStylesheet(Stylesheets.defaultReader())
		assertTrue(book.stylesheets.any { it == Stylesheets.DEFAULT_READER_HREF })
	}

	@Test
	fun defaultReaderHrefIsOverridable() {
		val sheet = Stylesheets.defaultReader(href = "css/my-custom.css")
		assertEquals("css/my-custom.css", sheet.href)
	}

	@Test
	fun stylesheetDefaultHrefIsNamespaced() {
		// Make sure the bare ctor default is also collision-safe.
		val sheet = Stylesheet(css = "body{}")
		assertEquals("styles/__epub4kmp_default.css", sheet.href)
	}
}
