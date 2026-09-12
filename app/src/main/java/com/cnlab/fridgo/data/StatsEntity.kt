package com.cnlab.fridgo.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single-row table tracking the app's purpose metric:
 *  - saved: items the user removed while still fresh/soon (used in time)
 *  - wasted: items the user removed after they had expired
 */
@Entity(tableName = "stats")
data class StatsEntity(
    @PrimaryKey val id: Int = 0,
    val saved: Int = 0,
    val wasted: Int = 0
)
