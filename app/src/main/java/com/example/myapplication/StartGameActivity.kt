package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.widget.LinearLayout

class StartGameActivity : AppCompatActivity(), ShapeView.GameEndListener {
    private lateinit var shapeView: ShapeView
    private lateinit var playerName: String
    private var backgroundMusic: MediaPlayer? = null

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
    }
    private fun playGameMusic() {
        if (backgroundMusic?.isPlaying == true) return
        runOnUiThread {

            if (backgroundMusic == null) {
                backgroundMusic = MediaPlayer.create(this, R.raw.game_music)
                backgroundMusic?.isLooping = true
                backgroundMusic?.setVolume(0.2f, 0.2f)
            }
            else if (!backgroundMusic!!.isPlaying) {
                backgroundMusic!!.start()
            }
        }
    }

    override fun onGameEnd(finalScore: Int) {
        saveScore(playerName, finalScore)

        // Stop game music
        backgroundMusic?.stop()
        backgroundMusic?.release()
        backgroundMusic = null


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
