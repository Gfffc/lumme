package com.univesp.lumme.data.remote

import com.univesp.lumme.data.local.TokenStore
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * Injeta o JWT armazenado no DataStore em todas as chamadas ao backend.
 * Se o token não existir (usuário não logado), a requisição segue sem Authorization
 * e o backend responderá 401 — tratado na camada de repositório.
 */
class AuthInterceptor @Inject constructor(
    private val tokenStore: TokenStore
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { tokenStore.jwt() }
        val request = chain.request().newBuilder().apply {
            addHeader("Accept", "application/json")
            addHeader("Content-Type", "application/json")
            token?.let { addHeader("Authorization", "Bearer $it") }
        }.build()
        return chain.proceed(request)
    }
}
