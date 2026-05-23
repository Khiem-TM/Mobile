package com.vitalai.data.remote

import com.vitalai.data.local.TokenManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenManager: TokenManager,
    @Named("refreshAuthApi") private val refreshAuthApi: AuthApi
) : Authenticator {

    private val refreshLock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.priorResponseCount >= MAX_AUTH_RETRIES) return null

        val requestAccessToken = response.request.header("Authorization")
            ?.removePrefix("Bearer ")
            ?.trim()

        return synchronized(refreshLock) {
            runBlocking {
                val latestAccessToken = tokenManager.accessToken.first()
                if (!latestAccessToken.isNullOrBlank() && latestAccessToken != requestAccessToken) {
                    return@runBlocking response.request.withAccessToken(latestAccessToken)
                }

                val refreshToken = tokenManager.refreshToken.first()
                if (refreshToken.isNullOrBlank()) {
                    tokenManager.clearTokens()
                    return@runBlocking null
                }

                try {
                    val refreshResponse = refreshAuthApi.refresh(
                        mapOf("refresh_token" to refreshToken)
                    )
                    val newAccessToken = refreshResponse.body()?.data?.accessToken

                    if (refreshResponse.isSuccessful && !newAccessToken.isNullOrBlank()) {
                        tokenManager.saveAccessToken(newAccessToken)
                        response.request.withAccessToken(newAccessToken)
                    } else {
                        tokenManager.clearTokens()
                        null
                    }
                } catch (_: Exception) {
                    null
                }
            }
        }
    }

    private fun Request.withAccessToken(accessToken: String): Request {
        return newBuilder()
            .header("Authorization", "Bearer $accessToken")
            .build()
    }

    private val Response.priorResponseCount: Int
        get() {
            var count = 1
            var prior = priorResponse
            while (prior != null) {
                count++
                prior = prior.priorResponse
            }
            return count
        }

    private companion object {
        const val MAX_AUTH_RETRIES = 2
    }
}
