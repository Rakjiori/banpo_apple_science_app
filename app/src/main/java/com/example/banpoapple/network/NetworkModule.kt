package com.example.banpoapple.network

import com.example.banpoapple.BuildConfig
import okhttp3.Interceptor
import okhttp3.JavaNetCookieJar
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.net.CookieManager
import java.net.CookiePolicy
import java.util.concurrent.TimeUnit
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

/**
 * 중앙에서 OkHttp/Retrofit과 쿠키 공유 설정을 제공합니다.
 */
object NetworkModule {

    internal val cookieManager = CookieManager().apply {
        setCookiePolicy(CookiePolicy.ACCEPT_ALL)
    }

    private val xsrfInterceptor = Interceptor { chain ->
        val request = chain.request()
        val xsrf = cookieManager.cookieStore.cookies.firstOrNull { it.name == "XSRF-TOKEN" }
        val needsHeader = xsrf != null && request.header("X-XSRF-TOKEN") == null

        val newRequest = if (needsHeader) {
            request.newBuilder()
                .addHeader("X-XSRF-TOKEN", xsrf!!.value)
                .build()
        } else {
            request
        }

        chain.proceed(newRequest)
    }

    val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        OkHttpClient.Builder()
            .cookieJar(JavaNetCookieJar(cookieManager))
            .addInterceptor(xsrfInterceptor)
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    val moshi: Moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    val api: BackendApi by lazy { retrofit.create(BackendApi::class.java) }
}
