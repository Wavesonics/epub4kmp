package io.documentnode.epub4j.domain

/**
 * The guide is a selection of special pages of the book.
 * Examples of these are the cover, list of illustrations, etc.
 *
 * It is an optional part of an epub, and support for the various types
 * of references varies by reader.
 *
 * The only part of this that is heavily used is the cover page.
 *
 * @author paul
 */
class Guide {
    internal var references: MutableList<GuideReference> = mutableListOf()
    private var coverPageIndex = -1

    fun getReferences(): List<GuideReference> {
        return references
    }

    fun setReferences(references: MutableList<GuideReference>) {
        this.references = references
        uncheckCoverPage()
    }

    private fun uncheckCoverPage() {
        coverPageIndex = COVERPAGE_UNITIALIZED
    }

    val coverReference: GuideReference?
        get() {
            checkCoverPage()
            if (coverPageIndex >= 0) {
                return references[coverPageIndex]
            }
            return null
        }

    fun setCoverReference(guideReference: GuideReference): Int {
        if (coverPageIndex >= 0) {
            references[coverPageIndex] = guideReference
        } else {
            references.add(0, guideReference)
            coverPageIndex = 0
        }
        return coverPageIndex
    }

    private fun checkCoverPage() {
        if (coverPageIndex == COVERPAGE_UNITIALIZED) {
            initCoverPage()
        }
    }


    private fun initCoverPage() {
        var result = COVERPAGE_NOT_FOUND
        for (i in references.indices) {
            val guideReference = references[i]
            if (guideReference.type == GuideReference.COVER) {
                result = i
                break
            }
        }
        coverPageIndex = result
    }

    var coverPage: Resource?
        /**
         * The coverpage of the book.
         *
         * @return The coverpage of the book.
         */
        get() {
            val guideReference = coverReference ?: return null
            return guideReference.resource
        }
        set(coverPage) {
            val coverpageGuideReference = GuideReference(
                coverPage,
                GuideReference.COVER, DEFAULT_COVER_TITLE
            )
            setCoverReference(coverpageGuideReference)
        }


    fun addReference(reference: GuideReference): ResourceReference {
        references.add(reference)
        uncheckCoverPage()
        return reference
    }

    /**
     * A list of all GuideReferences that have the given
     * referenceTypeName (ignoring case).
     *
     * @param referenceTypeName
     * @return A list of all GuideReferences that have the given
     * referenceTypeName (ignoring case).
     */
    fun getGuideReferencesByType(
        referenceTypeName: String
    ): List<GuideReference> {
        return references.filter {
            it.type.equals(referenceTypeName, ignoreCase = true)
        }
    }

    companion object {
        val DEFAULT_COVER_TITLE: String = GuideReference.COVER

        private const val COVERPAGE_NOT_FOUND = -1
        private const val COVERPAGE_UNITIALIZED = -2
    }
}
