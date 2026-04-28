package io.documentnode.epub4j.browsersupport

import io.documentnode.epub4j.domain.Book
import io.documentnode.epub4j.domain.Resource
import io.documentnode.epub4j.util.StringUtil.equals
import io.documentnode.epub4j.util.StringUtil.toString

/**
 * Captures a single navigation action (book / spine / resource / fragment change)
 * for delivery to a [NavigationEventListener].
 */
class NavigationEvent(
    val source: Any?,
    private val navigator: Navigator? = null
) {
    var oldResource: Resource? = navigator?.currentResource
    private var oldSpinePos: Int = navigator?.currentSpinePos ?: 0
    private var oldBook: Book? = navigator?.book
    private var oldSectionPos: Int = navigator?.currentSectionPos ?: 0
    private var oldFragmentId: String? = navigator?.currentFragmentId

    fun setOldPagePos(oldPagePos: Int) {
        this.oldSectionPos = oldPagePos
    }

    private val currentSectionPos: Int get() = navigator?.currentSectionPos ?: 0
    private val currentSpinePos: Int get() = navigator?.currentSpinePos ?: 0
    private val currentFragmentId: String get() = navigator?.currentFragmentId ?: ""

    val isBookChanged: Boolean
        get() = oldBook == null || oldBook !== navigator?.book

    val isSpinePosChanged: Boolean
        get() = oldSpinePos != currentSpinePos

    val isFragmentChanged: Boolean
        get() = equals(oldFragmentId, currentFragmentId)

    val currentResource: Resource? get() = navigator?.currentResource

    val currentBook: Book? get() = navigator?.book

    val isResourceChanged: Boolean
        get() = oldResource !== currentResource

    val isSectionPosChanged: Boolean
        get() = oldSectionPos != currentSectionPos

    override fun toString(): String = toString(
        "oldSectionPos", oldSectionPos,
        "oldResource", oldResource,
        "oldBook", oldBook,
        "oldFragmentId", oldFragmentId,
        "oldSpinePos", oldSpinePos,
        "currentPagePos", currentSectionPos,
        "currentResource", currentResource,
        "currentBook", currentBook,
        "currentFragmentId", currentFragmentId,
        "currentSpinePos", currentSpinePos
    )
}
