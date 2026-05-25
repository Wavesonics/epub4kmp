package com.darkrockstudios.epub4kmp.cli

import io.documentnode.epub4kmp.domain.Book
import io.documentnode.epub4kmp.domain.MediaTypes
import io.documentnode.epub4kmp.domain.Resource
import io.documentnode.epub4kmp.epub.EpubProcessorSupport
import nl.adaptivity.xmlutil.dom2.*

object EpubToMarkdown {

  fun convert(epubPath: String): String {
    val book = EpubIo.readEpub(epubPath)
    val out = StringBuilder()
    spineHtmlResources(book).forEach { resource ->
      val md = htmlResourceToMarkdown(resource).trim()
      if (md.isNotEmpty()) {
        out.append(md).append("\n\n")
      }
    }
    if (out.isEmpty()) {
      val title = book.title.ifBlank { "Untitled" }
      out.append("# ").append(title).append('\n')
    }
    return out.toString().trimEnd() + "\n"
  }

  private fun spineHtmlResources(book: Book): List<Resource> {
    val refs = book.spine.getSpineReferences()
    val xhtml = MediaTypes.XHTML
    return refs.mapNotNull { ref ->
      val r = ref.resource ?: return@mapNotNull null
      if (r.mediaType == xhtml) r else null
    }
  }

  private fun htmlResourceToMarkdown(resource: Resource): String {
    val raw = resource.asString()
    val sanitized = sanitizeHtmlEntities(raw)
    val doc = try {
      EpubProcessorSupport.parseDocument(sanitized.encodeToByteArray(), resource.inputEncoding)
    } catch (e: Exception) {
      return ""
    }
    val root = doc.documentElement ?: return ""
    val body = firstChildElement(root, "body") ?: root
    val ctx = Ctx()
    walk(body, ctx)
    return ctx.out.toString()
  }

  private fun sanitizeHtmlEntities(src: String): String {
    // Replace common HTML-named entities that XML parsers do not know about.
    return src
      .replace("&nbsp;", "&#160;")
      .replace("&copy;", "&#169;")
      .replace("&reg;", "&#174;")
      .replace("&trade;", "&#8482;")
      .replace("&mdash;", "&#8212;")
      .replace("&ndash;", "&#8211;")
      .replace("&hellip;", "&#8230;")
      .replace("&laquo;", "&#171;")
      .replace("&raquo;", "&#187;")
      .replace("&lsquo;", "&#8216;")
      .replace("&rsquo;", "&#8217;")
      .replace("&ldquo;", "&#8220;")
      .replace("&rdquo;", "&#8221;")
  }

  private fun firstChildElement(parent: Element, name: String): Element? {
    val kids: NodeList = parent.childNodes
    for (i in 0 until kids.length) {
      val el = kids.item(i) as? Element ?: continue
      if (el.localName.equals(name, ignoreCase = true)) return el
      firstChildElement(el, name)?.let { return it }
    }
    return null
  }

  private class Ctx {
    val out = StringBuilder()
    var inPre: Boolean = false
    var listDepth: Int = 0
    val orderedCounters = ArrayDeque<Int>()
  }

  private fun walk(node: Node, ctx: Ctx) {
    val el = node as? Element
    if (el != null) {
      emitElement(el, ctx)
      return
    }
    val text = node as? Text
    if (text != null) {
      val data = text.getData()
      if (ctx.inPre) ctx.out.append(data)
      else ctx.out.append(collapseWhitespace(data))
    }
  }

