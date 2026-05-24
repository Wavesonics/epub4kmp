package com.darkrockstudios.epub4kmp.cli

import io.documentnode.epub4kmp.domain.Book
import io.documentnode.epub4kmp.epub.EpubReader
import io.documentnode.epub4kmp.epub.EpubWriter
import okio.FileSystem
import okio.Path.Companion.toPath

object EpubIo {
  private val fs: FileSystem get() = FileSystem.SYSTEM

  fun readText(path: String): String =
    fs.read(path.toPath()) { readUtf8() }

  fun exists(path: String): Boolean = fs.exists(path.toPath())

  fun writeText(text: String, path: String) {
    fs.write(path.toPath()) { writeUtf8(text) }
  }

  fun readEpub(path: String): Book =
    EpubReader().readEpub(fs, path.toPath())

  fun writeEpub(book: Book, path: String) {
    val sink = fs.sink(path.toPath())
    try {
      EpubWriter().write(book, sink)
    } finally {
      sink.close()
    }
  }
}
