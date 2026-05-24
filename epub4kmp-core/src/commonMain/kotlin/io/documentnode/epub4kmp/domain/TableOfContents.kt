package io.documentnode.epub4kmp.domain

/**
 * The table of contents of the book.
 * The TableOfContents is a tree structure at the root it is a list of TOCReferences, each if which may have as children another list of TOCReferences.
 *
 * The table of contents is used by epub as a quick index to chapters and sections within chapters.
 * It may contain duplicate entries, may decide to point not to certain chapters, etc.
 *
 * See the spine for the complete list of sections in the order in which they should be read.
 *
 * @see Spine
 *
 *
 * @author paul
 */
class TableOfContents(
    private var tocReferences: MutableList<TOCReference> = mutableListOf()
) {
    fun getTocReferences(): List<TOCReference> {
        return tocReferences
    }

    fun setTocReferences(tocReferences: MutableList<TOCReference>) {
        this.tocReferences = tocReferences
    }

    /**
     * Calls addTOCReferenceAtLocation after splitting the path using the given pathSeparator.
     *
     * @param resource
     * @param path
     * @param pathSeparator
     * @return the new TOCReference
     */
    /**
     * Calls addTOCReferenceAtLocation after splitting the path using the DEFAULT_PATH_SEPARATOR.
     * @return the new TOCReference
     */
    fun addSection(
        resource: Resource,
        path: String,
        pathSeparator: String = DEFAULT_PATH_SEPARATOR
    ): TOCReference? {
        val pathElements = path.split(pathSeparator).dropLastWhile(String::isEmpty)
        return addSection(resource, pathElements)
    }

    /**
     * Adds the given Resources to the TableOfContents at the location specified by the pathElements.
     *
     * Example:
     * Calling this method with a Resource and new String[] {"chapter1", "paragraph1"} will result in the following:
     *
     *  * a TOCReference with the title "chapter1" at the root level.<br></br>
     * If this TOCReference did not yet exist it will have been created and does not point to any resource
     *  * A TOCReference that has the title "paragraph1". This TOCReference will be the child of TOCReference "chapter1" and
     * will point to the given Resource
     *
     *
     * @param resource
     * @param pathElements
     * @return the new TOCReference
     */
    fun addSection(
        resource: Resource,
        pathElements: List<String>
    ): TOCReference? {
        if (pathElements.isEmpty()) {
            return null
        }
        var result: TOCReference? = null
        var currentTocReferences = this.tocReferences
        for (i in pathElements.indices) {
            val currentTitle = pathElements[i]
            result = findTocReferenceByTitle(currentTitle, currentTocReferences)
            if (result == null) {
                result = TOCReference(currentTitle, null, null)
                currentTocReferences.add(result)
            }
            currentTocReferences = result.children
        }
        result?.resource = resource
        return result
    }

    /**
     * Adds the given Resources to the TableOfContents at the location specified by the pathElements.
     *
     * Example:
     * Calling this method with a Resource and new int[] {0, 0} will result in the following:
     *
     *  * a TOCReference at the root level.<br></br>
     * If this TOCReference did not yet exist it will have been created with a title of "" and does not point to any resource
     *  * A TOCReference that points to the given resource and is a child of the previously created TOCReference.<br></br>
     * If this TOCReference didn't exist yet it will be created and have a title of ""
     *
     *
     * @param resource
     * @param pathElements
     * @return the new TOCReference
     */
    fun addSection(
        resource: Resource,
        pathElements: IntArray,
        sectionTitlePrefix: String,
        sectionNumberSeparator: String
    ): TOCReference? {
        if (pathElements.isEmpty()) return null

        var result: TOCReference? = null
        var currentTocReferences = this.tocReferences
        for (index in pathElements.indices) {
            val currentIndex = pathElements[index]
            result = if(
                currentIndex > 0 &&
                currentIndex < (currentTocReferences.size - 1)
            ) {
                currentTocReferences[currentIndex]
            } else {
                null
            }
            if (result == null) {
                paddTOCReferences(
                    currentTocReferences,
                    pathElements,
                    index,
                    sectionTitlePrefix,
                    sectionNumberSeparator
                )
                result = currentTocReferences[currentIndex]
            }
            currentTocReferences = result.children
        }
        result?.resource = resource
        return result
    }

    private fun paddTOCReferences(
        currentTocReferences: MutableList<TOCReference>,
        pathElements: IntArray,
        pathPos: Int,
        sectionPrefix: String,
        sectionNumberSeparator: String
    ) {
        for (index in currentTocReferences.size..pathElements[pathPos]) {
            val sectionTitle = createSectionTitle(
                pathElements,
                pathPos,
                index,
                sectionPrefix,
                sectionNumberSeparator
            )
            currentTocReferences.add(TOCReference(sectionTitle, null, null))
        }
    }

    private fun createSectionTitle(
        pathElements: IntArray,
        pathPos: Int,
        lastPos: Int,
        sectionPrefix: String,
        sectionNumberSeparator: String
    ): String {
        val title = StringBuilder(sectionPrefix)
        for (i in 0 until pathPos) {
            if (i > 0) {
                title.append(sectionNumberSeparator)
            }
            title.append(pathElements[i] + 1)
        }
        if (pathPos > 0) {
            title.append(sectionNumberSeparator)
        }
        title.append(lastPos + 1)
        return title.toString()
    }

    fun addTOCReference(tocReference: TOCReference): TOCReference {
        tocReferences.add(tocReference)
        return tocReference
    }

    val allUniqueResources: List<Resource>
        /**
         * All unique references (unique by href) in the order in which they are referenced to in the table of contents.
         *
         * @return All unique references (unique by href) in the order in which they are referenced to in the table of contents.
         */
        get() {
            val uniqueHrefs: MutableSet<String?> = mutableSetOf()
            val result: MutableList<Resource> = mutableListOf()
            getAllUniqueResources(uniqueHrefs, result, tocReferences)
            return result
        }

    /**
     * The total number of references in this table of contents.
     *
     * @return The total number of references in this table of contents.
     */
    fun size(): Int {
        return getTotalSize(tocReferences)
    }

    /**
     * The maximum depth of the reference tree
     * @return The maximum depth of the reference tree
     */
    fun calculateDepth(): Int {
        return calculateDepth(tocReferences, 0)
    }

    private fun calculateDepth(
        tocReferences: List<TOCReference>,
        currentDepth: Int
    ): Int {
        var maxChildDepth = 0
        for (tocReference in tocReferences) {
            val childDepth = calculateDepth(tocReference.children, 1)
            if (childDepth > maxChildDepth) {
                maxChildDepth = childDepth
            }
        }
        return currentDepth + maxChildDepth
    }

    companion object {
        const val DEFAULT_PATH_SEPARATOR: String = "/"

        /**
         * Finds the first TOCReference in the given list that has the same title as the given Title.
         *
         * @param title
         * @param tocReferences
         * @return null if not found.
         */
        private fun findTocReferenceByTitle(
            title: String,
            tocReferences: List<TOCReference>
        ): TOCReference? {
            for (tocReference in tocReferences) {
                if (title == tocReference.title) {
                    return tocReference
                }
            }
            return null
        }

        private fun getAllUniqueResources(
            uniqueHrefs: MutableSet<String?>,
            result: MutableList<Resource>,
            tocReferences: List<TOCReference>
        ) {
            for (tocReference in tocReferences) {
                val resource = tocReference.resource
                if (resource != null && !uniqueHrefs.contains(resource.href)) {
                    uniqueHrefs.add(resource.href)
                    result.add(resource)
                }
                getAllUniqueResources(uniqueHrefs, result, tocReference.children)
            }
        }

        private fun getTotalSize(tocReferences: Collection<TOCReference>): Int {
            var result = tocReferences.size
            for (tocReference in tocReferences) {
                result += getTotalSize(tocReference.children)
            }
            return result
        }
    }
}
