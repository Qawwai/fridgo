package com.cnlab.fridgo.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cnlab.fridgo.databinding.ItemPlannedMealBinding
import com.cnlab.fridgo.model.PlannedMeal

/** One planned meal plus the live "missing from your fridge" list for it. */
data class PlanRow(val meal: PlannedMeal, val missing: List<String>)

class PlannedMealAdapter(
    private var rows: List<PlanRow>,
    private val onCook: (PlannedMeal) -> Unit,
    private val onRemove: (PlannedMeal) -> Unit
) : RecyclerView.Adapter<PlannedMealAdapter.VH>() {

    inner class VH(val b: ItemPlannedMealBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemPlannedMealBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun getItemCount() = rows.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val row = rows[position]
        val meal = row.meal
        holder.b.textName.text = meal.name
        Glide.with(holder.b.imageThumb).load(meal.thumbUrl).into(holder.b.imageThumb)

        holder.b.textSub.text = when {
            meal.cooked -> "✅ Cooked"
            row.missing.isEmpty() -> "Ready to cook — you have everything"
            else -> "Need ${row.missing.size}: ${row.missing.take(3).joinToString(", ")}"
        }

        // Cooked meals only keep the Remove action.
        holder.b.btnCook.visibility = if (meal.cooked) View.GONE else View.VISIBLE
        holder.b.root.alpha = if (meal.cooked) 0.6f else 1f

        holder.b.btnCook.setOnClickListener { onCook(meal) }
        holder.b.btnRemove.setOnClickListener { onRemove(meal) }
    }

    fun update(newRows: List<PlanRow>) {
        rows = newRows
        notifyDataSetChanged()
    }
}
