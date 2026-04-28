package io.documentnode.epub4j.domain

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * A Book's identifier.
 *
 * Defaults to a random UUID and scheme "UUID"
 */
class Identifier @OptIn(ExperimentalUuidApi::class) constructor(
    val scheme: String = Scheme.UUID,
    val value: String = Uuid.random().toString()
) {
    interface Scheme {
        companion object {
            const val UUID: String = "UUID"
            const val ISBN: String = "ISBN"
            const val URL: String = "URL"
            const val URI: String = "URI"
        }
    }

    /**
     * This bookId property allows the book creator to add multiple ids and
     * tell the epubwriter which one to write out as the bookId.
     *
     * The Dublin Core metadata spec allows multiple identifiers for a Book.
     * The epub spec requires exactly one identifier to be marked as the book id.
     *
     * @return whether this is the unique book id.
     */
    var isBookId: Boolean = false

    companion object {
        /**
         * The first identifier for which the bookId is true is made the
         * bookId identifier.
         *
         * If no identifier has bookId == true then the first bookId identifier
         * is written as the primary.
         *
         * @param identifiers
         * @return The first identifier for which the bookId is true is made
         * the bookId identifier.
         */
        fun getBookIdIdentifier(identifiers: List<Identifier>): Identifier? {
            if (identifiers.isEmpty()) {
                return null
            }
            return identifiers.firstOrNull(Identifier::isBookId) ?: identifiers.first()
        }
    }
}
