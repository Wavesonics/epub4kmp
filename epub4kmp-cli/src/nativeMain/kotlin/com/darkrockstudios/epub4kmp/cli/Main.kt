package com.darkrockstudios.epub4kmp.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required

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

  override fun run() {
    val book = MarkdownToEpub.convert(
      markdownPath = input,
      author = author,
      language = language,
    )
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
