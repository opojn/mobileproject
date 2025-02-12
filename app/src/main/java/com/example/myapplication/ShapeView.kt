package com.example.myapplication

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.random.Random



class ShapeView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    interface GameEndListener {
        fun onGameEnd(finalScore: Int)
    }

    private var gameEndListener: GameEndListener? = null

    fun setGameEndListener(listener: GameEndListener) {
        gameEndListener = listener
    }

    private fun endGame() {
        gameEndListener?.onGameEnd(score)
    }

    private val paint = Paint().apply {
        style = Paint.Style.FILL
    }



    private var showShapes = false
    private var countdown = 5
    private val handler = Handler(Looper.getMainLooper())
    private var score = 0
    private var gameDuration = 10
    private var remainingTime = gameDuration
    private var gameRunning = false

    private var shapes = mutableListOf<ShapeData>()  // Use custom ShapeData class
    private var buttonBounds = FloatArray(4)

    data class ShapeData(var x: Float, var y: Float, var size: Float, var color: Int, var type: String) // Custom Shape class

    init {
        startCountdown()
    }

    private fun startCountdown() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (countdown > 0) {
                    countdown--
                    invalidate()
                    handler.postDelayed(this, 1000)
                } else {
                    showShapes = true
                    gameRunning = true
                    startGameTimer()
                    generateRandomShapes()
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
                    gameRunning = false
                    showShapes = false
                    invalidate()
                }
            }
        }, 1000)
    }

    private fun generateRandomShapes() {
        if (!gameRunning) return

        shapes.clear()
        val numberOfShapes = Random.nextInt(5, 11)

        var hasTriangle = false
        val usedPositions = mutableListOf<Pair<Float, Float>>()

        repeat(numberOfShapes) {
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

            usedPositions.add(Pair(x, y))
            val color = getRandomColor()
            var type = listOf("Circle", "Square", "Triangle").random()

            if (!hasTriangle && it == numberOfShapes - 1) {
                type = "Triangle"
                hasTriangle = true
            }
            if (type == "Triangle") hasTriangle = true

            shapes.add(ShapeData(x, y, size, color, type))
        }

        if (!hasTriangle) {
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

            shapes.add(ShapeData(x, y, size, getRandomColor(), "Triangle"))
        }

        invalidate()
    }

    private fun getRandomColor(): Int {
        return Color.rgb(Random.nextInt(256), Random.nextInt(256), Random.nextInt(256))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (!gameRunning && remainingTime == 0) {
            paint.color = Color.BLACK
            paint.textSize = 80f
            endGame()
            canvas.drawText("Final Score: $score", width / 2f - 200, height / 2f, paint)
            return
        }

        if (!showShapes) {
            paint.color = Color.BLACK
            paint.textSize = 100f
            canvas.drawText(countdown.toString(), width / 2f - 50, height / 2f, paint)
            return
        }

        paint.color = Color.BLACK
        paint.textSize = 50f
        canvas.drawText("Time: $remainingTime", 50f, 80f, paint)

        paint.color = Color.BLACK
        paint.textSize = 50f
        canvas.drawText("Score: $score", width - 200f, 80f, paint)

        paint.textSize = 70f
        canvas.drawText("Triangle", width / 2f - 100, height - 50f, paint)

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
                        score += if (shape.type == "Triangle") 1 else -1
                        generateRandomShapes()
                        break
                    }
                }
                invalidate()
            }
        }
        return true
    }
}
