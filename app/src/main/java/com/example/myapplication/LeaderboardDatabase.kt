package com.example.myapplication

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.myapplication.LeaderboardDao

@Database(entities = [Leaderboard::class], version = 1, exportSchema = false)
abstract class LeaderboardDatabase : RoomDatabase() {
    abstract fun leaderboardDao(): LeaderboardDao

    companion object {
        @Volatile
        private var INSTANCE: LeaderboardDatabase? = null

        fun getDatabase(context: Context): LeaderboardDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LeaderboardDatabase::class.java,
                    "leaderboard_database"
                )
                    .fallbackToDestructiveMigration() // Handles schema changes
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
