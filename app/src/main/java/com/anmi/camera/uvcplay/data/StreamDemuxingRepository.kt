package com.anmi.camera.uvcplay.data

import com.anmi.camera.uvcplay.net.StreamDemuxApiService
import jakarta.inject.Inject

class StreamDemuxingRepository @Inject constructor(
    private val apiService: StreamDemuxApiService
) {
    suspend fun start(streamUrl:String): Int? {
        return apiService.start(streamUrl).toCode()
    }
    suspend fun stop(streamUrl:String): Int? {
        return apiService.stop(streamUrl).toCode()
    }
}
