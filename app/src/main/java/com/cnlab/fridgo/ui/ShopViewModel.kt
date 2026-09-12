package com.cnlab.fridgo.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cnlab.fridgo.data.FoodCategory
import com.cnlab.fridgo.data.ShopProductDto
import com.cnlab.fridgo.data.ShopRepository
import kotlinx.coroutines.launch

/**
 * Drives the in-app store. Loads the merged catalog once, then filters it
 * client-side by category chip and/or search text — so both are instant and
 * span every source. Also publishes which categories actually have items, so the
 * chip row only offers useful filters.
 */
class ShopViewModel(
    private val repo: ShopRepository = ShopRepository()
) : ViewModel() {

    sealed class UiState {
        object Loading : UiState()
        data class Success(val products: List<ShopProductDto>) : UiState()
        data class Empty(val reason: String) : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _state = MutableLiveData<UiState>(UiState.Loading)
    val state: LiveData<UiState> = _state

    /** Categories present in the catalog (drives the filter chips). */
    private val _categories = MutableLiveData<List<FoodCategory>>(emptyList())
    val categories: LiveData<List<FoodCategory>> = _categories

    private var all: List<ShopProductDto> = emptyList()
    private var activeCategory: FoodCategory? = null
    private var query: String = ""

    /** Fetch the merged catalog once. */
    fun loadCatalog() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            try {
                all = repo.catalog()
                if (all.isEmpty()) {
                    _state.value = UiState.Empty("Couldn't load the catalog — check your connection.")
                    return@launch
                }
                // Categories that actually have items, in the enum's natural order.
                val present = all.map { FoodCategory.of(it.title) }.toSet()
                _categories.value = FoodCategory.values().filter { it in present }
                apply()
            } catch (e: Exception) {
                _state.value = UiState.Error(e.localizedMessage ?: "Something went wrong.")
            }
        }
    }

    /** Filter by a category chip (null = All). */
    fun setCategory(category: FoodCategory?) {
        activeCategory = category
        apply()
    }

    /** Filter by search text (live). */
    fun search(text: String) {
        query = text.trim()
        apply()
    }

    val selectedCategory: FoodCategory? get() = activeCategory

    private fun apply() {
        if (all.isEmpty()) return
        val q = query.lowercase()
        val filtered = all.filter { p ->
            (activeCategory == null || FoodCategory.of(p.title) == activeCategory) &&
                (q.isEmpty() || p.title.lowercase().contains(q))
        }
        _state.value = if (filtered.isEmpty()) {
            UiState.Empty(
                if (query.isNotEmpty()) "No items match “$query”."
                else "Nothing in ${activeCategory?.label ?: "this category"} yet."
            )
        } else UiState.Success(filtered)
    }
}
