package com.pbl6.fitme.settings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.pbl6.fitme.R

class SettingsAdapter(
    private val items: List<SettingOption>,
    private val onItemClick: (SettingOption) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_TITLE_ONLY = 0
        private const val TYPE_TITLE_WITH_VALUE = 1
    }

    inner class TitleViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tvTitleSetting)
        val arrow: ImageView = view.findViewById(R.id.ivArrow)
    }

    inner class ValueViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tvTitleSetting2)
        val value: TextView = view.findViewById(R.id.tvValue2)
        val arrow: ImageView = view.findViewById(R.id.ivArrow2)
    }

    override fun getItemViewType(position: Int): Int {
        return if (items[position].type == SettingType.TITLE_WITH_VALUE)
            TYPE_TITLE_WITH_VALUE
        else
            TYPE_TITLE_ONLY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_TITLE_WITH_VALUE) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_setting_with_value, parent, false)
            ValueViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_setting, parent, false)
            TitleViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        when (holder) {
            is TitleViewHolder -> {
                holder.title.text = item.title
                holder.itemView.setOnClickListener { onItemClick(item) }
            }

            is ValueViewHolder -> {
                holder.title.text = item.title
                holder.value.text = item.value
                holder.itemView.setOnClickListener { onItemClick(item) }
            }
        }
    }

    override fun getItemCount() = items.size
}
