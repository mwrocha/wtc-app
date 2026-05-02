package br.com.fiap.wtcconnecta.data.remote

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.Interceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "http://192.168.0.4:8080/"

    var authToken: String? = null

    // ── Callback chamado quando sessão é invalidada (401) ─────────────────────
    // Registrado pelo NavGraph ao iniciar o app
    var onSessionExpired: (() -> Unit)? = null

    private val authInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder().apply {
            authToken?.let { token ->
                addHeader("Authorization", "Bearer $token")
            }
        }.build()
        chain.proceed(request)
    }

    // ── Interceptor que detecta 401 e dispara logout ──────────────────────────
    private val sessionInterceptor = Interceptor { chain ->
        val response = chain.proceed(chain.request())
        if (response.code == 401) {
            // Limpa o token em memória
            authToken = null
            // Dispara o callback de sessão expirada na thread principal
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                onSessionExpired?.invoke()
            }
        }
        response
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(sessionInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}