package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.EditText
import android.widget.TextView

//class StartMenuActivity : AppCompatActivity() {
//    private var backgroundMusic: MediaPlayer? = null
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_start_menu)
//
//        val nameInput: EditText = findViewById(R.id.name_input)
//        val startButton: Button = findViewById(R.id.button_start)
//        val leaderboardButton: Button = findViewById(R.id.button_leaderboard)
//        val errorMessage: TextView = findViewById(R.id.error_message)
//
//        // Start background music
//        playMenuMusic()
//
//        startButton.setOnClickListener {
//            val playerName = nameInput.text.toString().trim()
//
//            if (playerName.isNotEmpty()) {
//                val intent = Intent(this, StartGameActivity::class.java)
//                intent.putExtra("PLAYER_NAME", playerName) // Pass name to game
//                startActivity(intent)
//                stopMenuMusic()
//            } else {
//                errorMessage.text = "PLEASE KEY IN NAME TO START THE GAME"
//            }
//        }
//
//        leaderboardButton.setOnClickListener {
//            val intent = Intent(this, LeaderboardActivity::class.java)
//            startActivity(intent)
//        }
//    }
//
//    private fun playMenuMusic() {
//        val sharedPreferences = getSharedPreferences("GameSettings", Context.MODE_PRIVATE)
//        val bgmVolume = sharedPreferences.getFloat("BGM_VOLUME", 0.5f)
//        if (backgroundMusic?.isPlaying == true) return
//
//        if (backgroundMusic == null) {
//            backgroundMusic = MediaPlayer.create(this, R.raw.menu_music)
//            backgroundMusic?.isLooping = true
//            backgroundMusic?.setVolume(0.5f, 0.5f) // Set BGM to 50% volume
//            backgroundMusic?.start()
//        }
//    }
//
//
//    private fun stopMenuMusic() {
//        backgroundMusic?.stop()
//        backgroundMusic?.release()
//        backgroundMusic = null
//    }
//
//    override fun onResume() {
//        super.onResume()
//        playMenuMusic() // Restart music when returning to menu
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        stopMenuMusic()
//    }
////    private fun playSound(mediaPlayer: MediaPlayer) {
////        if (mediaPlayer.isPlaying) {
////            mediaPlayer.stop()
////            mediaPlayer.prepare()
////        }
////        mediaPlayer.start()
////    }
//
//}
class StartMenuActivity : AppCompatActivity() {
    private var backgroundMusic: MediaPlayer? = null

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
                onDestroy()
            } else {
                errorMessage.text = "PLEASE KEY IN NAME TO START THE GAME"
            }
        }

        leaderboardButton.setOnClickListener {
            val intent = Intent(this, LeaderboardActivity::class.java)
            startActivity(intent)
        }
        playMenuMusic()
    }

    private fun playMenuMusic() {
        if (backgroundMusic?.isPlaying == true) return

        runOnUiThread {
            if (backgroundMusic == null) {
                backgroundMusic = MediaPlayer.create(this, R.raw.menu_music)
                backgroundMusic?.isLooping = true
                backgroundMusic?.setVolume(0.5f, 0.5f)
            }
            backgroundMusic?.start()
        }
    }

    override fun onPause() {
        super.onPause()
        backgroundMusic?.pause()
    }

    override fun onResume() {
        super.onResume()
        backgroundMusic?.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        backgroundMusic?.release()
        backgroundMusic = null
    }
}