package com.cnlab.fridgo.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cnlab.fridgo.data.CartStore
import com.cnlab.fridgo.databinding.ItemCartLineBinding

/**
 * Cart line list: image, title, line price and a quantity stepper (with the
 * minus turning into a remove at qty 1). Any change calls back so the host can
 * persist it to [CartStore] and refresh the subtotal.
 */
class CartAdapter(
    private var lines: List<CartStore.CartLine>,
    private val onChange: () -> Unit
) : RecyclerView.Adapter<CartAdapter.VH>() {

    inner class VH(val b: ItemCartLineBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemCartLineBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun getItemCount() = lines.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val line = lines[position]
        val p = line.product
        holder.b.textTitle.text = p.title
        holder.b.textLinePrice.text = "$%.2f".format(p.price * line.quantity)
        holder.b.textUnitPrice.text = "${p.priceLabel} each"
        holder.b.textQty.text = line.quantity.toString()
        Glide.with(holder.b.image).load(p.heroImage).into(holder.b.image)

        holder.b.btnMinus.setOnClickListener {
            CartStore.setQuantity(p.id, line.quantity - 1)
            onChange()
        }
        holder.b.btnPlus.setOnClickListener {
            CartStore.setQuantity(p.id, line.quantity + 1)
            onChange()
        }
        holder.b.btnRemove.setOnClickListener {
            CartStore.remove(p.id)
            onChange()
        }
    }

    fun update(newLines: List<CartStore.CartLine>) {
        lines = newLines
        notifyDataSetChanged()
    }
}
