package com.example.myapplication

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "leaderboard")
data class Leaderboard(
    @PrimaryKey(autoGenerate = true) val id: Long? = null,
    @ColumnInfo(name = "playerName") val playerName: String,
    @ColumnInfo(name = "score") val score: Int
)
