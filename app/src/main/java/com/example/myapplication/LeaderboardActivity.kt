package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
//
class LeaderboardActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leaderboard)

        val backButton: Button = findViewById(R.id.button_back_leaderboard)
        backButton.setOnClickListener {
            finish() // Closes LeaderboardActivity and returns to StartMenuActivity
        }
    }
}
