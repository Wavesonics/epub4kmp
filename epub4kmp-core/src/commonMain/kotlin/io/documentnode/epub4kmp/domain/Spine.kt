package io.documentnode.epub4kmp.domain

/**
 * The spine sections are the sections of the book in the order in which the book should be read.
 *
 * This contrasts with the Table of Contents sections which is an index into the Book's sections.
 *
 * @see TableOfContents
 */
class Spine(
    internal var spineReferences: List<SpineReference>
) {

    var tocResource: Resource? = null

    /**
     * Creates a spine out of all the resources in the table of contents.
     *
     * @param tableOfContents
     */
    constructor(tableOfContents: TableOfContents): this(
        spineReferences = createSpineReferences(
            tableOfContents.allUniqueResources
        )
    )

    constructor(): this(emptyList())

    fun getSpineReferences(): List<SpineReference> {
        return spineReferences
    }

    fun setSpineReferences(spineReferences: List<SpineReference>) {
        this.spineReferences = spineReferences
    }

    /**
     * Gets the resource at the given index.
     * Null if not found.
     *
     * @param index
     * @return the resource at the given index.
     */
    fun getResource(index: Int): Resource? {
        if(index !in spineReferences.indices) {
            return null
        }
        return spineReferences[index].resource
    }

    /**
     * Finds the first resource that has the given resourceId.
     *
     * Null if not found.
     *
     * @param resourceId
     * @return the first resource that has the given resourceId.
     */
    fun findFirstResourceById(resourceId: String?): Int {
        if (resourceId.isNullOrBlank()) {
            return -1
        }

        return spineReferences.indexOfFirst { it.resourceId == resourceId }
    }

    /**
     * Adds the given spineReference to the spine references and returns it.
     *
     * @param spineReference
     * @return the given spineReference
     */
    fun addSpineReference(spineReference: SpineReference): SpineReference {
        spineReferences = spineReferences + spineReference
        return spineReference
    }

    /**
     * Adds the given resource to the spine references and returns it.
     *
     * @return the given spineReference
     */
    fun addResource(resource: Resource): SpineReference {
        return addSpineReference(SpineReference(resource))
    }

    /**
     * The number of elements in the spine.
     *
     * @return The number of elements in the spine.
     */
    val size: Int get() = spineReferences.size

    /**
     * The position within the spine of the given resource.
     *
     * @param currentResource
     * @return something &lt; 0 if not found.
     */
    fun getResourceIndex(currentResource: Resource?): Int {
        return getResourceIndex(currentResource?.href)
    }

    /**
     * The first position within the spine of a resource with the given href.
     *
     * @return something &lt; 0 if not found.
     */
    fun getResourceIndex(resourceHref: String?): Int {
        if(resourceHref.isNullOrBlank()) {
            return -1
        }
        return spineReferences.indexOfFirst { it.resource?.href == resourceHref }
    }

    val isEmpty: Boolean
        /**
         * Whether the spine has any references
         * @return Whether the spine has any references
         */
        get() = spineReferences.isEmpty()

    companion object {
        fun createSpineReferences(
            resources: List<Resource>
        ): List<SpineReference> {
            return resources.map(::SpineReference)
        }
    }
}
