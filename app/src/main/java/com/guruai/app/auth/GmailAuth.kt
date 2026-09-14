package com.guruai.app.auth

import android.accounts.Account
import android.content.Context
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GmailAuth {
    private const val SCOPE_READONLY = "https://www.googleapis.com/auth/gmail.readonly"
    private const val SCOPE_COMPOSE = "https://www.googleapis.com/auth/gmail.compose"

    fun signInClient(context: Context): GoogleSignInClient {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(SCOPE_READONLY), Scope(SCOPE_COMPOSE))
            .build()
        return GoogleSignIn.getClient(context, options)
    }

    suspend fun getAccessToken(context: Context): String? {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return null
        val androidAccount: Account = account.account ?: return null
        return withContext(Dispatchers.IO) {
            try {
                GoogleAuthUtil.getToken(context, androidAccount, "oauth2:$SCOPE_READONLY $SCOPE_COMPOSE")
            } catch (e: Exception) {
                null
            }
        }
    }

    fun connectedEmail(context: Context): String? {
        return GoogleSignIn.getLastSignedInAccount(context)?.email
    }
}
