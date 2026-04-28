package io.documentnode.epub4j.domain

abstract class ResourceReference(
    open var resource: Resource?
) {
    val resourceId: String?
        get() = resource?.id
}
