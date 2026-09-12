package com.cnlab.fridgo.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.cnlab.fridgo.R
import com.cnlab.fridgo.databinding.ActivityDiscoverBinding
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

/**
 * Free-form recipe discovery over TheMealDB: full-text search, browse by
 * category, browse by cuisine, and a random "surprise me". Results reuse the
 * recipe card list and still show how many ingredients you already have.
 */
class DiscoverActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDiscoverBinding
    private val viewModel: DiscoverViewModel by viewModels()
    private lateinit var adapter: RecipeAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDiscoverBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "Discover recipes"

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

        binding.editSearch.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val q = v.text.toString().trim()
                if (q.isNotEmpty()) {
                    hideKeyboard()
                    clearChipSelections()
                    viewModel.search(q)
                }
                true
            } else false
        }

        binding.btnSurprise.setOnClickListener {
            SoundManager.play(SoundManager.Fx.TAP)
            Anim.bounce(binding.btnSurprise) {
                clearChipSelections()
                viewModel.surprise()
            }
        }

        viewModel.categories.observe(this) { names ->
            populateChips(binding.chipCategories, names) { viewModel.byCategory(it) }
        }
        viewModel.cuisines.observe(this) { names ->
            populateChips(binding.chipCuisines, names) { viewModel.byCuisine(it) }
        }
        viewModel.state.observe(this) { render(it) }

        viewModel.loadFilters()
    }

    private fun render(state: DiscoverViewModel.UiState) {
        binding.progress.visibility =
            if (state is DiscoverViewModel.UiState.Loading) View.VISIBLE else View.GONE
        val showList = state is DiscoverViewModel.UiState.Success
        binding.recyclerResults.visibility = if (showList) View.VISIBLE else View.GONE
        binding.textStatus.visibility =
            if (state is DiscoverViewModel.UiState.Success || state is DiscoverViewModel.UiState.Loading)
                View.GONE else View.VISIBLE

        when (state) {
            is DiscoverViewModel.UiState.Idle ->
                binding.textStatus.text = "Search, pick a category, or hit 🎲"
            is DiscoverViewModel.UiState.Success -> {
                adapter.update(state.recipes)
                binding.recyclerResults.scheduleLayoutAnimation()
                SoundManager.play(SoundManager.Fx.SUCCESS)
            }
            is DiscoverViewModel.UiState.Empty -> binding.textStatus.text = state.reason
            is DiscoverViewModel.UiState.Error ->
                binding.textStatus.text = "Network error: ${state.message}"
            is DiscoverViewModel.UiState.Loading -> Unit
        }
    }

    private fun populateChips(group: ChipGroup, names: List<String>, onPick: (String) -> Unit) {
        if (group.childCount > 0 || names.isEmpty()) return
        names.forEach { name ->
            val chip = Chip(this).apply {
                text = name
                isCheckable = true
                setOnClickListener {
                    if (isChecked) {
                        SoundManager.play(SoundManager.Fx.TAP)
                        binding.editSearch.text?.clear()
                        onPick(name)
                    }
                }
            }
            group.addView(chip)
        }
    }

    private fun clearChipSelections() {
        binding.chipCategories.clearCheck()
        binding.chipCuisines.clearCheck()
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(binding.editSearch.windowToken, 0)
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
