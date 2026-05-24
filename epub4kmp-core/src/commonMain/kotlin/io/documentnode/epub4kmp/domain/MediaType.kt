package io.documentnode.epub4kmp.domain

/**
 * MediaType is used to tell the type of content a resource is.
 *
 * Examples of mediatypes are image/gif, text/css and application/xhtml+xml
 *
 * All allowed mediaTypes are maintained by the MediaTypeService.
 *
 * @see MediaTypes
 */
data class MediaType(
  val name: String,
  val defaultExtension: String,
  val extensions: List<String>
) {
    constructor(
        name: String,
        defaultExtension: String,
        vararg extensions: String = arrayOf(defaultExtension)
    ) : this(name, defaultExtension, extensions.toList())
}
