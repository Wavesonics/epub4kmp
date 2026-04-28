package io.documentnode.epub4j.domain

/**
 * Representation of a Book.
 *
 * All resources of a Book (html, css, xml, fonts, images) are represented
 * as Resources. See getResources() for access to these.<br></br>
 * A Book as 3 indexes into these Resources, as per the epub specification.<br></br>
 * <dl>
 * <dt>Spine</dt>
 * <dd>these are the Resources to be shown when a user reads the book from
 * start to finish.</dd>
 * <dt>Table of Contents</dt><dt>
</dt> * <dd>The table of contents. Table of Contents references may be in a
 * different order and contain different Resources than the spine, and often do.
</dd> * <dt>Guide</dt>
 * <dd>The Guide has references to a set of special Resources like the
 * cover page, the Glossary, the copyright page, etc.
</dd></dl> *
 *
 *
 * The complication is that these 3 indexes may and usually do point to
 * different pages.
 * A chapter may be split up in 2 pieces to fit it in to memory. Then the
 * spine will contain both pieces, but the Table of Contents only the first.
 *
 * The Content page may be in the Table of Contents, the Guide, but not
 * in the Spine.
 * Etc.
 *
 *
 *
 * Please see the illustration at: doc/schema.svg
 *
 * @author paul
 * @author jake
 */
class Book {
    /**
     * The collection of all images, chapters, sections, xhtml files,
     * stylesheets, etc that make up the book.
     *
     * @return The collection of all images, chapters, sections, xhtml files,
     * stylesheets, etc that make up the book.
     */
    var resources: Resources = Resources()

    /**
     * The Book's metadata (titles, authors, etc)
     *
     * @return The Book's metadata (titles, authors, etc)
     */
    var metadata: Metadata = Metadata()

    /**
     * The sections of the book that should be shown if a user reads the book
     * from start to finish.
     *
     * @return The Spine
     */
    var spine: Spine = Spine()

    /**
     * The Table of Contents of the book.
     *
     * @return The Table of Contents of the book.
     */
    var tableOfContents: TableOfContents = TableOfContents()

    /**
     * The guide; contains references to special sections of the book like
     * colophon, glossary, etc.
     *
     * @return The guide; contains references to special sections of the book
     * like colophon, glossary, etc.
     */
    val guide: Guide = Guide()
    
    var opfResource: Resource? = null
    
    var ncxResource: Resource? = null

    /**
     * The book's cover image.
     *
     * @return The book's cover image.
     */
    var coverImage: Resource? = null
        set(coverImage) {
            if (coverImage == null) {
                return
            }
            if (!resources.containsByHref(coverImage.href)) {
                resources.add(coverImage)
            }
            field = coverImage
        }

    /**
     * Adds the resource to the table of contents of the book as a child
     * section of the given parentSection
     *
     * @param parentSection
     * @param sectionTitle
     * @param resource
     * @param fragmentId
     * @return The table of contents
     */
    fun addSection(
        parentSection: TOCReference,
        sectionTitle: String?,
        resource: Resource,
        fragmentId: String? = null
    ): TOCReference {
        resources.add(resource)
        if (spine.findFirstResourceById(resource.id) < 0) {
            spine.addSpineReference(SpineReference(resource))
        }
        return parentSection.addChildSection(
            TOCReference(sectionTitle, resource, fragmentId)
        )
    }

    /**
     * Adds a resource to the book's set of resources, table of contents and
     * if there is no resource with the id in the spine also adds it to the spine.
     *
     * @param title
     * @param resource
     * @param fragmentId
     * @return The table of contents
     */
    fun addSection(
        title: String?, resource: Resource, fragmentId: String? = null
    ): TOCReference {
        resources.add(resource)
        val tocReference = tableOfContents
            .addTOCReference(TOCReference(title, resource, fragmentId))
        if (spine.findFirstResourceById(resource.id) < 0) {
            spine.addSpineReference(SpineReference(resource))
        }
        return tocReference
    }

    fun generateSpineFromTableOfContents() {
        val spine = Spine(tableOfContents)

        // in case the tocResource was already found and assigned
        spine.tocResource = this.spine.tocResource

        this.spine = spine
    }


    fun addResource(resource: Resource): Resource {
        return resources.add(resource)
    }

    var coverPage: Resource?
        /**
         * The book's cover page as a Resource.
         * An XHTML document containing a link to the cover image.
         *
         * @return The book's cover page as a Resource
         */
        get() {
            var coverPage = guide.coverPage
            if (coverPage == null) {
                coverPage = spine.getResource(0)
            }
            return coverPage
        }
        set(coverPage) {
            if (coverPage == null) {
                return
            }
            if (!resources.containsByHref(coverPage.href)) {
                resources.add(coverPage)
            }
            guide.coverPage = coverPage
        }


    val title: String
        /**
         * Gets the first non-blank title from the book's metadata.
         *
         * @return the first non-blank title from the book's metadata.
         */
        get() = metadata.firstTitle


    val contents: List<Resource>
        /**
         * All Resources of the Book that can be reached via the Spine, the
         * TableOfContents or the Guide.
         *
         *
         * Consists of a list of "reachable" resources:
         *
         *  * The coverpage
         *  * The resources of the Spine that are not already in the result
         *  * The resources of the Table of Contents that are not already in the
         * result
         *  * The resources of the Guide that are not already in the result
         *
         * To get all html files that make up the epub file use
         * [.getResources]
         * @return All Resources of the Book that can be reached via the Spine,
         * the TableOfContents or the Guide.
         */
        get() {
            val result: MutableMap<String, Resource> = mutableMapOf()
            addToContentsResult(coverPage, result)

            for (spineReference in spine.spineReferences) {
                addToContentsResult(spineReference.resource, result)
            }

            for (resource in tableOfContents.allUniqueResources) {
                addToContentsResult(resource, result)
            }

            for (guideReference in guide.references) {
                addToContentsResult(guideReference.resource, result)
            }

            return ArrayList(result.values)
        }

    companion object {
        private fun addToContentsResult(
            resource: Resource?,
            allReachableResources: MutableMap<String, Resource>
        ) {
            if (
                resource?.href != null &&
                !allReachableResources.containsKey(resource.href)
            ) {
                allReachableResources[resource.href!!] = resource
            }
        }
    }
}

