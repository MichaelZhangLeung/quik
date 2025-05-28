package com.anmi.camera.uvcplay.net

import com.anmi.camera.uvcplay.model.BaseBean
import com.anmi.camera.uvcplay.model.CaseModel
import retrofit2.http.GET

interface ApiService {
    @GET("v1/visit/case/list")
    suspend fun getPosts(): BaseBean<List<CaseModel>>
}
