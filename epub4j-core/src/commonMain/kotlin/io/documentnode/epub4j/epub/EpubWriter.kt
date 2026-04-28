package io.documentnode.epub4j.epub

import io.documentnode.epub4j.domain.Book
import io.documentnode.epub4j.domain.MediaTypes
import io.documentnode.epub4j.domain.Resource
import no.synth.kmpzip.crypto.Crypto
import no.synth.kmpzip.okio.asOutputStream
import no.synth.kmpzip.zip.ZipConstants
import no.synth.kmpzip.zip.ZipEntry
import no.synth.kmpzip.zip.ZipOutputStream
import okio.Sink
import okio.buffer

/**
 * Generates an EPUB file. Not thread-safe, single-use.
 */
class EpubWriter(
    private val bookProcessor: BookProcessor = BookProcessor.IDENTITY_BOOKPROCESSOR
) {
    /**
     * Writes the given [book] to [sink] as a complete EPUB ZIP archive.
     */
    fun write(book: Book, sink: Sink) {
        val processedBook = processBook(book)
        val zos = ZipOutputStream(sink.buffer().asOutputStream())
        try {
            writeMimeType(zos)
            writeContainer(zos)
            initTOCResource(processedBook)
            writeResources(processedBook, zos)
            writePackageDocument(processedBook, zos)
        } finally {
            zos.close()
        }
    }

    private fun processBook(book: Book): Book = bookProcessor.processBook(book)

    private fun initTOCResource(book: Book) {
        try {
            val tocResource = NCXDocument.createNCXResource(book)
            val currentTocResource = book.spine.tocResource
            if (currentTocResource != null) {
                book.resources.remove(currentTocResource.href)
            }
            book.spine.tocResource = tocResource
            book.resources.add(tocResource)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private fun writeResources(book: Book, zos: ZipOutputStream) {
        book.resources.all.forEach { writeResource(it, zos) }
    }

    private fun writeResource(resource: Resource, zos: ZipOutputStream) {
        try {
            zos.putNextEntry(ZipEntry("OEBPS/" + resource.href))
            zos.write(resource.bytes())
            zos.closeEntry()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun writePackageDocument(book: Book, zos: ZipOutputStream) {
        val sb = StringBuilder()
        val writer = EpubProcessorSupport.createXmlWriter(sb)
        try {
            PackageDocumentWriter.write(this, writer, book)
        } finally {
            writer.close()
        }
        val bytes = sb.toString().encodeToByteArray()
        zos.putNextEntry(ZipEntry("OEBPS/content.opf"))
        zos.write(bytes)
        zos.closeEntry()
    }

    private fun writeContainer(zos: ZipOutputStream) {
        val xml = buildString {
            append("<?xml version=\"1.0\"?>\n")
            append("<container version=\"1.0\" xmlns=\"urn:oasis:names:tc:opendocument:xmlns:container\">\n")
            append("\t<rootfiles>\n")
            append("\t\t<rootfile full-path=\"OEBPS/content.opf\" media-type=\"application/oebps-package+xml\"/>\n")
            append("\t</rootfiles>\n")
            append("</container>")
        }
        zos.putNextEntry(ZipEntry("META-INF/container.xml"))
        zos.write(xml.encodeToByteArray())
        zos.closeEntry()
    }

    /** Stores the mimetype as an uncompressed STORED entry (required by the EPUB spec). */
    private fun writeMimeType(zos: ZipOutputStream) {
        val mimetypeBytes: ByteArray = MediaTypes.EPUB.name.encodeToByteArray()
        val entry = ZipEntry("mimetype").apply {
            method = ZipConstants.STORED
            size = mimetypeBytes.size.toLong()
            compressedSize = mimetypeBytes.size.toLong()
            crc = Crypto.crc32(mimetypeBytes)
        }
        zos.putNextEntry(entry)
        zos.write(mimetypeBytes)
        zos.closeEntry()
    }

    val ncxId: String get() = "ncx"
    val ncxHref: String get() = "toc.ncx"
    val ncxMediaType: String get() = MediaTypes.NCX.name

    companion object {
        const val EMPTY_NAMESPACE_PREFIX: String = ""
    }
}
