package io.documentnode.epub4j.epub

import io.documentnode.epub4j.domain.Author
import io.documentnode.epub4j.domain.Date
import io.documentnode.epub4j.domain.Identifier
import io.documentnode.epub4j.domain.Metadata
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.dom2.Document
import nl.adaptivity.xmlutil.dom2.Element
import nl.adaptivity.xmlutil.dom2.attributes
import nl.adaptivity.xmlutil.dom2.documentElement
import nl.adaptivity.xmlutil.dom2.length
import nl.adaptivity.xmlutil.dom2.ownerDocument
import nl.adaptivity.xmlutil.dom2.textContent

/**
 * Reads the package document metadata.
 *
 * Split out from [PackageDocumentReader] for size.
 */
internal object PackageDocumentMetadataReader : PackageDocumentBase() {
    fun readMetadata(packageDocument: Document): Metadata {
        val result = Metadata()
        val rootElement = packageDocument.documentElement ?: return result
        val metadataElement = DOMUtil.getFirstElementByTagNameNS(
            rootElement,
            NAMESPACE_OPF, OPFTags.metadata
        )
        if (metadataElement == null) {
            println("Package does not contain element " + OPFTags.metadata)
            return result
        }
        result.setTitles(
            DOMUtil.getElementsTextChild(metadataElement, NAMESPACE_DUBLIN_CORE, DCTags.title)
        )
        result.setPublishers(
            DOMUtil.getElementsTextChild(metadataElement, NAMESPACE_DUBLIN_CORE, DCTags.publisher)
        )
        result.setDescriptions(
            DOMUtil.getElementsTextChild(metadataElement, NAMESPACE_DUBLIN_CORE, DCTags.description)
        )
        result.setRights(
            DOMUtil.getElementsTextChild(metadataElement, NAMESPACE_DUBLIN_CORE, DCTags.rights)
        )
        result.setTypes(
            DOMUtil.getElementsTextChild(metadataElement, NAMESPACE_DUBLIN_CORE, DCTags.type)
        )
        result.subjects =
            DOMUtil.getElementsTextChild(metadataElement, NAMESPACE_DUBLIN_CORE, DCTags.subject)
        result.setIdentifiers(readIdentifiers(metadataElement))
        result.setAuthors(readCreators(metadataElement))
        result.setContributors(readContributors(metadataElement))
        result.setDates(readDates(metadataElement))
        result.otherProperties = readOtherProperties(metadataElement)
        result.setMetaAttributes(readMetaProperties(metadataElement))
        val languageTag = DOMUtil.getFirstElementByTagNameNS(
            metadataElement, NAMESPACE_DUBLIN_CORE, DCTags.language
        )
        if (languageTag != null) {
            result.language = DOMUtil.getTextChildrenContent(languageTag)
        }
        return result
    }

    /**
     * Consumes EPUB 3 `<meta property="...">` elements.
     */
    private fun readOtherProperties(metadataElement: Element): Map<QName, String> {
        val result: MutableMap<QName, String> = HashMap()
        val metaTags = metadataElement.getElementsByTagName(OPFTags.meta)
        for (i in 0 until metaTags.length) {
            val element = metaTags.item(i) as? Element ?: continue
            val property = element.attributes.getNamedItem(OPFAttributes.property) ?: continue
            val name = property.getValue()
            val value = element.textContent ?: ""
            result[QName(name)] = value
        }
        return result
    }

    /**
     * Consumes EPUB 2 `<meta name="..." content="...">` elements.
     */
    private fun readMetaProperties(metadataElement: Element): Map<String, String> {
        val metaTags = metadataElement.getElementsByTagName(OPFTags.meta)
        val result = mutableMapOf<String, String>()
        for (i in 0 until metaTags.length) {
            val element = metaTags.item(i) as? Element ?: continue
            val name = element.getAttribute(OPFAttributes.name) ?: continue
            val content = element.getAttribute(OPFAttributes.content) ?: ""
            result[name] = content
        }
        return result
    }

    private fun getBookIdId(document: Document): String? {
        val packageElement = DOMUtil.getFirstElementByTagNameNS(
            document.documentElement ?: return null,
            NAMESPACE_OPF,
            OPFTags.packageTag
        )
        return packageElement?.getAttributeNS(NAMESPACE_OPF, OPFAttributes.uniqueIdentifier)
    }

    private fun readCreators(metadataElement: Element): List<Author> =
        readAuthors(DCTags.creator, metadataElement)

    private fun readContributors(metadataElement: Element): List<Author> =
        readAuthors(DCTags.contributor, metadataElement)

    private fun readAuthors(authorTag: String, metadataElement: Element): List<Author> {
        val elements = metadataElement.getElementsByTagNameNS(NAMESPACE_DUBLIN_CORE, authorTag)
        val out = mutableListOf<Author>()
        for (i in 0 until elements.length) {
            val element = elements.item(i) as? Element ?: continue
            createAuthor(element)?.let(out::add)
        }
        return out
    }

    private fun readDates(metadataElement: Element): List<Date> {
        val elements = metadataElement
            .getElementsByTagNameNS(NAMESPACE_DUBLIN_CORE, DCTags.date)
        val result = ArrayList<Date>(elements.length)
        for (i in 0 until elements.length) {
            val dateElement = elements.item(i) as? Element ?: continue
            try {
                val event = dateElement.getAttributeNS(NAMESPACE_OPF, OPFAttributes.event) ?: ""
                result.add(Date(DOMUtil.getTextChildrenContent(dateElement), event))
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
            }
        }
        return result
    }

    private fun createAuthor(authorElement: Element): Author? {
        val authorString = DOMUtil.getTextChildrenContent(authorElement)
        if (authorString.isBlank()) return null
        val spacePos = authorString.lastIndexOf(' ')
        val result = if (spacePos < 0) {
            Author(authorString)
        } else {
            Author(authorString.substring(0, spacePos), authorString.substring(spacePos + 1))
        }
        result.setRole(authorElement.getAttributeNS(NAMESPACE_OPF, OPFAttributes.role))
        return result
    }

    private fun readIdentifiers(metadataElement: Element): List<Identifier> {
        val identifierElements = metadataElement.getElementsByTagNameNS(
            NAMESPACE_DUBLIN_CORE, DCTags.identifier
        )
        if (identifierElements.length == 0) {
            println("Package does not contain element " + DCTags.identifier)
            return emptyList()
        }
        val bookIdId = getBookIdId(metadataElement.ownerDocument)
        val out = mutableListOf<Identifier>()
        for (i in 0 until identifierElements.length) {
            val element = identifierElements.item(i) as? Element ?: continue
            val schemeName = element.getAttributeNS(NAMESPACE_OPF, DCAttributes.scheme) ?: ""
            val identifierValue = DOMUtil.getTextChildrenContent(element)
            if (identifierValue.isBlank()) continue
            val identifier = Identifier(schemeName, identifierValue)
            if (element.getAttribute("id") == bookIdId) {
                identifier.isBookId = true
            }
            out.add(identifier)
        }
        return out
    }
}
