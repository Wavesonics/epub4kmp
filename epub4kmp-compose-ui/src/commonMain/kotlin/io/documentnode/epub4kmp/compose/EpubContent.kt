package io.documentnode.epub4kmp.compose

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.documentnode.epub4kmp.compose.internal.*
import io.documentnode.epub4kmp.domain.Book
import io.documentnode.epub4kmp.domain.Resource
import io.github.kdroidfilter.webview.jsbridge.IJsMessageHandler
import io.github.kdroidfilter.webview.jsbridge.JsMessage
import io.github.kdroidfilter.webview.jsbridge.rememberWebViewJsBridge
import io.github.kdroidfilter.webview.web.WebView
import io.github.kdroidfilter.webview.web.WebViewNavigator
import io.github.kdroidfilter.webview.web.rememberWebViewNavigator
import io.github.kdroidfilter.webview.web.rememberWebViewStateWithHTMLData

/**
 * Renders a single XHTML [resource] from [book] in a platform WebView.
 *
 * Images, stylesheets, and embedded fonts referenced by the chapter are
 * inlined as `data:` URIs so no per-platform request interception is needed.
 *
 * The rendered page inherits [backgroundColor] and [textColor], defaulting to
 * the surrounding Material theme's surface / onSurface so dark mode and light
 * mode both stay readable.
 *
 * @param onLinkClicked called when the user taps an `<a>` tag inside the
 *   chapter. The href is resolved against the chapter's path and the resolved
 *   [LinkClick] is delivered, ready to hand off to a
 *   [io.documentnode.epub4kmp.browsersupport.Navigator]. If null, link taps
 *   are ignored.
 */
@Composable
fun EpubContent(
	book: Book,
	resource: Resource,
	modifier: Modifier = Modifier,
	backgroundColor: Color = MaterialTheme.colorScheme.surface,
	textColor: Color = MaterialTheme.colorScheme.onSurface,
	onLinkClicked: ((LinkClick) -> Unit)? = null,
) {
	val backgroundCss = remember(backgroundColor) { backgroundColor.toCssHex() }
	val textCss = remember(textColor) { textColor.toCssHex() }
	val html = remember(book, resource, backgroundCss, textCss) {
		buildChapterDocument(book, resource, backgroundCss, textCss)
	}
	val state = rememberWebViewStateWithHTMLData(data = html)
	val navigator = rememberWebViewNavigator()
	val jsBridge = rememberWebViewJsBridge(navigator)

	// Configure settings during composition — before WebView()'s effects can
	// kick off native creation with default (and often unwritable) settings.
	remember(state, backgroundColor) {
		state.webSettings.isJavaScriptEnabled = true
		// Paint an opaque background so the OS doesn't show its window-clear
		// color (black on Windows) during resize before the WebView repaints.
		state.webSettings.backgroundColor = backgroundColor
		state.webSettings.desktopWebSettings.transparent = false
		defaultWebViewDataDir()?.let { dir ->
			state.webSettings.desktopWebSettings.dataDirectory = dir
		}
		// Wry doesn't auto-install window.kmpJsBridge — without this our in-book
		// link clicks would silently no-op because the JS handler can't reach
		// native. No-op on Android/iOS where the library already wires the bridge.
		desktopJsBridgeInitScript()?.let { script ->
			state.webSettings.desktopWebSettings.initScript = script
		}
		Unit
	}

	DisposableEffect(jsBridge, book, resource, onLinkClicked) {
		val handler = NavBridgeHandler { rawHref ->
			val cb = onLinkClicked ?: return@NavBridgeHandler
			val resolved = resolveLink(book, rawHref, resource)
			cb(LinkClick(href = rawHref, resource = resolved.resource, fragmentId = resolved.fragmentId))
		}
		jsBridge.register(handler)
		onDispose { jsBridge.unregister(handler) }
	}

	WebView(
		state = state,
		modifier = modifier,
		navigator = navigator,
		webViewJsBridge = jsBridge,
	)
}

private fun Color.toCssHex(): String {
	fun Float.toHexByte() = (this.coerceIn(0f, 1f) * 255f).toInt().toString(16).padStart(2, '0')
	return "#${red.toHexByte()}${green.toHexByte()}${blue.toHexByte()}"
}

private class NavBridgeHandler(
	private val onNavigate: (String) -> Unit,
) : IJsMessageHandler {
	override fun methodName(): String = JS_BRIDGE_NAME

	override fun handle(
		message: JsMessage,
		navigator: WebViewNavigator?,
		callback: (String) -> Unit,
	) {
		onNavigate(message.params)
		callback("ok")
	}
}
