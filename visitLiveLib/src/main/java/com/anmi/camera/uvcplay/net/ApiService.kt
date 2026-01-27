package com.anmi.camera.uvcplay.net

import com.anmi.camera.uvcplay.model.BaseBean
import com.anmi.camera.uvcplay.model.CaseModel
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface ApiService {
    @GET("v1/visit/case/list")
    suspend fun getPosts(): BaseBean<List<CaseModel>>
//    @GET("v1/visit/case/log/add")
//    suspend fun addCase(@QueryMap params: Map<String, @JvmSuppressWildcards Any>): BaseBean<Any>
//    @GET("v1/visit/case/log/add")
//    suspend fun addCase(@Query("case_id") case_id: String,): BaseBean<Any>
    @POST("v1/visit/case/log/add")
    suspend fun addCase(@Body caseModel: CaseModel): BaseBean<Any>
    @GET("v1/visit/end/time/update")
    suspend fun endVisit(@Query("visit_id") visitId: String): BaseBean<Any>
}
