package com.anmi.camera.uvcplay

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.anmi.camera.uvcplay.model.CaseModel
import com.anmi.camera.uvcplay.ui.CaseAdapter
import com.base.MyLog
import dagger.hilt.android.AndroidEntryPoint
import dev.alejandrorosas.apptemplate.R


@AndroidEntryPoint
class CasesChooseActivity : AppCompatActivity(R.layout.activity_cases_choose){

    private val viewModel by viewModels<MainEntryViewModel>()
    private lateinit var caseListRv: RecyclerView
    private var selectedItem: CaseModel? = null

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
        findViewById<Button>(R.id.btn_start_visit).setOnClickListener {
            if (selectedItem == null)  {
                Toast.makeText(this,
                    "请选中案件后开始外访", Toast.LENGTH_SHORT).show()
            } else {
//                Toast.makeText(this,
//                    "开始案件外访：${selectedItem?.case_debtor}", Toast.LENGTH_SHORT).show()
                val resultIntent = Intent().apply {
                    putExtra("case", selectedItem)
                }
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            }
        }
        caseListRv  = findViewById(R.id.rv_cases)
        caseListRv.setHasFixedSize(true)
        caseListRv.adapter = adapter
        caseListRv.layoutManager = LinearLayoutManager(this)

        viewModel.cases.observe(this) { posts ->
            adapter.submitList(posts)
        }

        viewModel.loadPosts()
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
}
