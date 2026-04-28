package io.documentnode.epub4j.util

object CollectionUtil {
    /**
     * Returns the first element of the list, null if the list is null or empty.
     */
    fun <T> first(list: List<T>?): T? = list?.firstOrNull()

    /**
     * Whether the given collection is null or has no elements.
     */
    fun isEmpty(collection: Collection<*>?): Boolean = collection.isNullOrEmpty()
}
