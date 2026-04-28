package io.documentnode.epub4j.domain

/**
 * An item in the Table of Contents.
 *
 * @see TableOfContents
 */
class TOCReference(
    title: String?,
    resource: Resource?,
    fragmentId: String?,
    var children: MutableList<TOCReference> = mutableListOf()
) : TitledResourceReference(resource, title, fragmentId) {

    fun addChildSection(childSection: TOCReference): TOCReference {
        children.add(childSection)
        return childSection
    }

    companion object {
        val comparatorByTitleIgnoreCase: Comparator<TOCReference> =
            compareBy { it.title?.lowercase() ?: "" }
    }
}
