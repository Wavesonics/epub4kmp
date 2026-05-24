package io.documentnode.epub4kmp.domain

/**
 * Represents one of the authors of the book
 */
data class Author(
    val firstname: String = "",
    val lastname: String = "",
    var relator: Relator = Relator.AUTHOR
) {
    fun setRole(code: String?): Relator {
        val result = Relator.byCode(code) ?: Relator.AUTHOR
        this.relator = result
        return result
    }
}
