package com.example.myapplication

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.random.Random
import android.os.Build

class ShapeView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    interface GameEndListener {
        fun onGameEnd(finalScore: Int)
    }

    private var gameEndListener: GameEndListener? = null

    fun setGameEndListener(listener: GameEndListener) {
        gameEndListener = listener
    }

    private val paint = Paint().apply {
        style = Paint.Style.FILL
    }

    private var showShapes = false
    private var countdown = 5
    private val handler = Handler(Looper.getMainLooper())
    private var score = 0
    private var gameDuration = 30
    private var remainingTime = gameDuration
    private var gameRunning = false
    private var finalScore: Int? = null
    private var isGameFinished = false
    private var shapes = mutableListOf<ShapeData>()
    private var targetShape = getRandomShape()

    // Media players for sound effects
    private val correctSound: MediaPlayer = MediaPlayer.create(context, R.raw.correct_sound)
    private val wrongSound: MediaPlayer = MediaPlayer.create(context, R.raw.wrong_sound)

    data class ShapeData(
        var x: Float,
        var y: Float,
        var size: Float,
        var color: Int,
        var type: String,
        var dx: Float = Random.nextFloat() * 10 - 5,
        var dy: Float = Random.nextFloat() * 10 - 5
    )

    init {
        startCountdown()
    }

    private fun getRandomShape(): String {
        return listOf("Circle", "Square", "Triangle").random()
    }

    private fun startCountdown() {
        targetShape = getRandomShape()
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (countdown > 0) {
                    countdown--
                    invalidate()
                    handler.postDelayed(this, 1000)
                } else {
                    countdown = -1
                    showShapes = true
                    gameRunning = true
                    startGameTimer()
                    generateRandomShapes()
                    moveShapes()
                    invalidate()
                }
            }
        }, 1000)
    }

    private fun startGameTimer() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (remainingTime > 0) {
                    remainingTime--
                    invalidate()
                    handler.postDelayed(this, 1000)
                } else {
                    endGame()
                }
            }
        }, 1000)
    }

    private fun endGame() {
        gameRunning = false
        showShapes = false
        isGameFinished = true
        finalScore = score
        gameEndListener?.onGameEnd(finalScore!!)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (countdown > 0) {
            paint.color = Color.BLACK
            paint.textSize = 100f
            canvas.drawText(countdown.toString(), width / 2f - 50, height / 2f, paint)
            return
        }

        if (isGameFinished) {
            paint.color = Color.BLACK
            paint.textSize = 80f
            canvas.drawText("Final Score: $finalScore", width / 2f - 200, height / 2f, paint)
            return
        }

        paint.color = Color.BLACK
        paint.textSize = 50f
        canvas.drawText("Time: $remainingTime", 50f, 80f, paint)
        canvas.drawText("Score: $score", width - 200f, 80f, paint)

        paint.textSize = 70f
        canvas.drawText("Target: $targetShape", width / 2f - 100, height - 50f, paint)

        for (shape in shapes) {
            paint.color = shape.color
            when (shape.type) {
                "Circle" -> canvas.drawCircle(shape.x, shape.y, shape.size / 2, paint)
                "Square" -> canvas.drawRect(shape.x, shape.y, shape.x + shape.size, shape.y + shape.size, paint)
                "Triangle" -> {
                    val path = Path()
                    path.moveTo(shape.x, shape.y - shape.size / 2)
                    path.lineTo(shape.x - shape.size / 2, shape.y + shape.size / 2)
                    path.lineTo(shape.x + shape.size / 2, shape.y + shape.size / 2)
                    path.close()
                    canvas.drawPath(path, paint)
                }
            }
        }
    }

    private fun playSound(mediaPlayer: MediaPlayer) {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.stop() // Stop if already playing
            mediaPlayer.prepare() // Prepare again for the next play
        }
        mediaPlayer.start()
    }


    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!showShapes || !gameRunning) return false

        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val iterator = shapes.iterator()
                while (iterator.hasNext()) {
                    val shape = iterator.next()
                    val hit = when (shape.type) {
                        "Circle" -> Math.sqrt(((x - shape.x) * (x - shape.x) + (y - shape.y) * (y - shape.y)).toDouble()) <= shape.size / 2
                        "Square" -> x in shape.x..(shape.x + shape.size) && y in shape.y..(shape.y + shape.size)
                        "Triangle" -> x in (shape.x - shape.size / 2)..(shape.x + shape.size / 2) && y in (shape.y - shape.size / 2)..(shape.y + shape.size / 2)
                        else -> false
                    }

                    if (hit) {
                        if (shape.type == targetShape) {
                            score++
                            playSound(correctSound)
                        } else {
                            score--
                            playSound(wrongSound)
                            vibrate()
                        }
                        targetShape = getRandomShape() // Change the target shape after every click
                        generateRandomShapes()
                        break
                    }
                }
                invalidate()
            }
        }
        return true
    }

    private fun vibrate() {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE)
                vibrator.vibrate(effect)
            } else {
                vibrator.vibrate(150)
            }
        }
    }

    private fun generateRandomShapes() {
        if (!gameRunning) return

        shapes.clear()
        val numberOfShapes = Random.nextInt(5, 11) // Random number of shapes
        val usedPositions = mutableListOf<Pair<Float, Float>>()
        var hasTargetShape = false

        repeat(numberOfShapes) {
            val size = Random.nextFloat() * 100f + 100f
            var x: Float
            var y: Float
            var attempts = 0

            // Ensure shapes do not overlap
            do {
                x = Random.nextFloat() * (width - size * 2) + size
                y = Random.nextFloat() * (height - size * 2) + size
                attempts++
            } while (attempts < 10 && usedPositions.any { (px, py) ->
                    Math.abs(px - x) < size && Math.abs(py - y) < size
                })

            usedPositions.add(Pair(x, y))

            // Ensure at least one target shape appears
            val type = if (!hasTargetShape && it == numberOfShapes - 1) {
                hasTargetShape = true
                targetShape
            } else {
                listOf("Circle", "Square", "Triangle").random()
            }

            // Track if the target shape is present
            if (type == targetShape) hasTargetShape = true

            shapes.add(ShapeData(x, y, size, getRandomColor(), type))
        }

        // If the target shape was not added, forcefully add it
        if (!hasTargetShape) {
            val size = Random.nextFloat() * 100f + 100f
            var x: Float
            var y: Float
            var attempts = 0

            do {
                x = Random.nextFloat() * (width - size * 2) + size
                y = Random.nextFloat() * (height - size * 2) + size
                attempts++
            } while (attempts < 10 && usedPositions.any { (px, py) ->
                    Math.abs(px - x) < size && Math.abs(py - y) < size
                })

            shapes.add(ShapeData(x, y, size, getRandomColor(), targetShape))
        }

        invalidate()
    }

    private fun getRandomColor(): Int {
        return Color.rgb(Random.nextInt(256), Random.nextInt(256), Random.nextInt(256))
    }

    private fun moveShapes() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (!gameRunning) return

                for (shape in shapes) {
                    shape.x += shape.dx
                    shape.y += shape.dy

                    if (shape.x - shape.size / 2 < 0 || shape.x + shape.size / 2 > width) {
                        shape.dx *= -1
                    }
                    if (shape.y - shape.size / 2 < 0 || shape.y + shape.size / 2 > height) {
                        shape.dy *= -1
                    }
                }
                invalidate()
                handler.postDelayed(this, 30)
            }
        }, 30)
    }

}
