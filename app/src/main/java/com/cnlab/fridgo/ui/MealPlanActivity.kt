package com.cnlab.fridgo.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.cnlab.fridgo.R
import com.cnlab.fridgo.data.FridgeStore
import com.cnlab.fridgo.data.MealPlanStore
import com.cnlab.fridgo.data.RecipeRepository
import com.cnlab.fridgo.databinding.ActivityMealPlanBinding
import com.cnlab.fridgo.model.PlannedMeal
import kotlinx.coroutines.launch

/**
 * The user's cooking plan. Lists every meal they intend to make, marks which
 * ingredients they're still missing (live against the current fridge), and rolls
 * the whole plan's shortfall into a single shopping list. Cooking a meal deducts
 * its ingredients from the fridge.
 */
class MealPlanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMealPlanBinding
    private lateinit var adapter: PlannedMealAdapter
    private val repo = RecipeRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMealPlanBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "Meal plan"

        adapter = PlannedMealAdapter(
            rows = emptyList(),
            onCook = { meal -> cook(meal) },
            onRemove = { meal ->
                SoundManager.play(SoundManager.Fx.DELETE)
                lifecycleScope.launch { MealPlanStore.remove(meal); refresh() }
            }
        )
        binding.recyclerPlan.adapter = adapter

        binding.btnShopAll.setOnClickListener {
            SoundManager.play(SoundManager.Fx.CART)
            Anim.bounce(binding.btnShopAll) {
                val missing = aggregateMissing()
                if (missing.isEmpty()) {
                    Toast.makeText(this, "Nothing to buy — you have it all!", Toast.LENGTH_SHORT).show()
                } else {
                    startActivity(
                        Intent(this, ShoppingActivity::class.java)
                            .putExtra(ShoppingActivity.EXTRA_MISSING, ArrayList(missing))
                    )
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun rows(): List<PlanRow> {
        val owned = FridgeStore.ownedKeys()
        return MealPlanStore.all().map { meal ->
            val missing = if (meal.cooked) emptyList()
            else repo.computeMissing(meal.ingredients, owned)
            PlanRow(meal, missing)
        }
    }

    /** Distinct ingredients missing across all not-yet-cooked meals. */
    private fun aggregateMissing(): List<String> {
        val owned = FridgeStore.ownedKeys()
        return MealPlanStore.planned()
            .flatMap { repo.computeMissing(it.ingredients, owned) }
            .distinctBy { it.lowercase() }
    }

    private fun refresh() {
        val rows = rows()
        adapter.update(rows)
        binding.recyclerPlan.scheduleLayoutAnimation()

        val planned = MealPlanStore.activeCount()
        val toBuy = aggregateMissing().size
        binding.textSummary.text =
            if (planned == 0) "No meals planned yet"
            else "🍳 $planned meal${if (planned > 1) "s" else ""} planned   •   🛒 $toBuy to buy"

        val empty = rows.isEmpty()
        binding.textEmpty.visibility = if (empty) View.VISIBLE else View.GONE
        binding.btnShopAll.visibility = if (toBuy > 0) View.VISIBLE else View.GONE
    }

    private fun cook(meal: PlannedMeal) {
        SoundManager.play(SoundManager.Fx.SUCCESS)
        lifecycleScope.launch {
            val usedUp = FridgeStore.deductForMeal(meal.ingredientKeys)
            MealPlanStore.markCooked(meal)
            refresh()
            val msg = if (usedUp.isEmpty()) "Enjoy your ${meal.name}!"
            else "Cooked! Used up: ${usedUp.joinToString(", ")}"
            Toast.makeText(this@MealPlanActivity, msg, Toast.LENGTH_LONG).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
