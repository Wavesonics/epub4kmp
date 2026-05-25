package io.documentnode.epub4kmp.compose.internal

/** Android WebView manages its own data directory; ignored by the library. */
internal actual fun defaultWebViewDataDir(): String? = null
