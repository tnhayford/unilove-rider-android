package com.unilove.rider.data.api

import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object NetworkModule {
  private const val PROD_HOST = "unilove.iderwell.com"
  private const val PROD_FALLBACK_IP = "138.199.192.146"
  private val apiByBaseUrl = ConcurrentHashMap<String, RiderApi>()

  @OptIn(ExperimentalSerializationApi::class)
  fun riderApi(baseUrl: String): RiderApi {
    val normalizedBaseUrl = baseUrl.trim().trimEnd('/') + "/"
    val targetHost = normalizedBaseUrl.toHttpUrlOrNull()?.host

    return apiByBaseUrl.getOrPut(normalizedBaseUrl) {
      val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
      val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .dns(buildDnsResolver(targetHost))
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

  private fun buildDnsResolver(targetHost: String?): Dns {
    if (targetHost.isNullOrBlank()) return Dns.SYSTEM
    if (!targetHost.equals(PROD_HOST, ignoreCase = true)) return Dns.SYSTEM

    return object : Dns {
      override fun lookup(hostname: String): List<InetAddress> {
        return try {
          Dns.SYSTEM.lookup(hostname)
        } catch (err: UnknownHostException) {
          if (hostname.equals(PROD_HOST, ignoreCase = true)) {
            listOf(InetAddress.getByName(PROD_FALLBACK_IP))
          } else {
            throw err
          }
        }
      }
    }
  }
}
