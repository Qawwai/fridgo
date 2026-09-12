package com.cnlab.fridgo.data

import androidx.room.*

@Dao
interface FridgeDao {
    @Query("SELECT * FROM fridge_items")
    suspend fun getAll(): List<FridgeEntity>

    @Query("SELECT COUNT(*) FROM fridge_items")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: FridgeEntity): Long

    @Update
    suspend fun update(item: FridgeEntity)

    @Delete
    suspend fun delete(item: FridgeEntity)
}

@Dao
interface StatsDao {
    @Query("SELECT * FROM stats WHERE id = 0")
    suspend fun get(): StatsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(stats: StatsEntity)
}
