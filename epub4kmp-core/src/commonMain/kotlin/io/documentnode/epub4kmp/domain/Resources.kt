package io.documentnode.epub4kmp.domain

import io.documentnode.epub4kmp.Constants
import io.documentnode.epub4kmp.domain.MediaTypes.isBitmapImage

/**
 * All the resources that make up the book.
 * XHTML files, images and epub xml documents must be here.
 */
class Resources {
    private var lastId = 1

    private var resources: MutableMap<String?, Resource> = mutableMapOf()

    /**
     * Adds a resource to the resources.
     *
     * Fixes the resources id and href if necessary.
     *
     * @param resource
     * @return the newly added resource
     */
    fun add(resource: Resource): Resource {
        fixResourceHref(resource)
        fixResourceId(resource)
        resources[resource.href] = resource
        return resource
    }

    /**
     * Checks the id of the given resource and changes to a unique identifier if it isn't one already.
     *
     * @param resource
     */
    fun fixResourceId(resource: Resource) {
        val href = resource.href ?: return
        // Null id is treated the same as blank — derive one from the href,
        // then fall back to a unique counter-based id if still blank or taken.
        var resourceId = resource.id.orEmpty()

        // first try and create a unique id based on the resource's href
        if (resourceId.isBlank()) {
            resourceId = href
                .substringBeforeLast('.')
                .substringAfterLast('/')
        }

        resourceId = makeValidId(resourceId, resource)

        // check if the id is unique. if not: create one from scratch
        if (resourceId.isBlank() || containsId(resourceId)) {
            resourceId = createUniqueResourceId(resource)
        }
        resource.id = resourceId
    }

    /**
     * Check if the id is a valid identifier. if not: prepend with valid identifier
     *
     * @param resource
     * @return a valid id
     */
    private fun makeValidId(resourceId: String, resource: Resource): String {
        return if (
            resourceId.isNotBlank() &&
            !isValidIdStart(resourceId.first())
        ) {
            getResourceItemPrefix(resource) + resourceId
        } else {
            resourceId
        }
    }

    private fun isValidIdStart(c: Char): Boolean = c.isLetter() || c == '_' || c == '$'

    private fun getResourceItemPrefix(resource: Resource): String {
        val result = if (isBitmapImage(resource.mediaType!!)) {
            IMAGE_PREFIX
        } else {
            ITEM_PREFIX
        }
        return result
    }

    /**
     * Creates a new resource id that is guaranteed to be unique for this set of Resources
     *
     * @param resource
     * @return a new resource id that is guaranteed to be unique for this set of Resources
     */
    private fun createUniqueResourceId(resource: Resource): String {
        var counter = lastId
        if (counter == Int.MAX_VALUE) {
            require(resources.size != Int.MAX_VALUE) { "Resources contains " + Int.MAX_VALUE + " elements: no new elements can be added" }
            counter = 1
        }
        val prefix = getResourceItemPrefix(resource)
        var result = prefix + counter
        while (containsId(result)) {
            result = prefix + (++counter)
        }
        lastId = counter
        return result
    }

    /**
     * Whether the map of resources already contains a resource with the given id.
     *
     * @param id
     * @return Whether the map of resources already contains a resource with the given id.
     */
    private fun containsId(id: String): Boolean {
        if (id.isBlank()) {
            return false
        }
        return resources.values.any { resource -> resource.id == id }
    }

    /**
     * Gets the resource with the given id.
     *
     * @param id
     * @return null if not found
     */
    fun getById(id: String): Resource? {
        return if (id.isBlank()) {
            return null
        } else {
            resources.values.firstOrNull { it.id == id }
        }
    }

    /**
     * Remove the resource with the given href.
     *
     * @param href
     * @return the removed resource, null if not found
     */
    fun remove(href: String?): Resource? {
        return resources.remove(href)
    }

    private fun fixResourceHref(resource: Resource) {
        if (
            resource.href?.isNotBlank() == true
            && !resources.containsKey(resource.href)
        ) return
        if (resource.href.isNullOrBlank()) {
            requireNotNull(resource.mediaType) { "Resource must have either a MediaType or a href" }
            var i = 1
            var href = createHref(resource.mediaType, i)
            while (resources.containsKey(href)) {
                href = createHref(resource.mediaType, ++i)
            }
            resource.href = href
        }
    }

