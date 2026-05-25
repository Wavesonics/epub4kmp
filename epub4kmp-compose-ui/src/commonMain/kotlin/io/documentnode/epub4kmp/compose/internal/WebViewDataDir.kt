package io.documentnode.epub4kmp.compose.internal

/**
 * A writable directory for the platform WebView's persistent storage (cookies,
 * cache, WebView2 user-data folder on Windows). Returns null on platforms
 * where the underlying WebView ignores this setting (Android, iOS).
 *
 * On JVM/Desktop this MUST be non-null and writable — WebView2 on Windows
 * fails with E_ACCESSDENIED when its default path is unwritable, which
 * happens routinely when launching the JVM out of a Gradle daemon, a
 * read-only install dir, or anywhere under Program Files.
 */
internal expect fun defaultWebViewDataDir(): String?
