package io.documentnode.epub4j.domain

import io.documentnode.epub4j.Constants

open class TitledResourceReference(
    resource: Resource?,
    var title: String? = null,
    var fragmentId: String? = null
) : ResourceReference(resource) {
    val completeHref: String?
        /**
         * If the fragmentId is blank it returns the resource href, otherwise
         * it returns the resource href + '#' + the fragmentId.
         *
         * @return If the fragmentId is blank it returns the resource href,
         * otherwise it returns the resource href + '#' + the fragmentId.
         */
        get() = if (fragmentId.isNullOrBlank()) {
            resource?.href
        } else {
            resource?.href + Constants.FRAGMENT_SEPARATOR_CHAR + fragmentId
        }

    fun setResource(resource: Resource?, fragmentId: String?) {
        super.resource = resource
        this.fragmentId = fragmentId
    }

    override var resource: Resource?
        get() = super.resource
        /**
         * Sets the resource to the given resource and sets the fragmentId to null.
         */
        set(resource) {
            setResource(resource, null)
        }
}
