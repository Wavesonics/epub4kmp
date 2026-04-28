package io.documentnode.epub4j.epub

import nl.adaptivity.xmlutil.dom2.Document
import nl.adaptivity.xmlutil.dom2.Element
import nl.adaptivity.xmlutil.dom2.Node
import nl.adaptivity.xmlutil.dom2.NodeList
import nl.adaptivity.xmlutil.dom2.NodeType
import nl.adaptivity.xmlutil.dom2.Text
import nl.adaptivity.xmlutil.dom2.childNodes
import nl.adaptivity.xmlutil.dom2.documentElement
import nl.adaptivity.xmlutil.dom2.length
import nl.adaptivity.xmlutil.dom2.nodeType

/**
 * Utility methods for working with the (xmlutil) DOM.
 */
internal object DOMUtil {
    /**
     * First tries to get the attribute value via `getAttributeNS`; if that's empty/null
     * falls back to `getAttribute` without a namespace.
     */
    fun getAttribute(
        element: Element,
        namespace: String,
        attribute: String
    ): String {
        val nsResult = element.getAttributeNS(namespace, attribute)
        if (!nsResult.isNullOrEmpty()) return nsResult
        return element.getAttribute(attribute) ?: ""
    }

    /**
     * Gets all descendant elements of the given parentElement with the given namespace
     * and tagname and returns each one's text content.
     */
    fun getElementsTextChild(
        parentElement: Element,
        namespace: String,
        tagname: String
    ): List<String> {
        val elements = parentElement.getElementsByTagNameNS(namespace, tagname)
        return (0 until elements.length).map { i ->
            getTextChildrenContent(elements.item(i) as Element)
        }
    }

    /**
     * Finds the first element with the given namespace and elementName whose
     * `findAttributeName` equals `findAttributeValue` (case-insensitive), and returns
     * the value of `resultAttributeName` on that element.
     */
    fun getFindAttributeValue(
        document: Document,
        namespace: String,
        elementName: String,
        findAttributeName: String,
        findAttributeValue: String,
        resultAttributeName: String
    ): String? {
        val root = document.documentElement ?: return null
        val metaTags = root.getElementsByTagNameNS(namespace, elementName)
        for (i in 0 until metaTags.length) {
            val element = metaTags.item(i) as? Element ?: continue
            val findValue = element.getAttribute(findAttributeName) ?: ""
            val resultValue = element.getAttribute(resultAttributeName) ?: ""
            if (findAttributeValue.equals(findValue, ignoreCase = true) && resultValue.isNotBlank()) {
                return resultValue
            }
        }
        return null
    }

    /**
     * The first descendant element of [parentElement] with the given namespace and tag name.
     */
    fun getFirstElementByTagNameNS(
        parentElement: Element,
        namespace: String,
        tagName: String
    ): Element? {
        val nodes = parentElement.getElementsByTagNameNS(namespace, tagName)
        if (nodes.length == 0) return null
        return nodes.item(0) as? Element
    }

    /**
     * The contents of all Text nodes that are children of [parentElement], concatenated and trimmed.
     *
     * The text walks all Text children rather than returning `firstChild.data` because some
     * platforms split text into multiple Text nodes (e.g. each Chinese character separately).
     */
    fun getTextChildrenContent(parentElement: Element): String {
        val children: NodeList = parentElement.childNodes
        val builder = StringBuilder()
        for (i in 0 until children.length) {
            val node: Node = children.item(i) ?: continue
            if (node.nodeType == NodeType.TEXT_NODE.value) {
                builder.append((node as Text).getData())
            }
        }
        return builder.toString().trim()
    }
}
