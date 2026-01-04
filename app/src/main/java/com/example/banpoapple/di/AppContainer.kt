package com.example.banpoapple.di

import com.example.banpoapple.BuildConfig
import com.example.banpoapple.auth.AuthRepository
import com.example.banpoapple.auth.AdminRepository
import com.example.banpoapple.content.ContentRepository
import com.example.banpoapple.network.NetworkModule

object AppContainer {
    val authRepository: AuthRepository by lazy {
        AuthRepository(
            client = NetworkModule.okHttpClient,
            api = NetworkModule.api,
            demoMode = BuildConfig.DEMO_MODE
        )
    }

    val contentRepository: ContentRepository by lazy {
        ContentRepository(
            api = NetworkModule.api,
            client = NetworkModule.okHttpClient,
            demoMode = BuildConfig.DEMO_MODE
        )
    }

    val adminRepository: AdminRepository by lazy {
        AdminRepository(
            api = NetworkModule.api,
            client = NetworkModule.okHttpClient,
            demoMode = BuildConfig.DEMO_MODE
        )
    }
}
