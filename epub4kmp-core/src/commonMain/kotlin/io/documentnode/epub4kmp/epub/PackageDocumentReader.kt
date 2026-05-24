package io.documentnode.epub4kmp.epub

import io.documentnode.epub4kmp.Constants
import io.documentnode.epub4kmp.domain.*
import io.documentnode.epub4kmp.util.ResourceUtil
import io.documentnode.epub4kmp.util.percentDecode
import nl.adaptivity.xmlutil.dom2.Document
import nl.adaptivity.xmlutil.dom2.Element
import nl.adaptivity.xmlutil.dom2.documentElement
import nl.adaptivity.xmlutil.dom2.length

/**
 * Reads the OPF package document defined by namespace `http://www.idpf.org/2007/opf`.
 */
object PackageDocumentReader : PackageDocumentBase() {
    private val POSSIBLE_NCX_ITEM_IDS = arrayOf("toc", "ncx", "ncxtoc")

    fun read(
        packageResource: Resource,
        epubReader: EpubReader,
        book: Book,
        resources: Resources
    ) {
        val packageDocument = ResourceUtil.getAsDocument(packageResource)
        val packageHref = packageResource.href ?: return
        val fixedResources = fixHrefs(packageHref, resources)
        readGuide(packageDocument, epubReader, book, fixedResources)

        // Books sometimes use non-identifier ids. We map these here to legal ones.
        val idMapping = mutableMapOf<String, String?>()

        val manifestResources = readManifest(
            packageDocument,
            packageHref,
            epubReader,
            fixedResources,
            idMapping
        )
        book.resources = manifestResources
        readCover(packageDocument, book)
        book.metadata = PackageDocumentMetadataReader.readMetadata(packageDocument)
        book.spine = readSpine(packageDocument, book.resources, idMapping)

        // If we did not find a cover page then we make the first spine item the cover page
        if (book.coverPage == null && book.spine.size > 0) {
            book.coverPage = book.spine.getResource(0)
        }
    }

    /**
     * Reads the manifest containing the resource ids, hrefs and mediatypes.
     */
    private fun readManifest(
        packageDocument: Document,
        @Suppress("UNUSED_PARAMETER") packageHref: String,
        @Suppress("UNUSED_PARAMETER") epubReader: EpubReader,
        resources: Resources,
        idMapping: MutableMap<String, String?>
    ): Resources {
        val rootElement = packageDocument.documentElement ?: return Resources()
        val manifestElement = DOMUtil.getFirstElementByTagNameNS(
            rootElement,
            NAMESPACE_OPF, OPFTags.manifest
        )
        val result = Resources()
        if (manifestElement == null) {
            println("Package document does not contain element " + OPFTags.manifest)
            return result
        }
        val itemElements = manifestElement.getElementsByTagNameNS(NAMESPACE_OPF, OPFTags.item)
        for (i in 0 until itemElements.length) {
            val itemElement = itemElements.item(i) as? Element ?: continue
            val id = DOMUtil.getAttribute(itemElement, NAMESPACE_OPF, OPFAttributes.id)
            val rawHref = DOMUtil.getAttribute(itemElement, NAMESPACE_OPF, OPFAttributes.href)
            val href = rawHref.percentDecode()
            val mediaTypeName = DOMUtil.getAttribute(itemElement, NAMESPACE_OPF, OPFAttributes.media_type)
            val resource = resources.remove(href)
            if (resource == null) {
                println("resource with href '$href' not found")
                continue
            }
            resource.id = id
            val mediaType = MediaTypes.getMediaTypeByName(mediaTypeName)
            if (mediaType != null) resource.mediaType = mediaType
            result.add(resource)
            idMapping[id] = resource.id
        }
        return result
    }

    private fun readGuide(
        packageDocument: Document,
        @Suppress("UNUSED_PARAMETER") epubReader: EpubReader,
        book: Book,
        resources: Resources
    ) {
        val rootElement = packageDocument.documentElement ?: return
        val guideElement = DOMUtil.getFirstElementByTagNameNS(
            rootElement, NAMESPACE_OPF, OPFTags.guide
        ) ?: return
        val guide = book.guide
        val guideReferences = guideElement.getElementsByTagNameNS(NAMESPACE_OPF, OPFTags.reference)
        for (i in 0 until guideReferences.length) {
            val ref = guideReferences.item(i) as? Element ?: continue
            val resourceHref = DOMUtil.getAttribute(ref, NAMESPACE_OPF, OPFAttributes.href)
            if (resourceHref.isBlank()) continue
            val resource = resources.getByHref(
                resourceHref.substringBefore(Constants.FRAGMENT_SEPARATOR_CHAR)
            )
            if (resource == null) {
                println("Guide is referencing resource with href $resourceHref which could not be found")
                continue
            }
            val type = DOMUtil.getAttribute(ref, NAMESPACE_OPF, OPFAttributes.type)
            if (type.isBlank()) {
                println("Guide is referencing resource with href $resourceHref which is missing the 'type' attribute")
                continue
            }
            val title = DOMUtil.getAttribute(ref, NAMESPACE_OPF, OPFAttributes.title)
            if (GuideReference.COVER.equals(type, ignoreCase = true)) continue
            val reference = GuideReference(
                resource,
                type,
                title,
                resourceHref.substringAfter(Constants.FRAGMENT_SEPARATOR_CHAR)
            )
            guide.addReference(reference)
        }
    }