  private fun emitElement(el: Element, ctx: Ctx) {
    when (el.localName.lowercase()) {
      "h1" -> headingBlock(el, ctx, "# ")
      "h2" -> headingBlock(el, ctx, "## ")
      "h3" -> headingBlock(el, ctx, "### ")
      "h4" -> headingBlock(el, ctx, "#### ")
      "h5" -> headingBlock(el, ctx, "##### ")
      "h6" -> headingBlock(el, ctx, "###### ")
      "p" -> {
        ensureBlankLine(ctx)
        walkChildren(el, ctx)
        ctx.out.append("\n\n")
      }
      "br" -> ctx.out.append("  \n")
      "hr" -> {
        ensureBlankLine(ctx)
        ctx.out.append("---\n\n")
      }
      "strong", "b" -> wrap(el, ctx, "**", "**")
      "em", "i" -> wrap(el, ctx, "*", "*")
      "code" -> if (ctx.inPre) walkChildren(el, ctx) else wrap(el, ctx, "`", "`")
      "pre" -> {
        ensureBlankLine(ctx)
        ctx.out.append("```\n")
        val was = ctx.inPre
        ctx.inPre = true
        walkChildren(el, ctx)
        ctx.inPre = was
        if (!ctx.out.endsWith("\n")) ctx.out.append("\n")
        ctx.out.append("```\n\n")
      }
      "blockquote" -> {
        ensureBlankLine(ctx)
        val inner = StringBuilder()
        val saved = ctx.out
        // Buffer child output, then prefix each line with "> ".
        val tmp = Ctx().also { it.inPre = ctx.inPre }
        walkChildren(el, tmp)
        tmp.out.toString().trim().lines().forEach { line ->
          saved.append("> ").append(line).append('\n')
        }
        saved.append('\n')
      }
      "a" -> {
        val href = el.getAttribute("href") ?: ""
        if (href.isBlank()) {
          walkChildren(el, ctx)
        } else {
          ctx.out.append('[')
          walkChildren(el, ctx)
          ctx.out.append("](").append(href).append(')')
        }
      }
      "img" -> {
        val src = el.getAttribute("src") ?: ""
        val alt = el.getAttribute("alt") ?: ""
        ctx.out.append("![").append(alt).append("](").append(src).append(')')
      }
      "ul" -> listBlock(el, ctx, ordered = false)
      "ol" -> listBlock(el, ctx, ordered = true)
      "li" -> walkChildren(el, ctx) // handled by listBlock
      "head", "title", "meta", "link", "script", "style" -> Unit // skip
      else -> walkChildren(el, ctx)
    }
  }

  private fun headingBlock(el: Element, ctx: Ctx, prefix: String) {
    ensureBlankLine(ctx)
    ctx.out.append(prefix)
    walkChildren(el, ctx)
    ctx.out.append("\n\n")
  }

  private fun wrap(el: Element, ctx: Ctx, open: String, close: String) {
    ctx.out.append(open)
    walkChildren(el, ctx)
    ctx.out.append(close)
  }

  private fun listBlock(el: Element, ctx: Ctx, ordered: Boolean) {
    ensureBlankLine(ctx)
    ctx.listDepth += 1
    if (ordered) ctx.orderedCounters.addLast(0)
    val indent = "  ".repeat(ctx.listDepth - 1)
    val kids: NodeList = el.childNodes
    for (i in 0 until kids.length) {
      val child = kids.item(i) as? Element ?: continue
      if (!child.localName.equals("li", ignoreCase = true)) continue
      val marker = if (ordered) {
        val cur = ctx.orderedCounters.removeLast() + 1
        ctx.orderedCounters.addLast(cur)
        "$cur. "
      } else "- "
      val tmp = Ctx().also {
        it.inPre = ctx.inPre
        it.listDepth = ctx.listDepth
      }
      walkChildren(child, tmp)
      val text = tmp.out.toString().trim()
      val lines = text.lines()
      if (lines.isNotEmpty()) {
        ctx.out.append(indent).append(marker).append(lines.first()).append('\n')
        for (j in 1 until lines.size) {
          ctx.out.append(indent).append("  ").append(lines[j]).append('\n')
        }
      }
    }
    if (ordered) ctx.orderedCounters.removeLast()
    ctx.listDepth -= 1
    ctx.out.append('\n')
  }

  private fun walkChildren(el: Element, ctx: Ctx) {
    val kids: NodeList = el.childNodes
    for (i in 0 until kids.length) {
      val n = kids.item(i) ?: continue
      walk(n, ctx)
    }
  }

  private fun ensureBlankLine(ctx: Ctx) {
    val s = ctx.out
    if (s.isEmpty()) return
    val end = s.length
    val last = s[end - 1]
    if (last != '\n') {
      s.append("\n\n")
    } else if (end < 2 || s[end - 2] != '\n') {
      s.append('\n')
    }
  }

  private fun collapseWhitespace(s: String): String {
    if (s.isEmpty()) return s
    val sb = StringBuilder(s.length)
    var prevSpace = false
    for (c in s) {
      val isWs = c == ' ' || c == '\t' || c == '\n' || c == '\r'
      if (isWs) {
        if (!prevSpace) sb.append(' ')
        prevSpace = true
      } else {
        sb.append(c)
        prevSpace = false
      }
    }
    return sb.toString()
  }
}
