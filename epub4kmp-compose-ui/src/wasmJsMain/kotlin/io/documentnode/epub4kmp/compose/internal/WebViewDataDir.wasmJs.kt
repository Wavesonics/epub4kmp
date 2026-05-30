package io.documentnode.epub4kmp.compose.internal

/** The browser manages its own WebView storage; there is no data dir to set. */
internal actual fun defaultWebViewDataDir(): String? = null
