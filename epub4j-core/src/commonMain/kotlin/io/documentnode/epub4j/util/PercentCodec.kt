package io.documentnode.epub4j.util

import okio.Buffer

/**
 * Decodes percent-escapes (e.g. `%20`) in a URL string, treating the underlying
 * bytes as UTF-8. Falls back to leaving any malformed escape unchanged.
 *
 * Replaces `java.net.URLDecoder.decode(s, "UTF-8")` for KMP.
 */
fun String.percentDecode(): String {
    if ('%' !in this) return this
    val out = Buffer()
    var i = 0
    while (i < length) {
        val c = this[i]
        if (c == '%' && i + 2 < length) {
            val hi = this[i + 1].digitToIntOrNull(16)
            val lo = this[i + 2].digitToIntOrNull(16)
            if (hi != null && lo != null) {
                out.writeByte((hi shl 4) or lo)
                i += 3
                continue
            }
        }
        out.writeUtf8CodePoint(c.code)
        i++
    }
    return out.readUtf8()
}
