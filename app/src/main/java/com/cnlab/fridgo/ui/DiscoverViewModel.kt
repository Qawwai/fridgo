package com.cnlab.fridgo.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cnlab.fridgo.data.FridgeStore
import com.cnlab.fridgo.data.RecipeRepository
import com.cnlab.fridgo.model.Recipe
import kotlinx.coroutines.launch

/**
 * Drives the Discover screen. Exposes the category/cuisine filter lists and the
 * recipe results for whichever discovery action the user took (free-text search,
 * a category, a cuisine, or "surprise me"). All networking is in the repository.
 */
class DiscoverViewModel(
    private val repo: RecipeRepository = RecipeRepository()
) : ViewModel() {

    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        data class Success(val recipes: List<Recipe>) : UiState()
        data class Empty(val reason: String) : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _state = MutableLiveData<UiState>(UiState.Idle)
    val state: LiveData<UiState> = _state

    private val _categories = MutableLiveData<List<String>>(emptyList())
    val categories: LiveData<List<String>> = _categories

    private val _cuisines = MutableLiveData<List<String>>(emptyList())
    val cuisines: LiveData<List<String>> = _cuisines

    /** Load the filter chips once. */
    fun loadFilters() {
        viewModelScope.launch {
            if (_categories.value.isNullOrEmpty()) _categories.value = repo.categoryNames()
            if (_cuisines.value.isNullOrEmpty()) _cuisines.value = repo.areaNames()
        }
    }

    fun search(query: String) = run("No recipes named “$query”.") {
        repo.searchByName(query, FridgeStore.ownedKeys())
    }

    fun byCategory(category: String) = run("No recipes in $category.") {
        repo.browseByCategory(category, FridgeStore.ownedKeys())
    }

    fun byCuisine(area: String) = run("No $area recipes found.") {
        repo.browseByArea(area, FridgeStore.ownedKeys())
    }

    fun surprise() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            try {
                val r = repo.surprise(FridgeStore.ownedKeys())
                _state.value = if (r == null) UiState.Empty("Couldn't fetch a recipe — try again.")
                else UiState.Success(listOf(r))
            } catch (e: Exception) {
                _state.value = UiState.Error(e.localizedMessage ?: "Something went wrong.")
            }
        }
    }

    private fun run(emptyMsg: String, block: suspend () -> List<Recipe>) {
        _state.value = UiState.Loading
        viewModelScope.launch {
            try {
                val recipes = block()
                _state.value = if (recipes.isEmpty()) UiState.Empty(emptyMsg)
                else UiState.Success(recipes)
            } catch (e: Exception) {
                _state.value = UiState.Error(e.localizedMessage ?: "Something went wrong.")
            }
        }
    }
}
