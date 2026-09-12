package com.cnlab.fridgo.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.cnlab.fridgo.R
import com.cnlab.fridgo.data.FridgeStore
import com.cnlab.fridgo.databinding.ActivityRecipeResultsBinding

/**
 * Shows recipes matched to the soonest-to-expire ingredients, retrieved over
 * the network and ranked by fewest missing ingredients. Plays a SUCCESS sound
 * once results arrive; TAP when a recipe is opened.
 */
class RecipeResultsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecipeResultsBinding
    private val viewModel: RecipeViewModel by viewModels()
    private lateinit var adapter: RecipeAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecipeResultsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "Cook before it spoils"

        binding.textSubtitle.text =
            "Using: " + FridgeStore.expiringIngredients(3).joinToString(", ")
        Anim.enter(binding.textSubtitle)

        adapter = RecipeAdapter(emptyList()) { recipe, view ->
            SoundManager.play(SoundManager.Fx.TAP)
            Anim.bounce(view) {
                startActivity(
                    Intent(this, RecipeDetailActivity::class.java)
                        .putExtra(RecipeDetailActivity.EXTRA_RECIPE, recipe)
                )
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }
        }
        binding.recyclerResults.adapter = adapter

        viewModel.state.observe(this) { state ->
            when (state) {
                is RecipeViewModel.UiState.Loading -> {
                    binding.progress.visibility = View.VISIBLE
                    binding.textStatus.visibility = View.GONE
                    binding.recyclerResults.visibility = View.GONE
                }
                is RecipeViewModel.UiState.Success -> {
                    binding.progress.visibility = View.GONE
                    binding.recyclerResults.visibility = View.VISIBLE
                    binding.textStatus.visibility = View.GONE
                    adapter.update(state.recipes)
                    binding.recyclerResults.scheduleLayoutAnimation()
                    // SUCCESS sound — results are in.
                    SoundManager.play(SoundManager.Fx.SUCCESS)
                }
                is RecipeViewModel.UiState.Empty -> {
                    binding.progress.visibility = View.GONE
                    binding.recyclerResults.visibility = View.GONE
                    binding.textStatus.visibility = View.VISIBLE
                    binding.textStatus.text = state.reason
                }
                is RecipeViewModel.UiState.Error -> {
                    binding.progress.visibility = View.GONE
                    binding.recyclerResults.visibility = View.GONE
                    binding.textStatus.visibility = View.VISIBLE
                    binding.textStatus.text = "Network error: ${state.message}"
                }
            }
        }

        viewModel.loadRecipesForExpiring()
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
