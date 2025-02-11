package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button

class StartMenuActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start_menu)

        val startButton: Button = findViewById(R.id.button_start)
        val leaderboardButton: Button = findViewById(R.id.button_leaderboard)

        startButton.setOnClickListener {
            val intent = Intent(this, StartGameActivity::class.java)
            startActivity(intent)
        }

        leaderboardButton.setOnClickListener {
            val intent = Intent(this, LeaderboardActivity::class.java)
            startActivity(intent)
        }
    }
}
