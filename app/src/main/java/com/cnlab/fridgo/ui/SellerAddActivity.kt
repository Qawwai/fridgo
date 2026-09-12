package com.cnlab.fridgo.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.cnlab.fridgo.data.ShopRepository
import com.cnlab.fridgo.databinding.ActivitySellerAddBinding
import kotlinx.coroutines.launch

/**
 * The seller side of the store ("adding new item as seller" in the MAP slide).
 * A simple product form posted to DummyJSON's create endpoint. DummyJSON
 * simulates the write — it returns the new product with a freshly assigned id,
 * which we surface as confirmation — so this is a faithful end-to-end seller
 * flow without needing any account or API key.
 */
class SellerAddActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySellerAddBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySellerAddBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "List a product"

        binding.btnPublish.setOnClickListener {
            SoundManager.play(SoundManager.Fx.TAP)
            Anim.bounce(binding.btnPublish) { publish() }
        }
    }

    private fun publish() {
        val title = binding.editTitle.text.toString().trim()
        val priceText = binding.editPrice.text.toString().trim()
        val description = binding.editDescription.text.toString().trim()

        if (title.isEmpty()) {
            binding.editTitle.error = "Give your product a name"
            return
        }
        val price = priceText.toDoubleOrNull()
        if (price == null || price <= 0.0) {
            binding.editPrice.error = "Enter a valid price"
            return
        }

        setLoading(true)
        lifecycleScope.launch {
            val created = ShopRepository().addProduct(title, price, description)
            setLoading(false)
            if (created == null) {
                Toast.makeText(this@SellerAddActivity, "Couldn't publish — check your connection.", Toast.LENGTH_SHORT).show()
                return@launch
            }
            SoundManager.play(SoundManager.Fx.SUCCESS)
            AlertDialog.Builder(this@SellerAddActivity)
                .setTitle("✅ Listed!")
                .setMessage("“${created.title}” is now listed for ${created.priceLabel} (product #${created.id}).")
                .setPositiveButton("Done") { _, _ -> finish() }
                .show()
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progress.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnPublish.isEnabled = !loading
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
