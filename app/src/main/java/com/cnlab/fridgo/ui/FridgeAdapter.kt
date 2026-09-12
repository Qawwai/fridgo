package com.cnlab.fridgo.ui

import android.animation.ObjectAnimator
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.cnlab.fridgo.data.FoodCategory
import com.cnlab.fridgo.databinding.ItemFridgeBinding
import com.cnlab.fridgo.model.Freshness
import com.cnlab.fridgo.model.FridgeItem
import java.text.SimpleDateFormat
import java.util.Locale

class FridgeAdapter(
    private var items: List<FridgeItem>,
    private val onDelete: (FridgeItem, View) -> Unit,
    private val onClick: (FridgeItem) -> Unit = {}
) : RecyclerView.Adapter<FridgeAdapter.VH>() {

    private val dateFmt = SimpleDateFormat("MMM d", Locale.getDefault())

    inner class VH(val b: ItemFridgeBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemFridgeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.b.textEmoji.text = FoodCategory.emojiFor(item.name)
        val amt = item.amountLabel
        holder.b.textName.text =
            if (amt.isEmpty()) item.name else "${item.name}   $amt"

        val days = item.daysRemaining
        val on = dateFmt.format(item.expiryEpochMillis)
        holder.b.textDays.text = when {
            days < 0 -> "Expired ${-days}d ago · was $on"
            days == 0L -> "Use today! · $on"
            days == 1L -> "1 day left · $on"
            else -> "$days days left · $on"
        }

        val color = when (item.state) {
            Freshness.URGENT -> Color.parseColor("#E0625A")
            Freshness.SOON -> Color.parseColor("#E8A23D")
            Freshness.FRESH -> Color.parseColor("#3DA88F")
        }
        holder.b.textDays.setTextColor(color)
        // tint the rounded status dot
        holder.b.statusDot.background?.setTint(color)
        // tint the progress fill (the clip layer of the layer-list)
        holder.b.freshnessBar.progressTintList = ColorStateList.valueOf(color)

        // Animate the bar filling up to its value.
        holder.b.freshnessBar.progress = 0
        ObjectAnimator.ofInt(holder.b.freshnessBar, "progress", item.freshnessPercent).apply {
            duration = 650
            startDelay = 80L
            start()
        }

        holder.b.btnDelete.setOnClickListener { onDelete(item, holder.itemView) }
        holder.b.root.setOnClickListener { onClick(item) }
    }

    fun update(newItems: List<FridgeItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
