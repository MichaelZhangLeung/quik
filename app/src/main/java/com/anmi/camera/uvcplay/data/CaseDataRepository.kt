package com.anmi.camera.uvcplay.data

import com.anmi.camera.uvcplay.model.CaseModel
import com.anmi.camera.uvcplay.net.ApiService
import com.base.MyLog
import com.base.log.Dlog
import jakarta.inject.Inject

class CaseDataRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun getCases(): List<CaseModel>? {
        return apiService.getPosts().toResult()
//        return listOf(
//            CaseModel(
//                case_debtor = "艳瑗",
//                case_id = "12008756",
//                visit_address = "浙江省杭州市余杭区西溪雅苑西区12幢3单",
//                debtor_image = "https://wp-regress.anmiai.com/recording/1000289/mm/860142068305993/2025/05/22/wx_6ebf2236-36d3-11f0-9b81-cf220647e930.png"
//            ),
//            CaseModel(
//                case_debtor = "艳瑗2",
//                case_id = "12008756",
//                visit_address = "北京市城南区友爱胡同420号",
//                debtor_image = "https://wp-regress.anmiai.com/recording/1000289/mm/860142068305993/2025/05/22/wx_6ebf2236-36d3-11f0-9b81-cf220647e930.png"
//            ),
//            CaseModel(
//                case_debtor = "艳瑗3",
//                case_id = "12008756",
//                visit_address = "浙江省杭州市余杭区西溪雅苑西区12幢3单",
//                debtor_image = "https://wp-regress.anmiai.com/recording/1000289/mm/860142068305993/2025/05/22/wx_6ebf2236-36d3-11f0-9b81-cf220647e930.png"
//            )
//        )
    }
    suspend fun addCase(case:CaseModel, visitId:String): Any? {
//        return apiService.addCase(case.toMap()).toResult()
        case.visit_id = visitId
        return apiService.addCase(case).toResult()
    }
    suspend fun endVisit(case:CaseModel?, visitId:String): Any? {
        return apiService.endVisit(visitId).toResult()
    }
}
