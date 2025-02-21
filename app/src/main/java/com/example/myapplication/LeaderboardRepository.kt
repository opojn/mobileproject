package com.example.myapplication


import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LeaderboardRepository(private val leaderboardDao: LeaderboardDao) {

    suspend fun insertScore(playerName: String, score: Int) {
        val leaderboard = Leaderboard(playerName = playerName, score = score)
        println("DEBUG: Repository - Inserting Score -> $leaderboard")

        leaderboardDao.insert(leaderboard)

        val scores = leaderboardDao.getAllScores()
        println("DEBUG: Repository - Scores After Insert -> ${scores.joinToString { "${it.playerName}: ${it.score}" }}")
    }

    suspend fun getLeaderboard(): List<Leaderboard> {
        return leaderboardDao.getAllScores()
    }
}

