package com.cnlab.fridgo.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cnlab.fridgo.databinding.ItemRecipeBinding
import com.cnlab.fridgo.model.Recipe

class RecipeAdapter(
    private var items: List<Recipe>,
    private val onClick: (Recipe, View) -> Unit
) : RecyclerView.Adapter<RecipeAdapter.VH>() {

    inner class VH(val b: ItemRecipeBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemRecipeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val r = items[position]
        holder.b.textName.text = r.name
        holder.b.textHave.text = "You have ${r.haveCount}/${r.totalCount} ingredients"
        holder.b.textMissing.text =
            if (r.missing.isEmpty()) "You can make this now!"
            else "Missing ${r.missing.size}: ${r.missing.take(3).joinToString(", ")}"
        Glide.with(holder.b.image).load(r.thumbUrl).into(holder.b.image)
        holder.b.root.setOnClickListener { onClick(r, holder.itemView) }
    }

    fun update(newItems: List<Recipe>) {
        items = newItems
        notifyDataSetChanged()
    }
}