    private fun createHref(mediaType: MediaType?, counter: Int): String {
        return if (isBitmapImage(mediaType!!)) {
            "image_" + counter + mediaType.defaultExtension
        } else {
            "item_" + counter + mediaType.defaultExtension
        }
    }

    val isEmpty: Boolean
        get() = resources.isEmpty()

    /**
     * The number of resources
     * @return The number of resources
     */
    val size: Int
        get() = resources.size

    val resourceMap: Map<String?, Resource>
        /**
         * The resources that make up this book.
         * Resources can be xhtml pages, images, xml documents, etc.
         *
         * @return The resources that make up this book.
         */
        get() = resources

    val all: Collection<Resource>
        get() = resources.values


    /**
     * Whether there exists a resource with the given href
     * @param href
     * @return Whether there exists a resource with the given href
     */
    fun containsByHref(href: String?): Boolean {
        if (href.isNullOrBlank()) {
            return false
        }
        return resources.containsKey(
            href.substringBefore(Constants.FRAGMENT_SEPARATOR_CHAR)
        )
    }

    /**
     * Sets the collection of Resources to the given collection of resources
     *
     * @param resources
     */
    fun set(resources: Collection<Resource>) {
        this.resources.clear()
        addAll(resources)
    }

    /**
     * Adds all resources from the given Collection of resources to the existing collection.
     *
     * @param resources
     */
    fun addAll(resources: Collection<Resource>) = resources.forEach { resource ->
        fixResourceHref(resource)
        this.resources[resource.href] = resource
    }

    /**
     * Sets the collection of Resources to the given collection of resources
     *
     * @param resources A map with as keys the resources href and as values the Resources
     */
    fun set(resources: Map<String?, Resource>) {
        this.resources = resources.toMutableMap()
    }


    /**
     * First tries to find a resource with as id the given idOrHref, if that
     * fails it tries to find one with the idOrHref as href.
     *
     * @param idOrHref
     * @return the found Resource
     */
    fun getByIdOrHref(idOrHref: String): Resource? {
        var resource = getById(idOrHref)
        if (resource == null) {
            resource = getByHref(idOrHref)
        }
        return resource
    }


    /**
     * Gets the resource with the given href.
     * If the given href contains a fragmentId then that fragment id will be ignored.
     *
     * @param href
     * @return null if not found.
     */
    fun getByHref(href: String): Resource? {
        var href = href.takeIf(String::isNotBlank) ?: return null
        href = href.substringBefore(Constants.FRAGMENT_SEPARATOR_CHAR)
        val result = resources[href]
        return result
    }

    /**
     * Gets the first resource (random order) with the give mediatype.
     *
     * Useful for looking up the table of contents as it's supposed to be the only resource with NCX mediatype.
     *
     * @param mediaType
     * @return the first resource (random order) with the give mediatype.
     */
    fun findFirstResourceByMediaType(mediaType: MediaType): Resource? {
        return findFirstResourceByMediaType(resources.values, mediaType)
    }

    /**
     * All resources that have the given MediaType.
     *
     * @param mediaType
     * @return All resources that have the given MediaType.
     */
    fun getResourcesByMediaType(mediaType: MediaType): List<Resource> {
        return all.filter { it.mediaType == mediaType }
    }

    /**
     * All Resources that match any of the given list of MediaTypes
     *
     * @param mediaTypes
     * @return All Resources that match any of the given list of MediaTypes
     */
    fun getResourcesByMediaTypes(mediaTypes: Array<MediaType>): List<Resource> {
        return all.filter { mediaTypes.contains(it.mediaType) }
    }

    val allHrefs: List<String>
        /**
         * All resource hrefs
         *
         * @return all resource hrefs
         */
        get() = resources.keys.filterNotNull()

    companion object {
        private const val IMAGE_PREFIX = "image_"
        private const val ITEM_PREFIX = "item_"

        /**
         * Gets the first resource (random order) with the give mediatype.
         *
         * Useful for looking up the table of contents as it's supposed to be the only resource with NCX mediatype.
         *
         * @param mediaType
         * @return the first resource (random order) with the give mediatype.
         */
        fun findFirstResourceByMediaType(
            resources: Collection<Resource>,
            mediaType: MediaType
        ): Resource? {
            return resources.firstOrNull { it.mediaType == mediaType }
        }
    }
}
