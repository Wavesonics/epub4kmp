package io.documentnode.epub4kmp.compose.internal

/** WKWebView manages its own data directory; ignored by the library. */
internal actual fun defaultWebViewDataDir(): String? = null
