package com.example.myapplication

import LeaderboardRepository
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LeaderboardActivity : AppCompatActivity() {
    private lateinit var repository: LeaderboardRepository
    private lateinit var leaderboardTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leaderboard)

        leaderboardTextView = findViewById(R.id.leaderboard_text)
        val backButton: Button = findViewById(R.id.button_back)

        val playerName = intent?.getStringExtra("PLAYER_NAME") ?: "Unknown Player"
        val score = intent?.getIntExtra("PLAYER_SCORE", -1) ?: -1

        /*lifecycleScope.launch(Dispatchers.IO) {
            val database = LeaderboardDatabase.getDatabase(applicationContext)
            repository = LeaderboardRepository(database.leaderboardDao())

            withContext(Dispatchers.Main) {
                displayLeaderboard()
            }
        }

        backButton.setOnClickListener {
            finish() // Go back to main menu
        }*/
        if (score != -1) {
            insertScore(playerName, score)
        } else {
            displayLeaderboard() // If no new score, just display the leaderboard
        }

        backButton.setOnClickListener {
            finish() // Go back to main menu
        }
    }


    private fun insertScore(playerName: String, score: Int) {
        lifecycleScope.launch {
            try {
                println("DEBUG: Inserting Score -> $playerName: $score")
                repository.insertScore(playerName, score)

                val scores = repository.getLeaderboard()
                println("DEBUG: Scores in DB -> ${scores.joinToString { "${it.playerName}: ${it.score}" }}")

                displayLeaderboard() // Refresh leaderboard after inserting a new score
            } catch (e: Exception) {
                e.printStackTrace()
                leaderboardTextView.text = "Error inserting score!"
            }
        }
    }


    private fun displayLeaderboard() {
        lifecycleScope.launch {
            try {
                val scores = repository.getLeaderboard() // Just fetch the scores, don't reinitialize DB
                val leaderboardText = scores.joinToString("\n") { "${it.playerName}: ${it.score}" }

                withContext(Dispatchers.Main) {
                    leaderboardTextView.text = leaderboardText.ifEmpty { "No scores yet!" }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                leaderboardTextView.text = "Error loading leaderboard!"
            }
        }
    }

}
