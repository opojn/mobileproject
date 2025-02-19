package com.example.myapplication

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.random.Random
// Box2D / JBox2D imports
import org.jbox2d.collision.shapes.CircleShape
import org.jbox2d.collision.shapes.EdgeShape
import org.jbox2d.collision.shapes.PolygonShape
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.Body
import org.jbox2d.dynamics.BodyDef
import org.jbox2d.dynamics.BodyType
import org.jbox2d.dynamics.FixtureDef
import org.jbox2d.dynamics.World

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
    private var speedMultiplier = 1.0f

    // Create a Box2D world with zero gravity.
    private val world = World(Vec2(0f, 0f))
    companion object {
        // Conversion factor: pixels per meter.
        const val PIXELS_PER_METER = 30f
    }
    private var boundariesCreated = false

    // New constant to boost the initial speed.
    private val initialSpeedFactor = 60f

    // Media players for sound effects
    private val correctSound: MediaPlayer = MediaPlayer.create(context, R.raw.correct_sound)
    private val wrongSound: MediaPlayer = MediaPlayer.create(context, R.raw.wrong_sound)

    // Each shape now stores its base speed (in world units) along with its Box2D body.
    data class ShapeData(
        var x: Float,             // Center x (in pixels)
        var y: Float,             // Center y (in pixels)
        var size: Float,          // Size (in pixels)
        var color: Int,
        var type: String,
        var dx: Float = Random.nextFloat() * 10 - 5,
        var dy: Float = Random.nextFloat() * 10 - 5,
        var baseSpeed: Float = 0f,  // Base speed in world units (set later)
        var body: Body? = null     // Reference to the Box2D body
    )

    init {
        startCountdown()
    }

    private fun getRandomShape(): String {
        return listOf("Circle", "Square", "Triangle").random()
    }

    // Create world boundaries once the view size is known.
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (!boundariesCreated) {
            createWorldBounds(w, h)
            boundariesCreated = true
        }
    }

    private fun createWorldBounds(w: Int, h: Int) {
        val bodyDef = BodyDef()
        val wallBody = world.createBody(bodyDef)

        // Top edge: from (0,0) to (w,0)
        val topEdge = EdgeShape()
        topEdge.set(Vec2(0f, 0f), Vec2(w / PIXELS_PER_METER, 0f))
        wallBody.createFixture(topEdge, 0f)

        // Bottom edge: from (0,h) to (w,h)
        val bottomEdge = EdgeShape()
        bottomEdge.set(Vec2(0f, h / PIXELS_PER_METER), Vec2(w / PIXELS_PER_METER, h / PIXELS_PER_METER))
        wallBody.createFixture(bottomEdge, 0f)

        // Left edge: from (0,0) to (0,h)
        val leftEdge = EdgeShape()
        leftEdge.set(Vec2(0f, 0f), Vec2(0f, h / PIXELS_PER_METER))
        wallBody.createFixture(leftEdge, 0f)

        // Right edge: from (w,0) to (w,h)
        val rightEdge = EdgeShape()
        rightEdge.set(Vec2(w / PIXELS_PER_METER, 0f), Vec2(w / PIXELS_PER_METER, h / PIXELS_PER_METER))
        wallBody.createFixture(rightEdge, 0f)
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
                    // Increase the speed multiplier every 5 seconds.
                    if (remainingTime > 0 && remainingTime % 5 == 0) {
                        speedMultiplier += 1.0f
                    }
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
            val finalText = "Final Score: $finalScore"
            val finalWidth = paint.measureText(finalText)
            canvas.drawText(finalText, (width - finalWidth) / 2, height / 2f, paint)
            return
        }

        paint.color = Color.BLACK
        paint.textSize = 50f
        canvas.drawText("Time: $remainingTime", 50f, 80f, paint)
        canvas.drawText("Score: $score", width - 200f, 80f, paint)

        paint.textSize = 70f
        val targetText = "Target: $targetShape"
        val textWidth = paint.measureText(targetText)
        canvas.drawText(targetText, (width - textWidth) / 2, height - 50f, paint)

        // Draw each shape using its center (x,y)
        for (shape in shapes) {
            paint.color = shape.color
            when (shape.type) {
                "Circle" -> canvas.drawCircle(shape.x, shape.y, shape.size / 2, paint)
                "Square" -> canvas.drawRect(
                    shape.x - shape.size / 2,
                    shape.y - shape.size / 2,
                    shape.x + shape.size / 2,
                    shape.y + shape.size / 2,
                    paint
                )
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
            mediaPlayer.stop()
            mediaPlayer.prepare()
        }
        mediaPlayer.start()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!showShapes || !gameRunning) return false

        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                for (shape in shapes) {
                    // Center-based hit detection.
                    val hit = when (shape.type) {
                        "Circle" -> Math.hypot((x - shape.x).toDouble(), (y - shape.y).toDouble()) <= shape.size / 2
                        "Square" -> x in (shape.x - shape.size / 2)..(shape.x + shape.size / 2) &&
                                y in (shape.y - shape.size / 2)..(shape.y + shape.size / 2)
                        "Triangle" -> x in (shape.x - shape.size / 2)..(shape.x + shape.size / 2) &&
                                y in (shape.y - shape.size / 2)..(shape.y + shape.size / 2)
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
                        targetShape = getRandomShape() // Change target after a hit
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

    // Generate new shapes and create their Box2D bodies.
    private fun generateRandomShapes() {
        // Remove any existing shapes.
        for (shape in shapes) {
            shape.body?.let { world.destroyBody(it) }
        }
        shapes.clear()
        val numberOfShapes = Random.nextInt(5, 11)
        val usedPositions = mutableListOf<Pair<Float, Float>>()
        var hasTargetShape = false

        repeat(numberOfShapes) { i ->
            val size = Random.nextFloat() * 100f + 100f
            var x: Float
            var y: Float
            var attempts = 0

            do {
                x = Random.nextFloat() * (width - size) + size / 2
                y = Random.nextFloat() * (height - size) + size / 2
                attempts++
            } while (attempts < 10 && usedPositions.any { (px, py) ->
                    Math.abs(px - x) < size && Math.abs(py - y) < size
                })
            usedPositions.add(Pair(x, y))
            val type = if (!hasTargetShape && i == numberOfShapes - 1) {
                hasTargetShape = true
                targetShape
            } else {
                listOf("Circle", "Square", "Triangle").random()
            }
            if (type == targetShape) hasTargetShape = true

            val shapeData = ShapeData(x, y, size, getRandomColor(), type)
            shapeData.body = createBodyForShape(shapeData)
            shapes.add(shapeData)
        }

        // Force-add a target shape if not present.
        if (!hasTargetShape) {
            val size = Random.nextFloat() * 100f + 100f
            var x: Float
            var y: Float
            var attempts = 0
            do {
                x = Random.nextFloat() * (width - size) + size / 2
                y = Random.nextFloat() * (height - size) + size / 2
                attempts++
            } while (attempts < 10 && usedPositions.any { (px, py) ->
                    Math.abs(px - x) < size && Math.abs(py - y) < size
                })
            val shapeData = ShapeData(x, y, size, getRandomColor(), targetShape)
            shapeData.body = createBodyForShape(shapeData)
            shapes.add(shapeData)
        }

        invalidate()
    }

    // Create a Box2D body for the given shape.
    private fun createBodyForShape(shape: ShapeData): Body {
        val bodyDef = BodyDef().apply {
            type = BodyType.DYNAMIC
            // Set the position using world units.
            position.set(shape.x / PIXELS_PER_METER, shape.y / PIXELS_PER_METER)
            fixedRotation = true
        }
        val body = world.createBody(bodyDef)

        // Set initial velocity using the initialSpeedFactor to boost speed.
        val initVel = Vec2(
            (shape.dx / PIXELS_PER_METER) * initialSpeedFactor,
            (shape.dy / PIXELS_PER_METER) * initialSpeedFactor
        )
        body.linearVelocity = initVel

        // Store the base speed (magnitude in world units).
        shape.baseSpeed = initVel.length()

        val fixtureDef = FixtureDef().apply {
            density = 1f
            friction = 0f
            restitution = 1f
        }

        when (shape.type) {
            "Circle" -> {
                val circle = CircleShape()
                circle.m_radius = (shape.size / 2) / PIXELS_PER_METER
                fixtureDef.shape = circle
            }
            "Square" -> {
                val polygon = PolygonShape()
                val halfSize = (shape.size / 2) / PIXELS_PER_METER
                polygon.setAsBox(halfSize, halfSize)
                fixtureDef.shape = polygon
            }
            "Triangle" -> {
                val polygon = PolygonShape()
                val halfSize = (shape.size / 2) / PIXELS_PER_METER
                // Define a triangle with top vertex at (0,-halfSize) and base from (-halfSize,halfSize) to (halfSize,halfSize).
                val vertices = arrayOf(
                    Vec2(0f, -halfSize),
                    Vec2(-halfSize, halfSize),
                    Vec2(halfSize, halfSize)
                )
                polygon.set(vertices, vertices.size)
                fixtureDef.shape = polygon
            }
        }
        body.createFixture(fixtureDef)
        return body
    }

    private fun getRandomColor(): Int {
        return Color.rgb(Random.nextInt(256), Random.nextInt(256), Random.nextInt(256))
    }

    // Step the Box2D simulation and update shape positions.
    private fun moveShapes() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (!gameRunning) return

                val timeStep = 1.0f / 60f
                val velocityIterations = 8
                val positionIterations = 3
                world.step(timeStep, velocityIterations, positionIterations)

                // Update velocity based on speed multiplier and then update positions.
                for (shape in shapes) {
                    shape.body?.let { body ->
                        val currentVel = body.linearVelocity
                        if (currentVel.length() > 0f) {
                            val newVel = currentVel.clone()
                            newVel.normalize()
                            newVel.mulLocal(shape.baseSpeed * speedMultiplier)
                            body.linearVelocity = newVel
                        }
                        val pos = body.position
                        shape.x = pos.x * PIXELS_PER_METER
                        shape.y = pos.y * PIXELS_PER_METER
                    }
                }
                invalidate()
                handler.postDelayed(this, 16) // Roughly 60 FPS.
            }
        }, 16)
    }
}
