package com.cnlab.fridgo.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.cnlab.fridgo.R
import com.cnlab.fridgo.data.MealPlanStore
import com.cnlab.fridgo.databinding.ActivityRecipeDetailBinding
import com.cnlab.fridgo.model.PlannedMeal
import com.cnlab.fridgo.model.Recipe
import kotlinx.coroutines.launch

/**
 * Full recipe view: hero image, category, instructions, full ingredient list,
 * and a clearly separated "You're missing" section that links to shopping.
 * The shop button uses the CART sound to match the buy action downstream.
 */
class RecipeDetailActivity : AppCompatActivity() {

    companion object { const val EXTRA_RECIPE = "extra_recipe" }

    private lateinit var binding: ActivityRecipeDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecipeDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        @Suppress("DEPRECATION")
        val recipe = (intent.getSerializableExtra(EXTRA_RECIPE) as? Recipe) ?: run {
            finish(); return
        }

        title = recipe.name
        Glide.with(this).load(recipe.thumbUrl).into(binding.imageHero)
        binding.textCategory.text = recipe.category
        binding.textIngredients.text = recipe.ingredients.joinToString("\n") { ing ->
            "• ${ing.measure} ${ing.name}".trim()
        }
        binding.textInstructions.text = recipe.instructions

        // Subtle staggered entrance for the key blocks.
        Anim.enter(binding.textCategory)
        Anim.enter(binding.textMissing, delay = 90)

        if (MealPlanStore.contains(recipe.id)) binding.btnAddPlan.text = "✓  In your plan"

        binding.btnAddPlan.setOnClickListener {
            if (MealPlanStore.contains(recipe.id)) {
                Toast.makeText(this, "Already in your meal plan", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            SoundManager.play(SoundManager.Fx.ADD)
            Anim.bounce(binding.btnAddPlan) {
                lifecycleScope.launch {
                    MealPlanStore.add(
                        PlannedMeal(
                            recipeId = recipe.id,
                            name = recipe.name,
                            thumbUrl = recipe.thumbUrl,
                            area = recipe.area,
                            ingredients = recipe.ingredients
                        )
                    )
                    binding.btnAddPlan.text = "✓  Added to plan"
                    Toast.makeText(this@RecipeDetailActivity, "Added to meal plan", Toast.LENGTH_SHORT).show()
                }
            }
        }

        if (recipe.missing.isEmpty()) {
            binding.textMissing.text = "You have everything you need!"
            binding.btnShop.visibility = View.GONE
        } else {
            binding.textMissing.text =
                "You're missing:\n" + recipe.missing.joinToString("\n") { "• $it" }
            binding.btnShop.visibility = View.VISIBLE
            binding.btnShop.setOnClickListener {
                SoundManager.play(SoundManager.Fx.CART)
                Anim.bounce(binding.btnShop) {
                    startActivity(
                        Intent(this, ShoppingActivity::class.java)
                            .putExtra(ShoppingActivity.EXTRA_MISSING, ArrayList(recipe.missing))
                    )
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_plan, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_plan -> {
            startActivity(Intent(this, MealPlanActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