    /**
     * Strips off the package prefixes up to the href of the packageHref.
     */
    fun fixHrefs(packageHref: String, resourcesByHref: Resources): Resources {
        val lastSlashPos = packageHref.lastIndexOf('/')
        if (lastSlashPos < 0) return resourcesByHref
        val result = Resources()
        for (resource in resourcesByHref.all) {
            val href = resource.href
            if (!href.isNullOrBlank() && href.length > lastSlashPos) {
                resource.href = href.substring(lastSlashPos + 1)
            }
            result.add(resource)
        }
        return result
    }

    private fun readSpine(
        packageDocument: Document,
        resources: Resources,
        idMapping: Map<String, String?>
    ): Spine {
        val rootElement = packageDocument.documentElement ?: return Spine()
        val spineElement = DOMUtil.getFirstElementByTagNameNS(
            rootElement, NAMESPACE_OPF, OPFTags.spine
        )
        if (spineElement == null) {
            println("Element ${OPFTags.spine} not found in package document, generating one automatically")
            return generateSpineFromResources(resources)
        }
        val result = Spine()
        val tocResourceId = DOMUtil.getAttribute(spineElement, NAMESPACE_OPF, OPFAttributes.toc)
        result.tocResource = findTableOfContentsResource(tocResourceId, resources)
        val spineNodes = rootElement.getElementsByTagNameNS(NAMESPACE_OPF, OPFTags.itemref)
        val spineReferences = mutableListOf<SpineReference>()
        for (i in 0 until spineNodes.length) {
            val item = spineNodes.item(i) as? Element ?: continue
            val itemref = DOMUtil.getAttribute(item, NAMESPACE_OPF, OPFAttributes.idref)
            if (itemref.isBlank()) {
                println("itemref with missing or empty idref")
                continue
            }
            val id = idMapping.getOrElse(itemref) { itemref }
            val resource = id?.let(resources::getByIdOrHref)
            if (resource == null) {
                println("resource with id '$id' not found")
                continue
            }
            val spineReference = SpineReference(resource)
            val linear = DOMUtil.getAttribute(item, NAMESPACE_OPF, OPFAttributes.linear)
            if (OPFValues.no.equals(linear, ignoreCase = true)) {
                spineReference.linear = false
            }
            spineReferences.add(spineReference)
        }
        result.setSpineReferences(spineReferences)
        return result
    }

    private fun generateSpineFromResources(resources: Resources): Spine {
        val result = Spine()
        val resourceHrefs = resources.allHrefs.sortedBy { it.lowercase() }
        for (resourceHref in resourceHrefs) {
            val resource = resources.getByHref(resourceHref) ?: continue
            when (resource.mediaType) {
                MediaTypes.NCX -> result.tocResource = resource
                MediaTypes.XHTML -> result.addSpineReference(SpineReference(resource))
                else -> {}
            }
        }
        return result
    }

    /**
     * Finds the table-of-contents Resource. Tries the spine `toc` attribute, then
     * common item-ids, then the first NCX-typed resource.
     */
    fun findTableOfContentsResource(tocResourceId: String, resources: Resources): Resource? {
        if (tocResourceId.isNotBlank()) {
            resources.getByIdOrHref(tocResourceId)?.let { return it }
        }

        // First resource with the NCX media type
        resources.findFirstResourceByMediaType(MediaTypes.NCX)?.let { return it }

        for (id in POSSIBLE_NCX_ITEM_IDS) {
            resources.getByIdOrHref(id)?.let { return it }
            resources.getByIdOrHref(id.uppercase())?.let { return it }
        }

        println(
            "Could not find table of contents resource. Tried resource with id '"
                + tocResourceId + "', " + Constants.DEFAULT_TOC_ID + ", "
                + Constants.DEFAULT_TOC_ID.uppercase()
                + " and any NCX resource."
        )
        return null
    }

    /**
     * All resources that have something to do with the cover page or cover image.
     */
    fun findCoverHrefs(packageDocument: Document): Set<String> {
        val result = mutableSetOf<String>()

        val coverResourceId = DOMUtil.getFindAttributeValue(
            packageDocument,
            NAMESPACE_OPF,
            OPFTags.meta,
            OPFAttributes.name,
            OPFValues.meta_cover,
            OPFAttributes.content
        )

        if (!coverResourceId.isNullOrBlank()) {
            val coverHref = DOMUtil.getFindAttributeValue(
                packageDocument,
                NAMESPACE_OPF,
                OPFTags.item,
                OPFAttributes.id,
                coverResourceId,
                OPFAttributes.href
            )
            if (!coverHref.isNullOrBlank()) {
                result.add(coverHref)
            } else {
                result.add(coverResourceId) // maybe the href was put in the cover id attribute
            }
        }
        // Try and find a reference tag with type=cover and a non-blank href
        val coverHref = DOMUtil.getFindAttributeValue(
            packageDocument,
            NAMESPACE_OPF,
            OPFTags.reference,
            OPFAttributes.type,
            OPFValues.reference_cover,
            OPFAttributes.href
        )
        if (!coverHref.isNullOrBlank()) result.add(coverHref)
        return result
    }

    private fun readCover(packageDocument: Document, book: Book) {
        val coverHrefs = findCoverHrefs(packageDocument)
        for (coverHref in coverHrefs) {
            val resource = book.resources.getByHref(coverHref)
            if (resource == null) {
                println("Cover resource $coverHref not found")
                continue
            }
            when {
                resource.mediaType === MediaTypes.XHTML -> book.coverPage = resource
                resource.mediaType?.let(MediaTypes::isBitmapImage) == true -> book.coverImage = resource
            }
        }
    }
}
