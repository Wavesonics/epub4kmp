package io.documentnode.epub4kmp.compose.internal

/**
 * Init script that installs `window.kmpJsBridge.callNative(method, params, cb)`.
 *
 * Returns non-null on JVM/Desktop, where the Wry backend does NOT auto-install
 * the bridge object and posts to a separate `window.ipc.postMessage` channel.
 * Returns null on Android (Android WebView is wired via `addJavascriptInterface`
 * by the library itself), iOS (WKScriptMessageHandler installs the bridge
 * symbol natively), and wasmJs (the iframe-postMessage shim does its own
 * install).
 *
 * Without this, in-book `<a>` clicks rendered by [buildChapterDocument] would
 * have nowhere to dispatch to and would silently no-op.
 */
internal expect fun desktopJsBridgeInitScript(): String?

/**
 * The shared body of the JS bridge bootstrap, exposed here so platform actuals
 * can reuse it verbatim. Posts a message shaped to match what the desktop IPC
 * channel + `JsMessageParsing` expects:
 *
 * ```
 * { "methodName": "...", "params": "<stringified>", "callbackId": -1 }
 * ```
 *
 * Callback support is included for parity with other backends, even though our
 * EPUB use case is fire-and-forget.
 */
internal const val JS_BRIDGE_INIT_SCRIPT: String = """
(function() {
  if (window.kmpJsBridge && typeof window.kmpJsBridge.callNative === 'function') {
    return;
  }
  var nextId = 1;
  var callbacks = {};
  window.kmpJsBridge = {
    callNative: function(methodName, params, callback) {
      var callbackId = callback ? (nextId++) : -1;
      if (callback) callbacks[callbackId] = callback;
      try {
        var serialized = typeof params === 'string' ? params : JSON.stringify(params);
        var envelope = JSON.stringify({
          methodName: methodName,
          params: serialized,
          callbackId: callbackId,
        });
        if (window.ipc && typeof window.ipc.postMessage === 'function') {
          window.ipc.postMessage(envelope);
        } else if (window.chrome && window.chrome.webview &&
                   typeof window.chrome.webview.postMessage === 'function') {
          window.chrome.webview.postMessage(envelope);
        }
      } catch (e) {
        // Swallow — there's nowhere useful to log from inside the bridge.
      }
    },
    onCallback: function(id, data) {
      var cb = callbacks[id];
      if (cb) { delete callbacks[id]; cb(data); }
    }
  };
})();
"""
