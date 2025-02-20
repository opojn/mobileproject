package com.example.myapplication
import LeaderboardRepository
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class LeaderboardActivity : AppCompatActivity() {
    private lateinit var repository: LeaderboardRepository
    private lateinit var leaderboardTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leaderboard)

        leaderboardTextView = findViewById(R.id.leaderboard_text)
        val backButton: Button = findViewById(R.id.button_back)

        val database = LeaderboardDatabase.getDatabase(this)
        repository = LeaderboardRepository(database.leaderboardDao())

        backButton.setOnClickListener {
            finish() // Go back to main menu
        }

        // Get the score and player name from the intent
        val playerName = intent.getStringExtra("PLAYER_NAME") ?: "Unknown Player"
        val score = intent.getIntExtra("PLAYER_SCORE", -1)

        if (score != -1) {
            insertScore(playerName, score)
        } else {
            displayLeaderboard() // If no new score, just display the leaderboard
        }
    }

    private fun insertScore(playerName: String, score: Int) {
        lifecycleScope.launch {
            repository.insertScore(playerName, score)
            displayLeaderboard() // Refresh leaderboard after inserting a new score
        }
    }

    private fun displayLeaderboard() {
        lifecycleScope.launch {
            val scores = repository.getLeaderboard()
            val leaderboardText = scores.joinToString("\n") { "${it.playerName}: ${it.score}" }
            leaderboardTextView.text = leaderboardText.ifEmpty { "No scores yet!" }
        }
    }
}
