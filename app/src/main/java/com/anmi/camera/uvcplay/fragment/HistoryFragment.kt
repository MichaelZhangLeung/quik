package com.anmi.camera.uvcplay.fragment

import android.app.AlertDialog
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.RelativeLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import com.anmi.camera.uvcplay.bridge.DeviceInfoBridge
import com.anmi.camera.uvcplay.bridge.DeviceInfoBridge.Companion.BRIDGE_NAME
import com.base.MyLog
import com.base.ThreadHelper
import com.tencent.smtt.export.external.interfaces.JsPromptResult
import com.tencent.smtt.export.external.interfaces.JsResult
import com.tencent.smtt.export.external.interfaces.SslError
import com.tencent.smtt.export.external.interfaces.SslErrorHandler
import com.tencent.smtt.sdk.WebChromeClient
import com.tencent.smtt.sdk.WebSettings
import com.tencent.smtt.sdk.WebView
import com.tencent.smtt.sdk.WebViewClient
import dagger.hilt.android.AndroidEntryPoint
import dev.alejandrorosas.apptemplate.MainViewModel
import dev.alejandrorosas.apptemplate.R

@AndroidEntryPoint
class HistoryFragment : Fragment() {

    companion object {
        private const val TAG = "[HistoryFragment]"
    }

    private val viewModel: MainViewModel by activityViewModels()

    private var layoutView: View? = null
    private var webView: WebView? = null

//    private val mUrl:String = "https://172.21.66.2:15658/algorithm/ai-envoy-h5/#/history"
    private val mUrl:String = "https://myvap.duyansoft.com/algorithm/ai-envoy-h5/#/history"

    // 记录上次 reload 的系统时间（毫秒）
    private var lastReloadTime: Long = 0L
    // 设置最小刷新间隔
    private val minReloadInterval = 10_000L

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.fragment_history, container, false).also { view ->
        layoutView = view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        viewModel.caseState.observe(viewLifecycleOwner) {
//                state,
//            -> switchLoadWeb(state.code)
//        }
//        viewModel.caseState.value?.code?.let { switchLoadWeb(it) }
        switchLoadWeb(1)
    }



    private fun initWebView(view:View) {

//        webView = view.findViewById(R.id.main_web)
        webView = WebView(requireContext())
        webView!!.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
        )
        webView?.let {
            val settings: WebSettings = it.settings
            settings.javaScriptEnabled = true // 支持JS
            settings.allowFileAccess = true
            settings.setSupportZoom(true)
            settings.databaseEnabled = true
            settings.domStorageEnabled = true // 支持DOM Storage

            settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW


            // 注入桥接对象，映射到 JS 全局的 AndroidBridge
            it.addJavascriptInterface(context?.let { it1 -> DeviceInfoBridge(it1) }, BRIDGE_NAME)


            it.webViewClient = object : WebViewClient() {

                override fun onPageStarted(p0: WebView?, p1: String?, p2: Bitmap?) {
                    super.onPageStarted(p0, p1, p2)
                    MyLog.e("$TAG#onPageStarted: $p1")
                }


                override fun onReceivedError(webView: WebView?, errorCode: Int, description: String, failingUrl: String) {
                    MyLog.e(
                        TAG +
                            "#onReceivedError: " + errorCode
                            + ", description: " + description
                            + ", url: " + failingUrl,
                    )
                }
                override fun onReceivedSslError(p0: WebView?, handler: SslErrorHandler?, p2: SslError?) {
                    MyLog.e("onReceivedSslError:$p2")
                    super.onReceivedSslError(p0, handler, p2)
                    handler?.proceed()
                }
            }
            // 处理 JS 弹窗，如 alert、confirm、prompt
            it.webChromeClient = object : WebChromeClient() {
                override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult): Boolean {
                    AlertDialog.Builder(context)
                        .setTitle(getString(R.string.text_prompt))
                        .setMessage(message)
                        .setPositiveButton(android.R.string.ok) { dialog, which -> result.confirm() }
                        .setCancelable(false)
                        .create()
                        .show()
                    return true
                }

                override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: JsResult): Boolean {
                    AlertDialog.Builder(context)
                        .setTitle(getString(R.string.text_confirm))
                        .setMessage(message)
                        .setPositiveButton(android.R.string.ok) { dialog, which -> result.confirm() }
                        .setNegativeButton(android.R.string.cancel) { dialog, which -> result.cancel() }
                        .create()
                        .show()
                    return true
                }

                override fun onJsPrompt(
                    view: WebView?,
                    url: String?,
                    message: String?,
                    defaultValue: String?,
                    result: JsPromptResult,
                ): Boolean {
                    val input = EditText(context)
                    input.setText(defaultValue)
                    AlertDialog.Builder(context)
                        .setTitle(message)
                        .setView(input)
                        .setPositiveButton(android.R.string.ok) { dialog, which -> result.confirm(input.text.toString()) }
                        .setNegativeButton(android.R.string.cancel) { dialog, which -> result.cancel() }
                        .create()
                        .show()
                    return true
                }
            }

            it.clearCache(true)

