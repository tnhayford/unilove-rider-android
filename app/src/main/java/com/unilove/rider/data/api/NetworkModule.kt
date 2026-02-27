package com.unilove.rider.data.api

import com.unilove.rider.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object NetworkModule {
  private val apiByBaseUrl = ConcurrentHashMap<String, RiderApi>()

  @OptIn(ExperimentalSerializationApi::class)
  fun riderApi(baseUrl: String): RiderApi {
    val normalizedBaseUrl = baseUrl.trim().trimEnd('/') + "/"

    return apiByBaseUrl.getOrPut(normalizedBaseUrl) {
      val logging = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
          HttpLoggingInterceptor.Level.BASIC
        } else {
          HttpLoggingInterceptor.Level.NONE
        }
      }
      val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .addInterceptor(logging)
        .build()

      val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
      }

      Retrofit.Builder()
        .baseUrl(normalizedBaseUrl)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(RiderApi::class.java)
    }
  }
}
