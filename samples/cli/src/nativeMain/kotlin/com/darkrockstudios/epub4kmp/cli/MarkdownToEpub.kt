package com.darkrockstudios.epub4kmp.cli

import io.documentnode.epub4kmp.domain.*
import kotlinx.html.*
import kotlinx.html.stream.appendHTML
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser

object MarkdownToEpub {

  private data class Chunk(val title: String, val markdown: String)

  fun convert(
    markdownPath: String,
    author: String,
    language: String,
    style: String? = null,
  ): Book {
    val source = EpubIo.readText(markdownPath)
    val parsed = splitChapters(source)

    val book = Book().apply {
      metadata.language = language
      metadata.addTitle(parsed.title)
      metadata.addAuthor(parseAuthor(author))
      resolveStylesheet(style)?.let { addStylesheet(it) }
    }

    // Title page: the H1 plus any prologue markdown.
    val titlePageMd = buildString {
      append("# ").append(parsed.title).append("\n\n")
      if (parsed.prologue.isNotBlank()) append(parsed.prologue)
    }
    book.addSection(parsed.title, htmlResource("title", "title.xhtml", parsed.title, titlePageMd))

    parsed.chapters.forEachIndexed { index, chunk ->
      val id = "chapter-${index + 1}"
      val href = "$id.xhtml"
      val chapterMd = "## ${chunk.title}\n\n${chunk.markdown}"
      book.addSection(chunk.title, htmlResource(id, href, chunk.title, chapterMd))
    }
    return book
  }

  private data class ParsedDoc(
    val title: String,
    val prologue: String,
    val chapters: List<Chunk>,
  )

  private fun splitChapters(source: String): ParsedDoc {
    val lines = source.lines()
    var title = "Untitled"
    val prologue = StringBuilder()
    val chapters = mutableListOf<Chunk>()
    var currentChapter: StringBuilder? = null
    var currentChapterTitle: String? = null
    var sawTitle = false

    fun flushChapter() {
      val c = currentChapter
      val t = currentChapterTitle
      if (c != null && t != null) {
        chapters.add(Chunk(t, c.toString().trim()))
      }
      currentChapter = null
      currentChapterTitle = null
    }

    for (line in lines) {
      val h1 = h1Of(line)
      val h2 = h2Of(line)
      when {
        h1 != null && !sawTitle -> {
          title = h1
          sawTitle = true
        }
        h2 != null -> {
          flushChapter()
          currentChapterTitle = h2
          currentChapter = StringBuilder()
        }
        currentChapter != null -> {
          currentChapter!!.append(line).append('\n')
        }
        sawTitle -> {
          prologue.append(line).append('\n')
        }
        // Lines before any H1 are ignored.
      }
    }
    flushChapter()
    return ParsedDoc(title, prologue.toString().trim(), chapters)
  }

  private fun h1Of(line: String): String? {
    val trimmed = line.trimStart()
    if (!trimmed.startsWith("# ") || trimmed.startsWith("## ")) return null
    return trimmed.removePrefix("# ").trim().takeIf { it.isNotEmpty() }
  }

  private fun h2Of(line: String): String? {
    val trimmed = line.trimStart()
    if (!trimmed.startsWith("## ") || trimmed.startsWith("### ")) return null
    return trimmed.removePrefix("## ").trim().takeIf { it.isNotEmpty() }
  }

  /**
   * Resolves a `--style` value to a [Stylesheet]:
   * - `null` → no stylesheet
   * - `"default"` → [Stylesheets.defaultReader]
   * - any other value → treated as a path to a `.css` file
   *
   * Throws [IllegalArgumentException] if the path doesn't exist.
   */
  private fun resolveStylesheet(style: String?): Stylesheet? = when {
    style == null -> null
    style == "default" -> Stylesheets.defaultReader()
    else -> {
      require(EpubIo.exists(style)) { "stylesheet file not found: $style" }
      Stylesheet(css = EpubIo.readText(style))
    }
  }

  private fun parseAuthor(raw: String): Author {
    val parts = raw.trim().split(' ').filter { it.isNotBlank() }
    return when (parts.size) {
      0 -> Author("Unknown", "")
      1 -> Author(parts[0], "")
      else -> Author(parts.dropLast(1).joinToString(" "), parts.last())
    }
  }

  private fun htmlResource(id: String, href: String, title: String, markdown: String): Resource {
    val body = renderMarkdownBody(markdown)
    val xhtml = wrapXhtml(title, body)
    return Resource(id, xhtml.encodeToByteArray(), href)
  }

  private fun renderMarkdownBody(markdown: String): String {
    val flavour = CommonMarkFlavourDescriptor()
    val tree = MarkdownParser(flavour).buildMarkdownTreeFromString(markdown)
    return HtmlGenerator(markdown, tree, flavour).generateHtml()
  }

  private fun wrapXhtml(pageTitle: String, bodyHtml: String): String = buildString {
    append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
    appendHTML(xhtmlCompatible = true)
      .html(namespace = "http://www.w3.org/1999/xhtml") {
        head { title(pageTitle) }
        body {
          // Body HTML is produced by the markdown renderer; insert verbatim.
          unsafe { +bodyHtml }
        }
      }
  }
}
