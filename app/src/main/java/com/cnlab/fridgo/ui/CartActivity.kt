package com.cnlab.fridgo.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.cnlab.fridgo.data.CartStore
import com.cnlab.fridgo.data.FridgeStore
import com.cnlab.fridgo.data.ShelfLifePredictor
import com.cnlab.fridgo.databinding.ActivityCartBinding
import com.cnlab.fridgo.model.FridgeItem
import kotlinx.coroutines.launch

/**
 * The buyer cart + simulated checkout. Lists every cart line with a quantity
 * stepper, shows a running subtotal, and on checkout confirms the (simulated)
 * order. Because DummyJSON doesn't really fulfil orders, checkout closes the
 * loop with the rest of Fridgo instead: it offers to drop the purchased items
 * straight into the user's fridge (with auto-predicted shelf lives).
 */
class CartActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCartBinding
    private lateinit var adapter: CartAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCartBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "Your cart"

        adapter = CartAdapter(CartStore.lines()) { refresh() }
        binding.recyclerCart.adapter = adapter

        binding.btnCheckout.setOnClickListener {
            SoundManager.play(SoundManager.Fx.CART)
            Anim.bounce(binding.btnCheckout) {
                if (!CartStore.isEmpty()) confirmCheckout()
            }
        }

        refresh()
    }

    private fun refresh() {
        val lines = CartStore.lines()
        adapter.update(lines)
        val empty = lines.isEmpty()
        binding.textEmpty.visibility = if (empty) View.VISIBLE else View.GONE
        binding.recyclerCart.visibility = if (empty) View.GONE else View.VISIBLE
        binding.footer.visibility = if (empty) View.GONE else View.VISIBLE
        binding.textSubtotal.text = "Subtotal: $%.2f".format(CartStore.subtotal())
    }

    private fun confirmCheckout() {
        val count = CartStore.count()
        AlertDialog.Builder(this)
            .setTitle("Place order")
            .setMessage("Checkout $count item${if (count == 1) "" else "s"} for ${"$%.2f".format(CartStore.subtotal())}?")
            .setPositiveButton("Place order") { _, _ -> placeOrder() }
            .setNegativeButton("Keep shopping", null)
            .show()
    }

    private fun placeOrder() {
        SoundManager.play(SoundManager.Fx.SUCCESS)
        // Snapshot before we offer to stock the fridge / clear the cart.
        val purchased = CartStore.lines()
        AlertDialog.Builder(this)
            .setTitle("✅ Order placed!")
            .setMessage("Your groceries are on the way. Add them to your fridge so Fridgo can track freshness and suggest recipes?")
            .setPositiveButton("Add to fridge") { _, _ -> stockFridge(purchased) }
            .setNegativeButton("No thanks") { _, _ ->
                CartStore.clear()
                finish()
            }
            .show()
    }

    private fun stockFridge(purchased: List<CartStore.CartLine>) {
        lifecycleScope.launch {
            val now = System.currentTimeMillis()
            for (line in purchased) {
                val shelfDays = ShelfLifePredictor.predict(line.product.title).days
                FridgeStore.add(
                    FridgeItem(
                        name = line.product.title,
                        purchaseEpochMillis = now,
                        shelfLifeDays = shelfDays,
                        quantity = line.quantity
                    )
                )
            }
            CartStore.clear()
            android.widget.Toast.makeText(
                this@CartActivity,
                "Added ${purchased.size} item${if (purchased.size == 1) "" else "s"} to your fridge",
                android.widget.Toast.LENGTH_SHORT
            ).show()
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
