package io.documentnode.epub4kmp.browsersupport

import io.documentnode.epub4kmp.domain.Book
import io.documentnode.epub4kmp.domain.Resource
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * A history of the user's locations with the epub.
 *
 * @author paul.siegmann
 */
@OptIn(ExperimentalTime::class)
class NavigationHistory(private val navigator: Navigator) : NavigationEventListener {
    class Location(var href: String)

    private var lastUpdateTime: Long = 0
    private var locations: MutableList<Location> = mutableListOf()
    var currentPos: Int = -1
        private set
    var currentSize: Int = 0
        private set
    var maxHistorySize: Int = DEFAULT_MAX_HISTORY_SIZE

    /**
     * If the time between a navigation event is less than the historyWaitTime
     * then the new location is not added to the history.
     *
     * When a user is rapidly viewing many pages using the slider we do not
     * want all of them to be added to the history.
     *
     * @return the time we wait before adding the page to the history
     */
    var historyWaitTime: Long = DEFAULT_HISTORY_WAIT_TIME

    init {
        navigator.addNavigationEventListener(this)
        navigator.book?.let { initBook(it) }
    }


    fun initBook(book: Book) {
        locations = mutableListOf()
        currentPos = -1
        currentSize = 0
        if (navigator.currentResource?.href != null) {
            addLocation(navigator.currentResource?.href!!)
        }
    }

    fun addLocation(resource: Resource) {
        resource.href?.let { addLocation(it) }
    }

    /**
     * Adds the location after the current position.
     * If the currentposition is not the end of the list then the elements
     * between the current element and the end of the list will be discarded.
     *
     * Does nothing if the new location matches the current location.
     * <br></br>
     * If this nr of locations becomes larger then the historySize then the
     * first item(s) will be removed.
     *
     * @param location
     */
    fun addLocation(location: Location) {
        // do nothing if the new location matches the current location
        if (locations.isNotEmpty() && location.href == locations[currentPos].href) {
            return
        }
        currentPos++
        if (currentPos != currentSize) {
            locations[currentPos] = location
        } else {
            locations.add(location)
            checkHistorySize()
        }
        currentSize = currentPos + 1
    }

    /**
     * Removes all elements that are too much for the maxHistorySize
     * out of the history.
     */
    private fun checkHistorySize() {
        while (locations.size > maxHistorySize) {
            locations.removeAt(0)
            currentSize--
            currentPos--
        }
    }

    fun addLocation(href: String) {
        addLocation(Location(href))
    }

    private fun getLocationHref(pos: Int): String? {
        if (pos < 0 || pos >= locations.size) {
            return null
        }
        return locations[currentPos].href
    }

    /**
     * Moves the current positions delta positions.
     *
     * move(-1) to go one position back in history.<br></br>
     * move(1) to go one position forward.<br></br>
     *
     * @param delta
     *
     * @return Whether we actually moved. If the requested value is illegal
     * it will return false, true otherwise.
     */
    fun move(delta: Int): Boolean {
        if (
            (currentPos + delta < 0) ||
            (currentPos + delta) >= currentSize
        ) {
            return false
        }
        currentPos += delta
        getLocationHref(currentPos)?.let { navigator.gotoResource(it, this) }
        return true
    }


    /**
     * If this is not the source of the navigationEvent then the addLocation
     * will be called with the href of the currentResource in the navigationEvent.
     */
    override fun navigationPerformed(navigationEvent: NavigationEvent) {
        if (this === navigationEvent.source) {
            return
        }
        if (navigationEvent.currentResource == null) {
            return
        }

        if ((Clock.System.now().toEpochMilliseconds() - this.lastUpdateTime) > historyWaitTime) {
            // if the user scrolled rapidly through the pages then the last page
            // will not be added to the history. We fix that here:
            navigationEvent.oldResource?.let { addLocation(it) }

            navigationEvent.currentResource?.href?.let { addLocation(it) }
        }
        lastUpdateTime = Clock.System.now().toEpochMilliseconds()
    }

    val currentHref: String?
        get() {
            if (currentPos < 0 || currentPos >= locations.size) {
                return null
            }
            return locations[currentPos].href
        }

    companion object {
        const val DEFAULT_MAX_HISTORY_SIZE: Int = 1000
        private const val DEFAULT_HISTORY_WAIT_TIME: Long = 1000
    }
}
