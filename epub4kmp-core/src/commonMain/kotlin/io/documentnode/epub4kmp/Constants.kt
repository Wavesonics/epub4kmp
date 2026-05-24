package io.documentnode.epub4kmp

interface Constants {
    companion object {
        const val CHARACTER_ENCODING: String = "UTF-8"
        const val DOCTYPE_XHTML: String =
            "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD XHTML 1.1//EN\" \"http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd\">"
        const val NAMESPACE_XHTML: String = "http://www.w3.org/1999/xhtml"
        const val EPUB4KMP_GENERATOR_NAME: String = "EPUB4KMP v"
        const val FRAGMENT_SEPARATOR_CHAR: Char = '#'
        const val DEFAULT_TOC_ID: String = "toc"
    }
}
