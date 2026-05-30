package io.documentnode.epub4kmp.compose.internal

/**
 * The wasmJs WebView backend renders into an iframe and installs its own
 * `window.kmpJsBridge` via the iframe-postMessage shim, so no extra init
 * script is needed here.
 */
internal actual fun desktopJsBridgeInitScript(): String? = null
