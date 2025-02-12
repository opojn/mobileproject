package com.example.myapplication

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.TextView

class LeaderboardActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leaderboard)

        val leaderboardTextView: TextView = findViewById(R.id.leaderboard_text)
        val backButton: Button = findViewById(R.id.button_back)

        backButton.setOnClickListener {
            finish() // Go back to main menu
        }

        displayLeaderboard(leaderboardTextView)
    }

    private fun displayLeaderboard(leaderboardTextView: TextView) {
        val sharedPreferences = getSharedPreferences("Leaderboard", Context.MODE_PRIVATE)
        val scores = sharedPreferences.getStringSet("scores", setOf()) ?: setOf()

        // Convert scores to a list and sort them from highest to lowest
        val sortedScores = scores.mapNotNull { entry ->
            val parts = entry.split(":")
            if (parts.size == 2) {
                parts[0] to parts[1].toIntOrNull()
            } else {
                null
            }
        }.sortedByDescending { it.second }

        val leaderboardText = sortedScores.joinToString("\n") { "${it.first}: ${it.second}" }
        leaderboardTextView.text = leaderboardText.ifEmpty { "No scores yet!" }
    }
}
