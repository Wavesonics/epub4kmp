package com.darkrockstudios.epub4kmp.cli

import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import okio.IOException

private class Epub4kmpCli : CliktCommand(name = "epub4kmp") {
  override fun help(context: Context) = "Convert between Markdown and EPUB."
  override fun run() = Unit
}

private class Md2Epub : CliktCommand(name = "md2epub") {
  override fun help(context: Context) =
    "Convert a Markdown file to an EPUB. The first H1 becomes the book title; each H2 becomes a chapter."

  private val input by option("-i", "--input", help = "Input .md path").required()
  private val output by option("-o", "--output", help = "Output .epub path").required()
  private val author by option("-a", "--author", help = "Author name").default("Unknown")
  private val language by option("-l", "--language", help = "Language code").default("en")
  private val style by option(
    "-s", "--style",
    help = "Stylesheet: 'default' for the built-in reader preset, or a path to a .css file. Omit for none.",
  )

  override fun run() {
    val book = try {
      MarkdownToEpub.convert(
        markdownPath = input,
        author = author,
        language = language,
        style = style,
      )
    } catch (e: IllegalArgumentException) {
      throw CliktError(e.message ?: "invalid argument")
    } catch (e: IOException) {
      throw CliktError("could not read input: ${e.message}")
    }
    EpubIo.writeEpub(book, output)
    echo("Wrote $output")
  }
}

private class Epub2Md : CliktCommand(name = "epub2md") {
  override fun help(context: Context) = "Convert an EPUB file to Markdown."

  private val input by option("-i", "--input", help = "Input .epub path").required()
  private val output by option("-o", "--output", help = "Output .md path").required()

  override fun run() {
    val md = EpubToMarkdown.convert(input)
    EpubIo.writeText(md, output)
    echo("Wrote $output")
  }
}

fun main(args: Array<String>) =
  Epub4kmpCli().subcommands(Md2Epub(), Epub2Md()).main(args)
