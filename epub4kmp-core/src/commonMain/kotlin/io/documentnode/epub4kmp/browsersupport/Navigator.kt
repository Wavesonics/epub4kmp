package io.documentnode.epub4kmp.browsersupport

import io.documentnode.epub4kmp.domain.Book
import io.documentnode.epub4kmp.domain.Resource

/**
 * A helper class for EPUB browser applications.
 *
 * Tracks the user's position within a [Book], supports moving between
 * resources, and notifies registered [NavigationEventListener]s.
 *
 * Not thread-safe; protect external access if you call this from multiple
 * coroutines.
 */
class Navigator(var book: Book? = null) {
    internal var currentSpinePos = 0
    var currentResource: Resource? = book?.coverPage
    var currentSectionPos: Int = 0
        private set
    var currentFragmentId: String? = null
        private set

    private val eventListeners: MutableList<NavigationEventListener> = mutableListOf()

    private fun handleEventListeners(navigationEvent: NavigationEvent) {
        for (listener in eventListeners) {
            listener.navigationPerformed(navigationEvent)
        }
    }

    fun addNavigationEventListener(listener: NavigationEventListener): Boolean =
        eventListeners.add(listener)

    fun removeNavigationEventListener(listener: NavigationEventListener): Boolean =
        eventListeners.remove(listener)

    fun gotoFirstSpineSection(source: Any?): Int = gotoSpineSection(0, source)

    fun gotoPreviousSpineSection(source: Any?): Int = gotoPreviousSpineSection(0, source)

    fun gotoPreviousSpineSection(pagePos: Int, source: Any?): Int =
        if (currentSpinePos < 0) gotoSpineSection(0, pagePos, source)
        else gotoSpineSection(currentSpinePos - 1, pagePos, source)

    fun hasNextSpineSection(): Boolean =
        currentSpinePos < (book?.spine?.size?.minus(1) ?: -1)

    fun hasPreviousSpineSection(): Boolean = currentSpinePos > 0

    fun gotoNextSpineSection(source: Any?): Int =
        if (currentSpinePos < 0) gotoSpineSection(0, source)
        else gotoSpineSection(currentSpinePos + 1, source)

    fun gotoResource(resourceHref: String, source: Any): Int {
        val resource = book?.resources?.getByHref(resourceHref)
        return resource?.let { gotoResource(it, source) } ?: -1
    }

    fun gotoResource(resource: Resource, source: Any?): Int =
        gotoResource(resource, 0, null, source)

    fun gotoResource(resource: Resource, fragmentId: String, source: Any?): Int =
        gotoResource(resource, 0, fragmentId, source)

    fun gotoResource(resource: Resource, pagePos: Int, source: Any?): Int =
        gotoResource(resource, pagePos, null, source)

    fun gotoResource(
        resource: Resource,
        pagePos: Int,
        fragmentId: String?,
        source: Any?
    ): Int {
        val navigationEvent = NavigationEvent(source, this)
        this.currentResource = resource
        this.currentSpinePos = book?.spine?.getResourceIndex(resource) ?: 0
        this.currentSectionPos = pagePos
        this.currentFragmentId = fragmentId
        handleEventListeners(navigationEvent)
        return currentSpinePos
    }

    fun gotoResourceId(resourceId: String?, source: Any?): Int {
        val book = book ?: return -1
        return gotoSpineSection(book.spine.findFirstResourceById(resourceId), source)
    }

    fun gotoSpineSection(newSpinePos: Int, source: Any?): Int =
        gotoSpineSection(newSpinePos, 0, source)

    /**
     * Go to a specific section. Illegal spine positions are silently ignored.
     */
    fun gotoSpineSection(newSpinePos: Int, newPagePos: Int, source: Any?): Int {
        if (newSpinePos == currentSpinePos) return currentSpinePos
        if (newSpinePos < 0 || newSpinePos >= (book?.spine?.size ?: 0)) return currentSpinePos
        val navigationEvent = NavigationEvent(source, this)
        currentSpinePos = newSpinePos
        currentSectionPos = newPagePos
        currentResource = book?.spine?.getResource(currentSpinePos)
        handleEventListeners(navigationEvent)
        return currentSpinePos
    }

    fun gotoLastSpineSection(source: Any?): Int =
        gotoSpineSection(book?.spine?.size?.minus(1) ?: 0, source)

    fun gotoBook(book: Book, source: Any?) {
        val navigationEvent = NavigationEvent(source, this)
        this.book = book
        this.currentFragmentId = null
        this.currentSectionPos = 0
        this.currentResource = null
        this.currentSpinePos = -1
        handleEventListeners(navigationEvent)
    }

    /**
     * The current position within the spine, or `< 0` if the current position is not within the spine.
     */
    fun getCurrentSpinePos(): Int = currentSpinePos

    /**
     * Sets the current index and resource without calling the event listeners.
     */
    fun setCurrentSpinePos(currentIndex: Int) {
        this.currentSpinePos = currentIndex
        this.currentResource = book?.spine?.getResource(currentIndex)
    }

    /**
     * Sets the current index and resource without calling the event listeners.
     */
    fun setCurrentResource(currentResource: Resource): Int {
        this.currentSpinePos = book?.spine?.getResourceIndex(currentResource) ?: 0
        this.currentResource = currentResource
        return currentSpinePos
    }
}
