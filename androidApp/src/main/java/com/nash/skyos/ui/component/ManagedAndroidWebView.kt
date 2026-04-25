package com.nash.skyos.ui.component

import android.content.Context
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun ManagedAndroidWebView(
    modifier: Modifier = Modifier,
    factory: (Context) -> WebView,
    update: (WebView) -> Unit = {},
) {
    var webView by remember { mutableStateOf<WebView?>(null) }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            factory(context).also { createdView ->
                webView = createdView
            }
        },
        update = { view ->
            if (webView !== view) {
                webView = view
            }
            update(view)
        },
    )

    DisposableEffect(Unit) {
        onDispose {
            webView?.disposeSkydownWebView()
            webView = null
        }
    }
}

fun WebView.disposeSkydownWebView() {
    runCatching { stopLoading() }
    runCatching { loadUrl("about:blank") }
    runCatching { onPause() }
    runCatching { pauseTimers() }
    runCatching { clearHistory() }
    runCatching { removeAllViews() }
    runCatching { destroy() }
}
