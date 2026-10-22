package az.mtk.app.data.api

import az.mtk.app.data.auth.AuthRepository
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

class AuthInterceptor(private val auth: AuthRepository) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = auth.currentAccessToken()
        val request = if (token != null) {
            chain.request().newBuilder().header("Authorization", "Bearer $token").build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}

/** A 401 means the backend rejected the token — refresh once and replay the call. */
class TokenAuthenticator(private val auth: AuthRepository) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.request.header("Authorization") == null) return null
        if (responseCount(response) >= 2) return null

        val token = auth.forceRefresh() ?: return null
        return response.request.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count += 1
            prior = prior.priorResponse
        }
        return count
    }
}

class ApiException(message: String, val status: Int? = null) : Exception(message)
