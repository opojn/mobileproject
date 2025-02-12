package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.LinearLayout

class StartGameActivity : AppCompatActivity(), ShapeView.GameEndListener {
    private lateinit var shapeView: ShapeView
    private lateinit var playerName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        playerName = intent.getStringExtra("PLAYER_NAME") ?: "Unknown"

        // Set up ShapeView
        shapeView = ShapeView(this, null)
        shapeView.setGameEndListener(this)

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.addView(shapeView)

        setContentView(layout)
    }

    override fun onGameEnd(finalScore: Int) {
        saveScore(playerName, finalScore)

        val intent = Intent(this, LeaderboardActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun saveScore(name: String, score: Int) {
        val sharedPreferences = getSharedPreferences("Leaderboard", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        val currentScores = sharedPreferences.getStringSet("scores", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        currentScores.add("$name:$score")

        editor.putStringSet("scores", currentScores)
        editor.apply()
    }
}
