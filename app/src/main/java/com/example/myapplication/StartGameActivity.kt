package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.widget.LinearLayout
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch



class StartGameActivity : AppCompatActivity(), ShapeView.GameEndListener {
    private lateinit var shapeView: ShapeView
    private lateinit var playerName: String
    private var backgroundMusic: MediaPlayer? = null
    private lateinit var repository: LeaderboardRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        playerName = intent.getStringExtra("PLAYER_NAME") ?: "Unknown"
        shapeView = ShapeView(this, null)
        shapeView.setGameEndListener(this)
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.addView(shapeView)

        setContentView(layout)
        playGameMusic()

        // Initialize Database and Repository
        val database = LeaderboardDatabase.getDatabase(applicationContext)
        repository = LeaderboardRepository(database.leaderboardDao())
    }

    private fun playGameMusic() {
        if (backgroundMusic?.isPlaying == true) return
        runOnUiThread {
            if (backgroundMusic == null) {
                backgroundMusic = MediaPlayer.create(this, R.raw.game_music)
                backgroundMusic?.isLooping = true
                backgroundMusic?.setVolume(0.2f, 0.2f)
            } else if (!backgroundMusic!!.isPlaying) {
                backgroundMusic!!.start()
            }
        }
    }

    override fun onGameEnd(finalScore: Int) {
        // Save Score Using Room Database
        saveScore(playerName, finalScore)

        // Stop game music
        backgroundMusic?.stop()
        backgroundMusic?.release()
        backgroundMusic = null

        // Pass the player's name and score to LeaderboardActivity
        val intent = Intent(this, LeaderboardActivity::class.java).apply {
            putExtra("PLAYER_NAME", playerName)
            putExtra("PLAYER_SCORE", finalScore)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)

        }
        startActivity(intent)
        finish()
    }

    private fun saveScore(name: String, score: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                println("DEBUG: Saving score to Room -> $name: $score")
                repository.insertScore(name, score)

                val scores = repository.getLeaderboard()
                println("DEBUG: Scores in Room after Insert -> ${scores.joinToString { "${it.playerName}: ${it.score}" }}")
            } catch (e: Exception) {
                e.printStackTrace()
                println("ERROR: Failed to save score!")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        backgroundMusic?.release()
        backgroundMusic = null
    }

    override fun onResume() {
        super.onResume()
        playGameMusic()
    }
}
