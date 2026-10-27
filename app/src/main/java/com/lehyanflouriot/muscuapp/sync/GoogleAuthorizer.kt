package com.lehyanflouriot.muscuapp.sync

import android.accounts.Account
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.ClearTokenRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import com.lehyanflouriot.muscuapp.core.sync.AccessTokens
import com.lehyanflouriot.muscuapp.core.sync.DriveRemote
import com.lehyanflouriot.muscuapp.core.sync.SyncException
import com.lehyanflouriot.muscuapp.core.sync.SyncProblem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Asks Google for the access to the folder of the app in the user's Google Drive. */
class GoogleAuthorizer(context: Context) {
    private val client = Identity.getAuthorizationClient(context.applicationContext)
    private val scopes = listOf(Scope(DriveRemote.SCOPE))

    /**
     * Asks for the access with [account], or with the account the user picks when [chooseAccount].
     * The result holds a token, or a screen of Google to show the user first.
     */
    suspend fun authorize(account: String?, chooseAccount: Boolean = false): AuthorizationResult {
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(scopes)
            .apply {
                when {
                    chooseAccount -> setPrompt(AuthorizationRequest.Prompt.SELECT_ACCOUNT)
                    !account.isNullOrEmpty() -> setAccount(Account(account, ACCOUNT_TYPE))
                }
            }
            .build()
        return guarded { client.authorize(request).await() }
    }

    /** What the screen of Google shown after [authorize] gave. */
    fun resultOf(data: Intent?): AuthorizationResult = guarded { client.getAuthorizationResultFromIntent(data) }

    /** Tokens of [account], given without asking the user anything once the access was granted. */
    fun tokensFor(account: String): AccessTokens = object : AccessTokens {
        override suspend fun token(): String {
            val result = authorize(account)
            return result.accessToken?.takeUnless { result.hasResolution() } ?: throw SyncException(SyncProblem.SIGN_IN)
        }

        override suspend fun invalidate(token: String) {
            try {
                client.clearToken(ClearTokenRequest.builder().setToken(token).build()).await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // The next token may still be the refused one: Google will refuse it again.
            }
        }
    }

    private inline fun <T> guarded(block: () -> T): T = try {
        block()
    } catch (e: ApiException) {
        val problem = when (e.statusCode) {
            CommonStatusCodes.NETWORK_ERROR -> SyncProblem.OFFLINE
            CommonStatusCodes.DEVELOPER_ERROR -> SyncProblem.SETUP
            CommonStatusCodes.INTERNAL_ERROR, CommonStatusCodes.TIMEOUT -> SyncProblem.SERVER
            else -> SyncProblem.SIGN_IN
        }
        throw SyncException(problem, e.message, e)
    }

    private companion object {
        const val ACCOUNT_TYPE = "com.google"
    }
}

/** Waits for a task of Google Play services. */
private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { continuation.resume(it) }
    addOnFailureListener { continuation.resumeWithException(it) }
    addOnCanceledListener { continuation.cancel() }
}
