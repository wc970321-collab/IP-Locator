package com.example.data.api

import com.example.data.model.IpApiCoResponse
import com.example.data.model.FreeIpApiResponse
import com.example.data.model.IpInfoIoResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

interface IpApiCoService {
    @GET("json/")
    suspend fun getCurrentIpInfo(): IpApiCoResponse

    @GET("{ip}/json/")
    suspend fun getIpInfo(@Path("ip") ip: String): IpApiCoResponse
}

interface FreeIpApiService {
    @GET("api/json")
    suspend fun getCurrentIpInfo(): FreeIpApiResponse

    @GET("api/json/{ip}")
    suspend fun getIpInfo(@Path("ip") ip: String): FreeIpApiResponse
}

interface IpInfoIoService {
    @GET("json")
    suspend fun getCurrentIpInfo(): IpInfoIoResponse

    @GET("{ip}/json")
    suspend fun getIpInfo(@Path("ip") ip: String): IpInfoIoResponse
}

object IpRetrofitClient {
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    val ipApiCoService: IpApiCoService by lazy {
        Retrofit.Builder()
            .baseUrl("https://ipapi.co/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(IpApiCoService::class.java)
    }

    val freeIpApiService: FreeIpApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://freeipapi.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(FreeIpApiService::class.java)
    }

    val ipInfoIoService: IpInfoIoService by lazy {
        Retrofit.Builder()
            .baseUrl("https://ipinfo.io/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(IpInfoIoService::class.java)
    }
}
