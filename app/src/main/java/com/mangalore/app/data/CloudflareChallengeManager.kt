package com.mangalore.app.data

import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CompletableDeferred

/**
 * Bridges the background network layer (MangaService runs on IO threads with no
 * window/UI attachment) with a Compose-hosted WebView the user can actually see.
 * Cloudflare's interstitial check requires a real, visible, attached browsing
 * context — a WebView built off-screen with no window will never clear it.
 *
 * MangaService calls [solve] from a background coroutine; that suspends until
 * [CloudflareChallengeHost] (mounted once near the root of the compose tree) has
 * shown the page in a dialog, the challenge clears on its own or the user completes
 * it, and the resulting HTML has been captured.
 */
object CloudflareChallengeManager {

    data class ChallengeRequest(
        val url: String,
        val extractJs: String,
        val completion: CompletableDeferred<String>
    )

    val pending = mutableStateOf<ChallengeRequest?>(null)

    suspend fun solve(
        url: String,
        extractJs: String = "document.documentElement.outerHTML"
    ): String {
        val completion = CompletableDeferred<String>()
        pending.value = ChallengeRequest(url, extractJs, completion)
        return try {
            completion.await()
        } finally {
            if (pending.value?.completion === completion) pending.value = null
        }
    }

    /** Called by the host once the dialog's WebView has landed on the real page. */
    fun resolve(html: String) {
        pending.value?.completion?.complete(html)
        pending.value = null
    }

    /** Called when the user dismisses the dialog before the challenge clears. */
    fun cancel() {
        pending.value?.completion?.cancel()
        pending.value = null
    }
}
