package io.documentnode.epub4kmp.util

import okio.FileSystem
import okio.Path

/**
 * Opens the ZIP at [zipPath] as a read-only [FileSystem].
 *
 * Backed by okio's `openZip` on JVM and native targets. wasmJs has no okio ZIP
 * filesystem (and no real on-disk filesystem in the browser), so the wasmJs
 * actual throws — load EPUBs there via `EpubReader.readEpub(source)` instead,
 * which streams through kmp-zip and works on every target.
 */
internal expect fun openEpubZip(fileSystem: FileSystem, zipPath: Path): FileSystem
