package com.cnlab.fridgo.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cnlab.fridgo.data.FoodCategory
import com.cnlab.fridgo.data.ShopProductDto
import com.cnlab.fridgo.databinding.ItemShopProductBinding

/**
 * Grid of catalog products. Each card shows the product image, title and price,
 * with an "Add" button that drops one into the cart. Tapping the card opens the
 * product detail screen.
 */
class ShopProductAdapter(
    private var items: List<ShopProductDto>,
    private val onOpen: (ShopProductDto, View) -> Unit,
    private val onAdd: (ShopProductDto, View) -> Unit
) : RecyclerView.Adapter<ShopProductAdapter.VH>() {

    inner class VH(val b: ItemShopProductBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemShopProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val p = items[position]
        val cat = FoodCategory.of(p.title)
        holder.b.textCategory.text = "${cat.emoji} ${cat.label}"
        holder.b.textTitle.text = p.title
        holder.b.textPrice.text = p.priceLabel
        Glide.with(holder.b.image).load(p.heroImage).into(holder.b.image)
        holder.b.root.setOnClickListener { onOpen(p, holder.itemView) }
        holder.b.btnAdd.setOnClickListener { onAdd(p, holder.b.btnAdd) }
    }

    fun update(newItems: List<ShopProductDto>) {
        items = newItems
        notifyDataSetChanged()
    }
}
