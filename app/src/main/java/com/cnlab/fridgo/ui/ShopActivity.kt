package com.cnlab.fridgo.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.cnlab.fridgo.R
import com.cnlab.fridgo.data.CartStore
import com.cnlab.fridgo.data.FoodCategory
import com.cnlab.fridgo.databinding.ActivityShopBinding
import com.google.android.material.chip.Chip

/**
 * The in-app store catalog (buyer "item list"). Browses a rich merged grocery
 * catalog (DummyJSON + ~500 TheMealDB ingredients) in a 2-column grid, with
 * category filter chips (Fruit / Vegetable / Meat / …), live search, a result
 * count, a cart action with the live item count, and a seller "+" FAB.
 *
 * Can be opened pre-searched via [EXTRA_QUERY] (used by the recipe shopping list).
 */
class ShopActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_QUERY = "extra_query"
    }

    private lateinit var binding: ActivityShopBinding
    private val viewModel: ShopViewModel by viewModels()
    private lateinit var adapter: ShopProductAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShopBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "Shop"

        adapter = ShopProductAdapter(
            emptyList(),
            onOpen = { product, view ->
                SoundManager.play(SoundManager.Fx.TAP)
                Anim.bounce(view) {
                    startActivity(
                        Intent(this, ShopDetailActivity::class.java)
                            .putExtra(ShopDetailActivity.EXTRA_PRODUCT, product)
                    )
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                }
            },
            onAdd = { product, view ->
                SoundManager.play(SoundManager.Fx.CART)
                Anim.bounce(view) {
                    CartStore.add(product)
                    invalidateOptionsMenu()
                    android.widget.Toast.makeText(
                        this, "Added ${product.title} to cart", android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
        binding.recyclerShop.layoutManager = GridLayoutManager(this, 2)
        binding.recyclerShop.adapter = adapter

        binding.editSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { viewModel.search(s?.toString().orEmpty()) }
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })

        binding.fabSell.setOnClickListener {
            SoundManager.play(SoundManager.Fx.TAP)
            Anim.bounce(binding.fabSell) {
                startActivity(Intent(this, SellerAddActivity::class.java))
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }
        }

        viewModel.categories.observe(this) { buildCategoryChips(it) }
        viewModel.state.observe(this) { render(it) }

        intent.getStringExtra(EXTRA_QUERY)?.trim()?.takeIf { it.isNotEmpty() }?.let {
            binding.editSearch.setText(it)
        }
        viewModel.loadCatalog()
    }

    override fun onResume() {
        super.onResume()
        invalidateOptionsMenu() // keep the cart badge fresh
    }

    /** Build the "All" + per-category filter chips once the catalog is known. */
    private fun buildCategoryChips(categories: List<FoodCategory>) {
        val group = binding.chipCategories
        if (group.childCount > 0 || categories.isEmpty()) return

        fun addChip(label: String, category: FoodCategory?) {
            val chip = Chip(this).apply {
                text = label
                isCheckable = true
                isChecked = category == viewModel.selectedCategory
                setOnClickListener {
                    SoundManager.play(SoundManager.Fx.TAP)
                    viewModel.setCategory(category)
                }
            }
            group.addView(chip)
        }

        addChip("All", null)
        categories.forEach { addChip("${it.emoji} ${it.label}", it) }
        (group.getChildAt(0) as? Chip)?.isChecked = viewModel.selectedCategory == null
    }

    private fun render(state: ShopViewModel.UiState) {
        binding.progress.visibility =
            if (state is ShopViewModel.UiState.Loading) View.VISIBLE else View.GONE
        val showList = state is ShopViewModel.UiState.Success
        binding.recyclerShop.visibility = if (showList) View.VISIBLE else View.GONE
        binding.textStatus.visibility =
            if (state is ShopViewModel.UiState.Empty || state is ShopViewModel.UiState.Error)
                View.VISIBLE else View.GONE
        binding.textCount.visibility = if (showList) View.VISIBLE else View.GONE

        when (state) {
            is ShopViewModel.UiState.Success -> {
                adapter.update(state.products)
                binding.textCount.text = "${state.products.size} items"
                binding.recyclerShop.scheduleLayoutAnimation()
            }
            is ShopViewModel.UiState.Empty -> binding.textStatus.text = state.reason
            is ShopViewModel.UiState.Error ->
                binding.textStatus.text = "Network error: ${state.message}"
            is ShopViewModel.UiState.Loading -> Unit
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_shop_cart, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val count = CartStore.count()
        menu.findItem(R.id.action_cart)?.title =
            if (count > 0) "🛒 Cart ($count)" else "🛒 Cart"
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_cart -> {
            SoundManager.play(SoundManager.Fx.TAP)
            startActivity(Intent(this, CartActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            true
        }
        R.id.action_profile -> {
            SoundManager.play(SoundManager.Fx.TAP)
            startActivity(Intent(this, ProfileActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
