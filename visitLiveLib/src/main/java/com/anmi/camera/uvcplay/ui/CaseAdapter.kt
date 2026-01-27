package com.anmi.camera.uvcplay.ui


import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.anmi.camera.uvcplay.model.CaseModel
import com.bumptech.glide.Glide
import dev.alejandrorosas.apptemplate.R
import dev.alejandrorosas.apptemplate.databinding.ItemSelectCaseBinding

class CaseAdapter(
    private var items: List<CaseModel>,
    private val onSelect: (selectedItem: CaseModel) -> Unit
) : RecyclerView.Adapter<CaseAdapter.CaseViewHolder>() {

    // 记录当前选中的位置
    private var selectedPosition = RecyclerView.NO_POSITION

    inner class CaseViewHolder(
        private val binding: ItemSelectCaseBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        init {
            // 单选框或整行点击都触发选择
            binding.rbSelect.setOnClickListener {
                val oldPos = selectedPosition
                selectedPosition = bindingAdapterPosition
                notifyItemChanged(oldPos)
                notifyItemChanged(selectedPosition)
                onSelect(items[selectedPosition])
            }
            binding.root.setOnClickListener {
                binding.rbSelect.performClick()
            }
        }

        fun bind(item: CaseModel, isSelected: Boolean) {
            // 设置文本
            binding.tvName.text = item.case_debtor
            val context = binding.tvCaseNo.context
            binding.tvCaseNo.text = context.getString(R.string.text_predix_format_case,
                context.getString(R.string.text_predix_case_number),
                item.case_id)
            binding.tvAddress.text = item.visit_address

            // 加载头像或显示占位
            if (!item.debtor_image.isNullOrEmpty()) {
                Glide.with(binding.ivAvatar.context)
                    .load(item.debtor_image)
                    .placeholder(R.drawable.ic_avatar_placeholder)
                    .into(binding.ivAvatar)
            } else {
                binding.ivAvatar.setImageResource(R.drawable.ic_avatar_placeholder)
            }

            // 单选状态
            binding.rbSelect.isChecked = isSelected
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CaseViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemSelectCaseBinding.inflate(inflater, parent, false)
        return CaseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CaseViewHolder, position: Int) {
        holder.bind(items[position], position == selectedPosition)
    }

    override fun getItemCount(): Int = items.size

    fun submitList(newList: List<CaseModel>) {
        items = newList
        notifyDataSetChanged()                                // 整体刷新，可用 DiffUtil 优化 :contentReference[oaicite:9]{index=9}
    }
}

