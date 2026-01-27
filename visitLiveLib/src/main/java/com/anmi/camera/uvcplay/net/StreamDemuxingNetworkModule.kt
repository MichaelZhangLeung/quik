package com.anmi.camera.uvcplay.net

import com.base.MyLog
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object StreamDemuxingNetworkModule {
//    @Provides
//    @Named("streamBaseUrl")
//    fun provideBaseUrl(): String = "https://test-ai.duyansoft.com/ai/stream-demuxing/"
    @Provides
    @Named("streamBaseUrl")
    fun provideBaseUrl(): String = "https://myvap.duyansoft.com/algorithm/visit-api/"
//    https://myvap.duyansoft.com/algorithm/visit-api/v1/visit/stream/start
//    https://myvap.duyansoft.com/algorithm/visit-api/v1/visit/stream/stop

    @Provides
    @Singleton
    @Named("streamRetrofit")
    fun provideRetrofit(
        @Named("streamBaseUrl") baseUrl: String,
        @Named("streamOkHttp") client: OkHttpClient
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    fun provideApiService(@Named("streamRetrofit") retrofit: Retrofit): StreamDemuxApiService {
        return retrofit.create(StreamDemuxApiService::class.java)
    }



    @Provides
    @Singleton
    @Named("streamLoggingInterceptor")
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        val interceptor = HttpLoggingInterceptor { message ->
            MyLog.d("[OkHttp]$message")
        }
        interceptor.level = HttpLoggingInterceptor.Level.BODY
        return interceptor
    }

    @Provides
    @Singleton
    @Named("streamOkHttp")
    fun provideOkHttpClient(
        @Named("streamLoggingInterceptor") loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            // 可选：设置超时
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
