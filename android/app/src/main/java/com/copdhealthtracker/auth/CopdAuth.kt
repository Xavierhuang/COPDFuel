package com.copdhealthtracker.auth

import android.content.Context
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUser
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserAttributes
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.AuthenticationHandler
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.GenericHandler
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.SignUpHandler
import com.amazonaws.services.cognitoidentityprovider.model.SignUpResult
import com.amazonaws.regions.Regions
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference

/**
 * Wrapper around Cognito User Pool for sign-in, sign-up, and JWT access.
 * Uses pool: us-east-2_qscPZox2h, client: 45i90gg5jtm20ddvq3rs6d0k8b, region us-east-2.
 */
class CopdAuth(context: Context) {
    private val pool = CognitoUserPool(
        context,
        USER_POOL_ID,
        CLIENT_ID,
        "", // No client secret for this app client
        Regions.US_EAST_2
    )

    fun getCurrentUser(): CognitoUser? = pool.currentUser

    fun getUser(username: String): CognitoUser = pool.getUser(username)

    fun signIn(username: String, password: String, callback: (Result<String>) -> Unit) {
        val user = pool.getUser(username.trim())
        user.getSessionInBackground(object : AuthenticationHandler {
            override fun onSuccess(session: com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserSession?, device: com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoDevice?) {
                val token = session?.idToken?.jwtToken
                if (token != null) callback(Result.success(token))
                else callback(Result.failure(Exception("No token")))
            }
            override fun getAuthenticationDetails(authenticationContinuation: com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationContinuation?, userId: String?) {
                authenticationContinuation?.setAuthenticationDetails(
                    com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationDetails(username, password, emptyMap())
                )
                authenticationContinuation?.continueTask()
            }
            override fun getMFACode(continuation: com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.MultiFactorAuthenticationContinuation?) {
                callback(Result.failure(Exception("MFA not implemented")))
            }
            override fun authenticationChallenge(continuation: com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.ChallengeContinuation?) {
                callback(Result.failure(Exception("Auth challenge")))
            }
            override fun onFailure(exception: Exception?) {
                callback(Result.failure(exception ?: Exception("Sign in failed")))
            }
        })
    }

    fun signOut() {
        pool.currentUser?.signOut()
    }

    /**
     * Sign up with email (username), password, and optional display name.
     * Callback receives Result.success(needsConfirmation) or Result.failure.
     */
    fun signUp(
        email: String,
        password: String,
        name: String?,
        callback: (Result<Boolean>) -> Unit
    ) {
        val username = email.trim()
        val attrs = CognitoUserAttributes().apply {
            addAttribute("email", username)
            addAttribute("name", name?.trim()?.takeIf { it.isNotEmpty() } ?: username.substringBefore("@", "User"))
        }
        pool.signUpInBackground(username, password, attrs, emptyMap(), object : SignUpHandler {
            override fun onSuccess(user: CognitoUser?, signUpResult: SignUpResult?) {
                val needsConfirmation = signUpResult?.codeDeliveryDetails != null
                callback(Result.success(needsConfirmation))
            }
            override fun onFailure(exception: Exception?) {
                callback(Result.failure(exception ?: Exception("Sign up failed")))
            }
        })
    }

    /**
     * Confirm sign-up with the code sent to the user's email.
     */
    fun confirmSignUp(username: String, code: String, callback: (Result<Unit>) -> Unit) {
        pool.getUser(username.trim()).confirmSignUpInBackground(code.trim(), false, object : GenericHandler {
            override fun onSuccess() {
                callback(Result.success(Unit))
            }
            override fun onFailure(exception: Exception?) {
                callback(Result.failure(exception ?: Exception("Confirmation failed")))
            }
        })
    }

    /**
     * Returns the current session's ID token (JWT) or null if not signed in / session expired.
     */
    fun getIdToken(callback: (Result<String?>) -> Unit) {
        val user = pool.currentUser ?: run {
            callback(Result.success(null))
            return
        }
        user.getSessionInBackground(object : AuthenticationHandler {
            override fun onSuccess(session: com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserSession?, device: com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoDevice?) {
                callback(Result.success(session?.idToken?.jwtToken))
            }
            override fun getAuthenticationDetails(authenticationContinuation: com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationContinuation?, userId: String?) {
                callback(Result.failure(Exception("Session expired")))
            }
            override fun getMFACode(continuation: com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.MultiFactorAuthenticationContinuation?) {
                callback(Result.failure(Exception("MFA required")))
            }
            override fun authenticationChallenge(continuation: com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.ChallengeContinuation?) {
                callback(Result.failure(Exception("Auth challenge")))
            }
            override fun onFailure(exception: Exception?) {
                callback(Result.success(null))
            }
        })
    }

    companion object {
        private const val USER_POOL_ID = "us-east-2_qscPZox2h"
        private const val CLIENT_ID = "45i90gg5jtm20ddvq3rs6d0k8b"
    }
}
