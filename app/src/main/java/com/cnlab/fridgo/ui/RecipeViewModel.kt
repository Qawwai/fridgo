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
 * Holds the recipe-results UI state. All networking happens in the repository,
 * launched on viewModelScope so it survives configuration changes and never
 * blocks the UI thread.
 */
class RecipeViewModel(
    private val repo: RecipeRepository = RecipeRepository()
) : ViewModel() {

    sealed class UiState {
        object Loading : UiState()
        data class Success(val recipes: List<Recipe>) : UiState()
        data class Empty(val reason: String) : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _state = MutableLiveData<UiState>(UiState.Loading)
    val state: LiveData<UiState> = _state

    fun loadRecipesForExpiring() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            try {
                val expiring = FridgeStore.expiringIngredients(count = 3)
                if (expiring.isEmpty()) {
                    _state.value = UiState.Empty("Your fridge is empty. Add some items first.")
                    return@launch
                }
                val owned = FridgeStore.ownedKeys()
                val recipes = repo.recipesForExpiring(expiring, owned)
                if (recipes.isEmpty()) {
                    _state.value = UiState.Empty(
                        "No recipes found for: ${expiring.joinToString(", ")}"
                    )
                } else {
                    _state.value = UiState.Success(recipes)
                }
            } catch (e: Exception) {
                _state.value = UiState.Error(e.localizedMessage ?: "Something went wrong.")
            }
        }
    }
}
