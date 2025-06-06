package com.anmi.camera.uvcplay

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.anmi.camera.uvcplay.model.CaseModel
import com.anmi.camera.uvcplay.state.AddCaseState
import com.anmi.camera.uvcplay.ui.CaseAdapter
import com.anmi.camera.uvcplay.utils.Utils
import com.anmi.camera.uvcplay.utils.Utils.setDebounceClickListener
import com.base.MyLog
import com.pedro.encoder.Frame
import dagger.hilt.android.AndroidEntryPoint
import dev.alejandrorosas.apptemplate.R


@AndroidEntryPoint
class CasesChooseActivity : AppCompatActivity(R.layout.activity_cases_choose){

    private val viewModel by viewModels<MainEntryViewModel>()
    private lateinit var caseListRv: RecyclerView
    private lateinit var loadingFl: FrameLayout
    private var selectedItem: CaseModel? = null
    private var wsUrl: String? = null
    private var visitId: String? = null
    private var streamSuccess: Boolean? = false

    companion object {
        private const val TAG = "[CasesChooseActivity]"
    }

    private fun doOnCreate() {

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // 监听点击
        findViewById<Toolbar>(R.id.toolbar).setNavigationOnClickListener {
            // 处理返回
            setResult(Activity.RESULT_CANCELED, null)
            finish()
        }

        // 创建 Adapter，并传入选择回调
        val adapter = CaseAdapter(emptyList()) { item ->
            selectedItem = item
            // 选中回调：可以更新界面或存储选中状态
            Toast.makeText(this,
                "选中：${item.case_debtor}", Toast.LENGTH_SHORT).show()
        }

        // 底部按钮点击
        findViewById<Button>(R.id.btn_start_visit).setDebounceClickListener(1000L) {
            if (selectedItem == null)  {
                Toast.makeText(this,
                    "请选中案件后开始外访", Toast.LENGTH_SHORT).show()
            } else {
                visitId = getVisitId()
                MyLog.d(TAG + "开始外访, visitId:$visitId")
                visitId?.let { it1 -> viewModel.addCase(selectedItem!!, it1) }
            }
        }
        loadingFl  = findViewById(R.id.loading_overlay)
        val progressBar: ProgressBar= findViewById(R.id.progress_bar)

        caseListRv  = findViewById(R.id.rv_cases)
        caseListRv.setHasFixedSize(true)
        caseListRv.adapter = adapter
        caseListRv.layoutManager = LinearLayoutManager(this)

        viewModel.cases.observe(this) { posts ->
            adapter.submitList(posts)
        }

        // 1. 先观察 ViewModel 中的状态
        viewModel.addCaseState.observe(this) { state ->
            when (state) {
                is AddCaseState.Idle -> {
                    loadingFl.visibility = View.GONE
                }
                is AddCaseState.Loading -> {
                    loadingFl.visibility = View.VISIBLE
                }
                is AddCaseState.Success -> {
                    loadingFl.visibility = View.GONE
                    Toast.makeText(this, "开启外访成功", Toast.LENGTH_SHORT).show()
                    // 可选：state.data 就是后台返回的 data
                    val resultIntent = Intent().apply {
                        putExtra("case", selectedItem)
                        putExtra("visit_id", visitId)
                    }
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish()
                }
                is AddCaseState.Error -> {
                    loadingFl.visibility = View.GONE
                    Toast.makeText(this,
                        "开启外访失败，请稍后再试", Toast.LENGTH_SHORT).show()
                    viewModel.idle()
                }
            }
        }

        viewModel.loadPosts()

        visitId = intent.extras?.getString("visit_id")
        wsUrl =intent.extras?.getString("ws_url")
        streamSuccess =intent.extras?.getBoolean("stream_success")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        doOnCreate()
    }

    override fun onStop() {
        super.onStop()
        MyLog.d(TAG + "onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        MyLog.d(TAG + "onDestroy")
    }

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
//        super.onBackPressed()
    }

    private fun getVisitId():String? {
        if (!TextUtils.isEmpty(visitId)){
            return visitId
        }

        if (streamSuccess == true){//推流已开始时，使用推流中的visitId
            return wsUrl?.substringAfterLast('/')
        }

        //推流未开始时，开始外访生成新visitId
        return Utils.generateVisitId()
    }
}
