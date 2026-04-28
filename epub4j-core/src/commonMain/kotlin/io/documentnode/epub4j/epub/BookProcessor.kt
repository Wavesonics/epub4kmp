package io.documentnode.epub4j.epub

import io.documentnode.epub4j.domain.Book

/**
 * Post-processes a book.
 *
 * Can be used to clean up a book after reading or before writing.
 *
 * @author paul
 */
interface BookProcessor {
    fun processBook(book: Book): Book

    companion object {
        val IDENTITY_BOOKPROCESSOR = object : BookProcessor {
            override fun processBook(book: Book): Book = book
        }
    }
}
