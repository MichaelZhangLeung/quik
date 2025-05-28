package com.anmi.camera.uvcplay.ui

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.alejandrorosas.apptemplate.R

class PocAlertDialog private constructor() : DialogFragment() {


    // 定义 Lambda 类型回调
    private var onConfirm: (() -> Unit)? = null
    private var onCancel: (() -> Unit)? = null

    companion object {
        private const val ARG_TITLE = "title"
        private const val ARG_CONTENT = "content"
        private const val ARG_CANCEL = "withCancel"
        private const val ARG_TEXT_CANCEL = "ARG_TEXT_CANCEL"
        private const val ARG_TEXT_CONFIRM = "ARG_TEXT_CONFIRM"

        fun newInstance(title: String, content: String, withCancel:Boolean, cancelText: String?, confirmText:  String?,
                        onConfirm: () -> Unit = {}, // 默认空实现
                        onCancel: () -> Unit = {}) = PocAlertDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_TITLE, title)
                    putString(ARG_CONTENT, content)
                    putString(ARG_TEXT_CANCEL, cancelText)
                    putString(ARG_TEXT_CONFIRM, confirmText)
                    putBoolean(ARG_CANCEL, withCancel)
                }

                this.onConfirm = onConfirm
                this.onCancel = onCancel
            }
        }


    override fun onStart() {
        super.onStart()
        // 拿到 Dialog 的 Window，设置宽高
        dialog?.window?.let { window ->
            val params = window.attributes

            val widthDp = 307
            val heightDp = 176

            val metrics = resources.displayMetrics
            val widthPx = (widthDp * metrics.density).toInt()
            val heightPx = (heightDp * metrics.density).toInt()

            // 宽度设为屏幕的 90%
//            val width = (resources.displayMetrics.widthPixels * 0.9).toInt()
//            // 高度 WRAP_CONTENT，也可以指定固定值或屏幕比例
//            val height = ViewGroup.LayoutParams.WRAP_CONTENT

            params.width = widthPx
            params.height = heightPx
            window.attributes = params
        }
    }
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext(), R.style.AppDialogTheme).create().apply {
            setView(createDialogView())
            window?.setBackgroundDrawableResource(R.drawable.dialog_background)
        }
    }

    private fun createDialogView(): View {
        return layoutInflater.inflate(getDialogLayout(), null).apply {
            arguments?.run {
                findViewById<TextView>(R.id.tvTitle).text = getString(ARG_TITLE)
                findViewById<TextView>(R.id.tvContent).text = getString(ARG_CONTENT)
            }

            findViewById<Button>(R.id.btnConfirm).apply {
                text = arguments?.getString(ARG_TEXT_CONFIRM)
                setOnClickListener {
                    // 可扩展回调：parentFragment?.onConfirmClick()
                    dismiss()
                    onConfirm?.invoke()
                }
            }

            findViewById<Button>(R.id.btnCancel)?.apply {
                text = arguments?.getString(ARG_TEXT_CANCEL)
                setOnClickListener {
                    dismiss()
                    onCancel?.invoke()
                    // 可扩展回调：parentFragment?.onConfirmClick()
                }
            }
        }
    }

    private fun getDialogLayout(): Int {
        return when (arguments?.getBoolean(ARG_CANCEL)) {
            true -> R.layout.dialog_poc_alert_with_cacel
            false -> R.layout.dialog_poc_alert
            else -> R.layout.dialog_poc_alert
        }
    }



    override fun onDestroyView() {
        super.onDestroyView()
        onConfirm = null // 防止内存泄漏
        onCancel = null
    }
}