//            ThreadHelper.getInstance().postToUIThread({
//                // 加载H5页面
//                it.loadUrl("https://test-ai.duyansoft.com/algorithm/ai-envoy/")
//            }, 1000L)
        }

    }

    private fun switchLoadWeb(startLoad:  Int){
        when (startLoad) {
            1 -> {
                layoutView?.let {
                    it.findViewById<FrameLayout>(R.id.fl_web_loaded)?.visibility = View.VISIBLE
                    it.findViewById<RelativeLayout>(R.id.rl_web_empty)?.visibility = View.GONE
                    initWebView(it)
                    it.findViewById<FrameLayout>(R.id.fl_web_loaded).removeAllViews()
                    it.findViewById<FrameLayout>(R.id.fl_web_loaded).addView(webView)
                    ThreadHelper.getInstance().postToUIThread(
                        {

                            val state = viewLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
                                && webView?.settings != null  // 已初始化
                            // 加载H5页面
                            MyLog.e("${TAG}加载历史webview:$state")
//                        webView?.loadUrl("https://test-ai.duyansoft.com/algorithm/ai-envoy/")
//                            webView?.loadUrl("https://test-ai.duyansoft.com/algorithm/ai-envoy-h5/")
                            webView?.loadUrl(mUrl)
                        },
                        1000L,
                    )
                }

            }
            0 -> {
                layoutView?.findViewById<FrameLayout>(R.id.fl_web_loaded)?.visibility = View.INVISIBLE
                layoutView?.findViewById<RelativeLayout>(R.id.rl_web_empty)?.visibility = View.VISIBLE
                webView?.loadUrl("about:blank")
                webView?.clearCache(true)
                webView?.destroy()
                webView = null
            }
            2 -> {
//                layoutView?.findViewById<FrameLayout>(R.id.fl_web_loaded)?.visibility = View.INVISIBLE
//                layoutView?.findViewById<RelativeLayout>(R.id.rl_web_empty)?.visibility = View.VISIBLE

//                webView?.loadUrl("about:blank")
//                webView?.clearCache(true)
//                webView?.destroy()
//                webView = null
            }
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        MyLog.e("$TAG onHiddenChanged hidden:$hidden")
        if (!hidden) {
//            reloadWebContent()
            notifyVisible()
        }
    }

    override fun onResume() {
        super.onResume()
        MyLog.e("$TAG onResume in, isVisible:$isVisible")
        if (isVisible){
//            reloadWebContent()
            notifyVisible()
        }
    }

    private fun notifyVisible() {
        MyLog.e("$TAG notifyVisible in")
        try {
//            webView?.evaluateJavascript("window.postMessage('tab-back')", null)
            webView?.evaluateJavascript("window.postMessage('tab-back', '*');", null)
        } catch (e: Throwable) {
            MyLog.e("$TAG notifyVisible exception:$e", e)
        }
    }


    private fun reloadWebContent() {
        MyLog.e("$TAG reloadWebContent in")

        val now = System.currentTimeMillis()
        if (now - lastReloadTime < minReloadInterval){
            return
        }

        val state = viewLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
            && webView?.settings != null  // 已初始化
        // 加载H5页面
        MyLog.e("${TAG} webview state:$state")
        if (lastReloadTime == 0L){
            MyLog.e("${TAG}首次加载")
            webView?.loadUrl(mUrl)
        } else{
            webView?.clearCache(true)
            ThreadHelper.getInstance().postToUIThread({
                MyLog.e("${TAG}开始重载")
                webView?.reload()
            }, 200L)
        }

        lastReloadTime = now
    }


    override fun onDestroyView() {
        super.onDestroyView()
        MyLog.e("$TAG ===>>onDestroyView")

        // 防泄漏
        webView?.destroy()
        webView = null
        lastReloadTime = 0
    }
}
