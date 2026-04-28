package io.documentnode.epub4j.util

/**
 * Various String utility functions.
 *
 * Most of the functions herein are re-implementations of the ones in Apache
 * Commons `StringUtils` — kept in this codebase to avoid pulling in the
 * full library.
 */
object StringUtil {
    /**
     * Normalizes a path containing `..`, `.` and empty segments. `X/foo/../Y`
     * becomes `X/Y`. Does not handle leading `..`.
     */
    fun collapsePathDots(path: String): String {
        val parts = path.split('/').dropLastWhile(String::isEmpty).toMutableList()
        var i = 0
        while (i < parts.size - 1) {
            val currentDir = parts[i]
            if (currentDir.isEmpty() || currentDir == ".") {
                parts.removeAt(i)
                i--
            } else if (currentDir == "..") {
                parts.removeAt(i - 1)
                parts.removeAt(i - 1)
                i -= 2
            }
            i++
        }
        val initial = if (path.startsWith("/")) "/" else ""
        return parts.foldIndexed(initial) { index, acc, part ->
            if (index < parts.lastIndex) "$acc$part/" else acc + part
        }
    }

    /**
     * Whether [source] ends with [suffix], ignoring case.
     */
    fun endsWithIgnoreCase(source: String, suffix: String): Boolean {
        if (suffix.isEmpty()) return true
        if (source.isEmpty()) return false
        if (suffix.length > source.length) return false
        return source.regionMatches(
            thisOffset = source.length - suffix.length,
            other = suffix,
            otherOffset = 0,
            length = suffix.length,
            ignoreCase = true
        )
    }

    /**
     * Returns [text], or [defaultValue] (default `""`) if `text` is null.
     */
    fun defaultIfNull(text: String?, defaultValue: String = ""): String =
        text ?: defaultValue

    /**
     * Null-safe string equality.
     */
    fun equals(text1: String?, text2: String?): Boolean = text1 == text2

    /**
     * Pretty toString printer.
     */
    fun toString(vararg keyValues: Any?): String {
        val result = StringBuilder()
        result.append('[')
        var i = 0
        while (i < keyValues.size) {
            if (i > 0) result.append(", ")
            result.append(keyValues[i])
            result.append(": ")
            val value: Any? = if (i + 1 < keyValues.size) keyValues[i + 1] else null
            if (value == null) result.append("<null>")
            else {
                result.append('\'')
                result.append(value)
                result.append('\'')
            }
            i += 2
        }
        result.append(']')
        return result.toString()
    }

    fun hashCode(vararg values: String): Int {
        var result = 31
        for (v in values) result = result xor v.hashCode()
        return result
    }
}
