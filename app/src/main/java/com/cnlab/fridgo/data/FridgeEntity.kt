package com.cnlab.fridgo.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cnlab.fridgo.model.FridgeItem

/** Room row backing a FridgeItem. Kept separate from the domain model. */
@Entity(tableName = "fridge_items")
data class FridgeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val purchaseEpochMillis: Long,
    val shelfLifeDays: Int,
    val quantity: Int = 1,
    val grams: Double? = null
)

fun FridgeEntity.toItem() = FridgeItem(
    name = name,
    purchaseEpochMillis = purchaseEpochMillis,
    shelfLifeDays = shelfLifeDays,
    id = id,
    quantity = quantity,
    grams = grams
)

fun FridgeItem.toEntity() = FridgeEntity(
    id = id,
    name = name,
    purchaseEpochMillis = purchaseEpochMillis,
    shelfLifeDays = shelfLifeDays,
    quantity = quantity,
    grams = grams
)
