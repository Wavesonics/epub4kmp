package io.documentnode.epub4kmp.compose.internal

import io.documentnode.epub4kmp.domain.Book
import io.documentnode.epub4kmp.domain.MediaTypes
import io.documentnode.epub4kmp.domain.Resource
import io.documentnode.epub4kmp.epub.StylesheetLinker
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Bridge object the page's JS calls into. The native side reads the
 * `epub4kmp.navigate(href)` invocation from a JS message channel and turns it
 * into a Navigator call.
 */
internal const val JS_BRIDGE_NAME = "epub4kmp"

/**
 * The script the page calls to ask native to navigate to another in-book href.
 * Uses ComposeNativeWebview's auto-injected `window.kmpJsBridge.callNative`,
 * which is available on every backend (Android WebView, WKWebView, Wry,
 * wasmJs iframe).
 */
private val NAV_BRIDGE_SCRIPT = """
<script>
(function() {
  document.addEventListener('click', function(e) {
    var t = e.target;
    while (t && t.tagName !== 'A') t = t.parentElement;
    if (!t) return;
    var href = t.getAttribute('href');
    if (!href) return;
    if (/^[a-z][a-z0-9+.\-]*:/i.test(href) && !href.startsWith('epub:')) return;
    e.preventDefault();
    if (window.kmpJsBridge && window.kmpJsBridge.callNative) {
      window.kmpJsBridge.callNative('$JS_BRIDGE_NAME', href);
    }
  }, true);
})();
</script>
""".trimIndent()

/**
 * Builds a self-contained HTML document for [chapter] in [book]. All image,
 * stylesheet, and font references are inlined as `data:` URIs so the result
 * can be loaded into any WebView without per-platform resource interception.
 *
 * Tradeoffs:
 * - Bloats memory for image-heavy chapters. Acceptable for v0.1; native
 *   sub-resource interception is the planned v0.2 optimization.
 * - Inter-chapter `<a>` clicks are intercepted in JS and posted to the host
 *   via [JS_BRIDGE_NAME]; native code translates them into Navigator calls.
 *
 * Theming: if [backgroundCss] and [textCss] are non-null, a `<style>` block is
 * prepended that pins `html, body { color; background-color }` to those
 * values, so the rendered page matches the surrounding Compose theme instead
 * of inheriting the system's dark-mode preference.
 */
internal fun buildChapterDocument(
	book: Book,
	chapter: Resource,
	backgroundCss: String? = null,
	textCss: String? = null,
): String {
	// Per-chapter inject: processBook would force-materialize every XHTML
	// resource in the book on each render and defeat lazyLoadedTypes XHTML.
	val raw = normalizeHead(chapter.asString())
	val withLinks = chapter.href?.let { href ->
		StylesheetLinker.injectLinksInto(book, href, raw)
	} ?: raw
	val withInlinedCss = inlineStylesheets(book, chapter, withLinks)
	val withInlinedImages = inlineImages(book, chapter, withInlinedCss)
	val themed = if (backgroundCss != null && textCss != null) {
		injectThemeStyle(withInlinedImages, backgroundCss, textCss)
	} else withInlinedImages
	return injectScript(themed, NAV_BRIDGE_SCRIPT)
}

private val HEAD_SELF_CLOSING = Regex("<head\\b[^>]*/>", RegexOption.IGNORE_CASE)
private val HEAD_OPEN = Regex("<head\\b[^>]*(?<!/)>", RegexOption.IGNORE_CASE)
private val HTML_OPEN = Regex("<html\\b[^>]*>", RegexOption.IGNORE_CASE)

/**
 * Guarantees the document has an explicit `<head>…</head>` so [injectThemeStyle]
 * and [injectScript] can land their content inside it. Handles three cases:
 *
 * 1. Already has `<head>…</head>` → unchanged.
 * 2. Has `<head/>` (self-closing — e.g. some Sigil/InDesign exports) → expand.
 * 3. No `<head>` at all → insert empty one right after `<html ...>`.
 */
internal fun normalizeHead(html: String): String {
	HEAD_SELF_CLOSING.find(html)?.let { match ->
		val openTag = match.value.removeSuffix("/>").trimEnd() + ">"
		return html.substring(0, match.range.first) +
			"$openTag</head>" +
			html.substring(match.range.last + 1)
	}
	if (HEAD_OPEN.containsMatchIn(html)) return html
	HTML_OPEN.find(html)?.let { match ->
		val insertAt = match.range.last + 1
		return html.substring(0, insertAt) + "<head></head>" + html.substring(insertAt)
	}
	return html
}

