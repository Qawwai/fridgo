package com.cnlab.fridgo.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.cnlab.fridgo.data.FoodCategory
import com.cnlab.fridgo.databinding.ItemShoppingBinding

/**
 * Each missing ingredient becomes a card with its category emoji and a single
 * "Shop" action that opens it in Fridgo's in-app store (DummyJSON-backed),
 * pre-searched for that item. This replaced the old hand-off to external Korean
 * storefronts, whose URLs were geo/bot-blocked and only showed Chrome error
 * pages.
 */
class ShoppingAdapter(
    private val items: List<String>,
    private val onFindInShop: (item: String, view: View) -> Unit
) : RecyclerView.Adapter<ShoppingAdapter.VH>() {

    inner class VH(val b: ItemShoppingBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemShoppingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val name = items[position]
        val category = FoodCategory.of(name)

        holder.b.textEmoji.text = category.emoji
        holder.b.textItem.text = name
        holder.b.textStoreHint.text = "Tap to find it in the shop"
        holder.b.btnFindInShop.setOnClickListener { onFindInShop(name, holder.b.btnFindInShop) }
    }
}
