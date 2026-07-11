package com.eren76.mangly

import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object SharedImageHttpClient {
    val instance: OkHttpClient by lazy {
        val dispatcher = Dispatcher().apply {
            maxRequests = 5
            maxRequestsPerHost = 3
        }

        OkHttpClient.Builder()
            .dispatcher(dispatcher)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
