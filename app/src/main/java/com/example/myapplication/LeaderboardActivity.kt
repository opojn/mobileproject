package com.example.myapplication

import android.content.Intent
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

        // Initialize the database and repository
        val database = LeaderboardDatabase.getDatabase(applicationContext)
        repository = LeaderboardRepository(database.leaderboardDao())

        val playerName = intent?.getStringExtra("PLAYER_NAME") ?: "Unknown Player"
        val score = intent?.getIntExtra("PLAYER_SCORE", -1) ?: -1

        if (score != -1) {
            insertScore(playerName, score)
        } else {
            displayLeaderboard() // If no new score, just display the leaderboard
        }

        backButton.setOnClickListener {
            val intent = Intent(this, StartMenuActivity::class.java)
            // Optionally, clear any lingering activities:
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }
    }

    private fun insertScore(playerName: String, score: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                println("DEBUG: Inserting Score -> $playerName: $score")
                repository.insertScore(playerName, score)

                val scores = repository.getLeaderboard()
                println("DEBUG: Scores in DB -> ${scores.joinToString { "${it.playerName}: ${it.score}" }}")

                withContext(Dispatchers.Main) {
                    displayLeaderboard() // Refresh leaderboard after inserting a new score
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    leaderboardTextView.text = "Error inserting score!"
                }
            }
        }
    }

    private fun displayLeaderboard() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val scores = repository.getLeaderboard() // Fetch scores on the IO thread
                val leaderboardText = scores.joinToString("\n") { "${it.playerName}: ${it.score}" }
                withContext(Dispatchers.Main) {
                    leaderboardTextView.text = leaderboardText.ifEmpty { "No scores yet!" }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    leaderboardTextView.text = "Error loading leaderboard!"
                }
            }
        }
    }
}
