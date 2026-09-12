package com.mangalore.app.ui.common

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mangalore.app.data.CloudflareChallengeManager

/**
 * Mount this once, near the root of the compose tree (see MangaloreApp). Whenever the
 * network layer hits a Cloudflare interstitial, this renders it in a real dialog with
 * a genuine, visible, interactive WebView — Cloudflare will not clear a challenge for
 * a WebView the user can't see, so the fetch stays blocked until this is on screen.
 * Most challenges clear on their own after a couple of seconds; a rare interactive
 * one (e.g. a Turnstile checkbox) needs the user to tap it, which they can do here.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CloudflareChallengeHost() {
    val request = CloudflareChallengeManager.pending.value ?: return

    Dialog(
        onDismissRequest = { CloudflareChallengeManager.cancel() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.94f).fillMaxHeight(0.85f),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("جارٍ التحقق من الاتصال…", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { CloudflareChallengeManager.cancel() }) {
                        Text("إلغاء")
                    }
                }
                AndroidView(
                    modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            var resolved = false
                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView, finishedUrl: String) {
                                    if (resolved) return
                                    // Give Cloudflare's own JS a moment to finish and redirect
                                    // before checking whether we've reached the real page.
                                    view.postDelayed({
                                        if (resolved) return@postDelayed
                                        view.evaluateJavascript(request.extractJs) { raw ->
                                            val html = raw?.trim('"')
                                                ?.replace("\\u003C", "<")
                                                ?.replace("\\\"", "\"")
                                                ?.replace("\\n", "\n") ?: ""
                                            val stillChallenged = html.contains("Just a moment") ||
                                                html.contains("cf-browser-verification") ||
                                                html.contains("Checking your browser") ||
                                                html.isBlank()
                                            if (!stillChallenged) {
                                                resolved = true
                                                CloudflareChallengeManager.resolve(html)
                                            }
                                            // else: leave the WebView on screen so the user
                                            // can interact with any visible challenge widget;
                                            // the next onPageFinished (after they solve it or
                                            // it clears itself) will re-check.
                                        }
                                    }, 900)
                                }
                            }
                            loadUrl(request.url)
                        }
                    }
                )
            }
        }
    }
}
