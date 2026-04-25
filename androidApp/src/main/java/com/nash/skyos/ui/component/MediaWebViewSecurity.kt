package com.nash.skyos.ui.component

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

private val SkydownAllowedWebViewSchemes = setOf("https", "http", "about")

@SuppressLint("SetJavaScriptEnabled")
@Suppress("DEPRECATION")
internal fun WebView.applySkydownMediaWebViewDefaults() {
    CookieManager.getInstance().apply {
        setAcceptCookie(true)
        setAcceptThirdPartyCookies(this@applySkydownMediaWebViewDefaults, true)
    }

    settings.apply {
        javaScriptEnabled = true
        mediaPlaybackRequiresUserGesture = false
        domStorageEnabled = true
        javaScriptCanOpenWindowsAutomatically = false
        loadsImagesAutomatically = true
        useWideViewPort = true
        loadWithOverviewMode = true
        allowFileAccess = false
        allowContentAccess = false
        allowFileAccessFromFileURLs = false
        allowUniversalAccessFromFileURLs = false
        setGeolocationEnabled(false)
        mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        safeBrowsingEnabled = true
        setSupportMultipleWindows(false)
    }
}

internal open class SkydownMediaWebViewClient : WebViewClient() {
    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val targetUri = request?.url ?: return false
        return !targetUri.isSkydownAllowedWebViewNavigation()
    }
}

internal fun WebView.loadSkydownWebUrl(url: String, headers: Map<String, String> = emptyMap()) {
    if (!url.isSkydownAllowedWebViewNavigation()) {
        if (this.url != "about:blank") {
            loadUrl("about:blank")
        }
        return
    }

    if (headers.isEmpty()) {
        loadUrl(url)
    } else {
        loadUrl(url, headers)
    }
}

private fun String.isSkydownAllowedWebViewNavigation(): Boolean =
    runCatching { Uri.parse(this).isSkydownAllowedWebViewNavigation() }.getOrDefault(false)

private fun Uri.isSkydownAllowedWebViewNavigation(): Boolean {
    val scheme = scheme?.lowercase() ?: return false
    return scheme in SkydownAllowedWebViewSchemes
}
