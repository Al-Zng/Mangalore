package com.mangalore.app

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Interactive verification surface. It is intentionally not rendered at launch: the caller owns
 * [visible] and should set it only after MangaloreScraper reports HTTP 403/challenge markup.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ChallengeWebViewDialog(
    visible: Boolean,
    challengeUrl: String,
    userAgent: String,
    onSolved: (cookieHeader: String?) -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("التحقق من الاتصال") },
        text = {
            AndroidView(
                modifier = Modifier.fillMaxWidth().height(420.dp),
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.userAgentString = userAgent
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView, url: String) {
                                // A completed challenge normally sets cf_clearance. We pass the
                                // cookie header back to the session layer; no bypass is attempted.
                                val cookies = CookieManager.getInstance().getCookie(url)
                                if (cookies?.contains("cf_clearance=") == true) onSolved(cookies)
                            }
                        }
                        loadUrl(challengeUrl)
                    }
                }
            )
        },
        confirmButton = {
            Button(onClick = onDismiss, modifier = Modifier.padding(4.dp)) { Text("إغلاق") }
        }
    )
}
