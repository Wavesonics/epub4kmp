package io.documentnode.epub4kmp.epub

import io.documentnode.epub4kmp.domain.Book

/**
 * A book processor that combines several other bookprocessors
 *
 * Fixes coverpage/coverimage.
 * Cleans up the XHTML.
 *
 * @author paul.siegmann
 */
class BookProcessorPipeline(
    private var bookProcessors: MutableList<BookProcessor> = mutableListOf()
) :
    BookProcessor {

    override fun processBook(book: Book): Book {
        return bookProcessors.fold(book) { processedBook, bookProcessor ->
            bookProcessor.processBook(processedBook)
        }
    }

    fun addBookProcessor(bookProcessor: BookProcessor) {
        bookProcessors.add(bookProcessor)
    }

    fun addBookProcessors(bookProcessors: List<BookProcessor>) {
        this.bookProcessors.addAll(bookProcessors)
    }


    fun getBookProcessors(): List<BookProcessor> {
        return bookProcessors
    }

    fun setBookProcessingPipeline(
        bookProcessingPipeline: List<BookProcessor>
    ) {
        this.bookProcessors = bookProcessingPipeline.toMutableList()
    }
}
