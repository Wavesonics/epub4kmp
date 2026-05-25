package io.documentnode.epub4kmp.compose.internal

import java.io.File

/**
 * Returns `<user.home>/.epub4kmp/webview-data`, creating the directory if it
 * doesn't exist. This is the WebView2 user-data folder on Windows and the
 * equivalent for WebKitGTK / WKWebView on Linux / macOS.
 *
 * Picking a path under the user's home avoids the common E_ACCESSDENIED
 * crash on Windows where WebView2's default location is unwritable.
 */
internal actual fun defaultWebViewDataDir(): String? {
	val dir = File(System.getProperty("user.home"), ".epub4kmp/webview-data")
	if (!dir.exists()) dir.mkdirs()
	return dir.absolutePath
}
