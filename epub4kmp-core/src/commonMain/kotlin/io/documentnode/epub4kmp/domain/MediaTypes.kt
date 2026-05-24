package io.documentnode.epub4kmp.domain

/**
 * Manages mediatypes that are used by epubs
 *
 * @author paul
 */
object MediaTypes {
    val XHTML: MediaType = MediaType(
        "application/xhtml+xml",
        ".xhtml",
        ".htm", ".html", ".xhtml"
    )
    val EPUB: MediaType = MediaType(
        "application/epub+zip",
        ".epub"
    )
    val NCX: MediaType = MediaType(
        "application/x-dtbncx+xml",
        ".ncx"
    )

    val JAVASCRIPT: MediaType = MediaType(
        "text/javascript",
        ".js"
    )
    val CSS: MediaType = MediaType("text/css", ".css")

    // images
    val JPG: MediaType = MediaType(
        "image/jpeg", ".jpg",
        ".jpg", ".jpeg"
    )
    val PNG: MediaType = MediaType("image/png", ".png")
    val GIF: MediaType = MediaType("image/gif", ".gif")

    val SVG: MediaType = MediaType("image/svg+xml", ".svg")

    // fonts
    val TTF: MediaType = MediaType(
        "application/x-truetype-font", ".ttf"
    )
    val OPENTYPE: MediaType = MediaType(
        "application/vnd.ms-opentype", ".otf"
    )
    val WOFF: MediaType = MediaType(
        "application/font-woff",
        ".woff"
    )

    // audio
    val MP3: MediaType = MediaType("audio/mpeg", ".mp3")
    val OGG: MediaType = MediaType("audio/ogg", ".ogg")

    // video
    val MP4: MediaType = MediaType("video/mp4", ".mp4")

    val SMIL: MediaType = MediaType(
        "application/smil+xml",
        ".smil"
    )
    val XPGT: MediaType = MediaType(
        "application/adobe-page-template+xml", ".xpgt"
    )
    val PLS: MediaType = MediaType(
        "application/pls+xml",
        ".pls"
    )

    var mediaTypes: Array<MediaType> = arrayOf(
        XHTML, EPUB, JPG, PNG, GIF, CSS, SVG, TTF, NCX, XPGT, OPENTYPE, WOFF,
        SMIL, PLS, JAVASCRIPT, MP3, MP4, OGG
    )

    var mediaTypesByName: MutableMap<String, MediaType> = HashMap()

    init {
        for (i in mediaTypes.indices) {
            mediaTypesByName[mediaTypes[i].name] = mediaTypes[i]
        }
    }

    fun isBitmapImage(mediaType: MediaType): Boolean {
        return mediaType === JPG || mediaType === PNG || mediaType === GIF
    }

    /**
     * Gets the MediaType based on the file extension.
     * Null of no matching extension found.
     *
     * @param filename
     * @return the MediaType based on the file extension.
     */
    fun determineMediaType(filename: String): MediaType? {
        return mediaTypesByName.values.firstOrNull { type ->
            type.extensions.any { extension ->
                filename.endsWith(extension, true)
            }
        }
    }

    fun getMediaTypeByName(mediaTypeName: String): MediaType? {
        return mediaTypesByName[mediaTypeName]
    }
}
