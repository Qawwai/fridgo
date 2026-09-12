package com.cnlab.fridgo.data

import androidx.room.*

@Dao
interface PlannedMealDao {
    @Query("SELECT * FROM planned_meals ORDER BY addedAt DESC")
    suspend fun getAll(): List<PlannedMealEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(meal: PlannedMealEntity): Long

    @Update
    suspend fun update(meal: PlannedMealEntity)

    @Delete
    suspend fun delete(meal: PlannedMealEntity)
}
