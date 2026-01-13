package com.anmi.camera.uvcplay.net

import com.anmi.camera.uvcplay.model.BaseBean
import retrofit2.http.GET
import retrofit2.http.Query

interface StreamDemuxApiService {
//    @GET("api/v1/start")
    @GET("v1/visit/stream/start")
    suspend fun start(@Query("stream_id") streamUrl: String): BaseBean<Any>

//    @GET("api/v1/stop")
    @GET("v1/visit/stream/stop")
    suspend fun stop(@Query("stream_id") streamUrl: String): BaseBean<Any>
}
