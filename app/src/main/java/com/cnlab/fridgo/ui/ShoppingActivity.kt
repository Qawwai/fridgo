package com.cnlab.fridgo.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.cnlab.fridgo.R
import com.cnlab.fridgo.databinding.ActivityShoppingBinding

/**
 * Lists the missing ingredients and lets the user shop for each one inside
 * Fridgo's own store ([ShopActivity], DummyJSON-backed), pre-searched for the
 * item. This replaced an older hand-off to external Korean storefronts, whose
 * URLs were geo/bot-blocked (HTTP 403) and only opened Chrome error pages.
 */
class ShoppingActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_MISSING = "extra_missing"
    }

    private lateinit var binding: ActivityShoppingBinding
    private lateinit var missing: ArrayList<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShoppingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "Shopping list"

        Anim.enter(binding.textHeader)

        @Suppress("DEPRECATION")
        missing = intent.getStringArrayListExtra(EXTRA_MISSING) ?: arrayListOf()

        binding.textHeader.text =
            "Buy what you're missing (${missing.size})"

        binding.recyclerShopping.adapter = ShoppingAdapter(missing) { item, view ->
            SoundManager.play(SoundManager.Fx.CART)
            Anim.bounce(view) { openShop(item) }
        }

        binding.btnSearchAll.setOnClickListener {
            SoundManager.play(SoundManager.Fx.CART)
            Anim.bounce(binding.btnSearchAll) {
                openShop("") // full catalog
            }
        }
    }

    /** Open the in-app store, pre-searched for [query] (blank = full catalog). */
    private fun openShop(query: String) {
        startActivity(
            Intent(this, ShopActivity::class.java)
                .putExtra(ShopActivity.EXTRA_QUERY, query)
        )
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