private fun injectThemeStyle(html: String, background: String, color: String): String {
	// Use !important so we beat per-book CSS that might set body{color:...}.
	// Color-scheme hint stops the WebView from auto-styling form controls /
	// scrollbars for the wrong scheme.
	val scheme = if (looksLight(background)) "light" else "dark"
	val style = """
<style>
  :root { color-scheme: $scheme; }
  html, body { background-color: $background !important; color: $color !important; }
</style>
""".trimIndent()
	val headOpen = Regex("<head\\b[^>]*>", RegexOption.IGNORE_CASE).find(html)
	return if (headOpen != null) {
		val insertAt = headOpen.range.last + 1
		html.substring(0, insertAt) + "\n" + style + html.substring(insertAt)
	} else {
		// No <head> — fall back to head injection done by injectScript below.
		style + html
	}
}

/** Crude perceived-lightness check for a `#rrggbb` or `rgb(...)` string. */
private fun looksLight(css: String): Boolean {
	val rgb = Regex("#?([0-9a-f]{2})([0-9a-f]{2})([0-9a-f]{2})", RegexOption.IGNORE_CASE)
		.find(css) ?: return true
	val r = rgb.groupValues[1].toInt(16)
	val g = rgb.groupValues[2].toInt(16)
	val b = rgb.groupValues[3].toInt(16)
	// Standard luminance approximation; >= 128 → light.
	return (0.299 * r + 0.587 * g + 0.114 * b) >= 128
}

@OptIn(ExperimentalEncodingApi::class)
private fun inlineStylesheets(book: Book, chapter: Resource, html: String): String {
	// Matches <link ... href="..." ...> and <link ... href='...' ...>, capturing the href.
	val regex = Regex(
		"""<link\b([^>]*?)href\s*=\s*["']([^"']+)["']([^>]*)/?>""",
		RegexOption.IGNORE_CASE,
	)
	return regex.replace(html) { match ->
		val href = match.groupValues[2]
		val resource = resolveHref(book, href, chapter) ?: return@replace match.value
		if (resource.mediaType != MediaTypes.CSS) return@replace match.value

		val css = inlineCssFontFaces(book, resource, resource.asString())
		"<style>\n$css\n</style>"
	}
}

@OptIn(ExperimentalEncodingApi::class)
private fun inlineCssFontFaces(book: Book, cssResource: Resource, css: String): String {
	// url(...) inside CSS — replace with data: URIs.
	val regex = Regex("""url\(\s*["']?([^"')]+)["']?\s*\)""", RegexOption.IGNORE_CASE)
	return regex.replace(css) { match ->
		val href = match.groupValues[1].trim()
		if (href.startsWith("data:") || href.startsWith("http")) return@replace match.value
		val target = resolveHref(book, href, cssResource) ?: return@replace match.value
		val mime = target.mediaType?.name ?: "application/octet-stream"
		val b64 = Base64.encode(target.bytes())
		"url(\"data:$mime;base64,$b64\")"
	}
}

@OptIn(ExperimentalEncodingApi::class)
private fun inlineImages(book: Book, chapter: Resource, html: String): String {
	val regex = Regex(
		"""(<(?:img|image)\b[^>]*?(?:src|xlink:href)\s*=\s*["'])([^"']+)(["'])""",
		RegexOption.IGNORE_CASE,
	)
	return regex.replace(html) { match ->
		val href = match.groupValues[2]
		if (href.startsWith("data:") || href.startsWith("http")) return@replace match.value
		val target = resolveHref(book, href, chapter) ?: return@replace match.value
		val mime = target.mediaType?.name ?: "image/jpeg"
		val b64 = Base64.encode(target.bytes())
		"${match.groupValues[1]}data:$mime;base64,$b64${match.groupValues[3]}"
	}
}

private fun injectScript(html: String, script: String): String {
	val headClose = Regex("</head\\s*>", RegexOption.IGNORE_CASE).find(html)
	return if (headClose != null) {
		html.substring(0, headClose.range.first) + script + html.substring(headClose.range.first)
	} else {
		script + html
	}
}
