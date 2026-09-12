package com.cnlab.fridgo.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.cnlab.fridgo.R
import com.cnlab.fridgo.data.CartStore
import com.cnlab.fridgo.data.ShopProductDto
import com.cnlab.fridgo.databinding.ActivityShopDetailBinding

/**
 * Buyer "item detail": full image, price, rating/stock, description and a
 * quantity stepper. Adds the chosen quantity to the cart and can jump straight
 * to checkout. Receives the product as a Serializable Intent extra (same idiom
 * as [RecipeDetailActivity]).
 */
class ShopDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PRODUCT = "extra_product"
    }

    private lateinit var binding: ActivityShopDetailBinding
    private var quantity = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShopDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        @Suppress("DEPRECATION")
        val product = intent.getSerializableExtra(EXTRA_PRODUCT) as? ShopProductDto
        if (product == null) { finish(); return }
        title = product.title

        Glide.with(binding.image).load(product.heroImage).into(binding.image)
        binding.textTitle.text = product.title
        binding.textPrice.text = product.priceLabel
        binding.textMeta.text = buildString {
            append("⭐ %.1f".format(product.rating))
            product.brand?.takeIf { it.isNotBlank() }?.let { append("   ·   ").append(it) }
            append("   ·   ").append(if (product.stock > 0) "In stock (${product.stock})" else "Out of stock")
        }
        binding.textDescription.text = product.description

        updateQtyLabel()
        binding.btnMinus.setOnClickListener {
            if (quantity > 1) { quantity--; updateQtyLabel() }
        }
        binding.btnPlus.setOnClickListener { quantity++; updateQtyLabel() }

        binding.btnAddToCart.setOnClickListener {
            SoundManager.play(SoundManager.Fx.CART)
            Anim.bounce(binding.btnAddToCart) {
                CartStore.add(product, quantity)
                Toast.makeText(this, "Added $quantity × ${product.title} to cart", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, CartActivity::class.java))
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }
        }
    }

    private fun updateQtyLabel() { binding.textQty.text = quantity.toString() }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
