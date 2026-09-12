package com.cnlab.fridgo.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [FridgeEntity::class, StatsEntity::class, PlannedMealEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun fridgeDao(): FridgeDao
    abstract fun statsDao(): StatsDao
    abstract fun plannedMealDao(): PlannedMealDao

    companion object {
        /** v1 -> v2: add the optional grams column to fridge items. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE fridge_items ADD COLUMN grams REAL")
            }
        }

        /** v2 -> v3: add the meal-plan table. */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS planned_meals (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        recipeId TEXT NOT NULL,
                        name TEXT NOT NULL,
                        thumbUrl TEXT NOT NULL,
                        area TEXT NOT NULL,
                        ingredientsJson TEXT NOT NULL,
                        cooked INTEGER NOT NULL,
                        addedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        @Volatile private var INSTANCE: AppDatabase? = null
        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext, AppDatabase::class.java, "fridgo.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build().also { INSTANCE = it }
            }
    }
}
