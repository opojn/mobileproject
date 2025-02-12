package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.EditText
import android.widget.TextView

class StartMenuActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start_menu)

        val nameInput: EditText = findViewById(R.id.name_input)
        val startButton: Button = findViewById(R.id.button_start)
        val leaderboardButton: Button = findViewById(R.id.button_leaderboard)
        val errorMessage: TextView = findViewById(R.id.error_message)

        startButton.setOnClickListener {
            val playerName = nameInput.text.toString().trim()

            if (playerName.isNotEmpty()) {
                val intent = Intent(this, StartGameActivity::class.java)
                intent.putExtra("PLAYER_NAME", playerName) // Pass name to game
                startActivity(intent)
            } else {
                errorMessage.text = "PLEASE KEY IN NAME TO START THE GAME"
            }
        }

        leaderboardButton.setOnClickListener {
            val intent = Intent(this, LeaderboardActivity::class.java)
            startActivity(intent)
        }
    }
}
