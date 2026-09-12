package com.mangalore.app

import com.mangalore.app.data.ChallengeHandler
import com.mangalore.app.data.ChallengeRequest
import com.mangalore.app.data.ChallengeResolution
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class ComposeChallengeHandler(private val onPending: (ChallengeRequest?) -> Unit) : ChallengeHandler {
    private var continuation: CancellableContinuation<ChallengeResolution?>? = null
    var pendingRequest: ChallengeRequest? = null
        private set

    override suspend fun requestChallenge(request: ChallengeRequest): ChallengeResolution? =
        suspendCancellableCoroutine { cont ->
            pendingRequest = request
            onPending(request)
            continuation = cont
            cont.invokeOnCancellation {
                if (continuation === cont) {
                    continuation = null
                    pendingRequest = null
                    onPending(null)
                }
            }
        }

    fun resolve(cookieHeader: String?, userAgent: String?) {
        val cont = continuation ?: return
        continuation = null
        pendingRequest = null
        onPending(null)
        cont.resume(ChallengeResolution(cookieHeader, userAgent))
    }

    fun cancel() {
        val cont = continuation ?: return
        continuation = null
        pendingRequest = null
        onPending(null)
        cont.resume(null)
    }
}
