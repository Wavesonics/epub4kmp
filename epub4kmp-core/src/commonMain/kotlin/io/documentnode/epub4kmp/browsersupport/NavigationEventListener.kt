package io.documentnode.epub4kmp.browsersupport

/**
 * Implemented by classes that want to be notified if the user moves to
 * another location in the book.
 *
 * @author paul
 */
interface NavigationEventListener {
    /**
     * Called whenever the user navigates to another position in the book.
     *
     * @param navigationEvent
     */
    fun navigationPerformed(navigationEvent: NavigationEvent)
}