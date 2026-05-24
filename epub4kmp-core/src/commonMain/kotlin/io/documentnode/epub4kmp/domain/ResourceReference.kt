package io.documentnode.epub4kmp.domain

abstract class ResourceReference(
    open var resource: Resource?
) {
    val resourceId: String?
        get() = resource?.id
}
