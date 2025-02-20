package com.example.myapplication

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import kotlin.math.hypot
import kotlin.math.max
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

    // Game state variables
    private var countdown = 3
    private val handler = Handler(Looper.getMainLooper())
    private var score = 0
    private var lives = 3
    private var combo = 0
    private var gameDuration = 30 // seconds
    private var remainingTime = gameDuration
    private var gameRunning = false
    private var isGameFinished = false

    // Difficulty variables
    private var difficultyLevel = 1
    private var spawnInterval = 3000L // base spawn interval in milliseconds
    private val spawnHandler = Handler(Looper.getMainLooper())
    private val minSpawnInterval = 3000L

    // New: Track last click time (in milliseconds)
    private var lastClickTime = System.currentTimeMillis()
    // If no click in the past 3000ms, we'll extend the spawn interval by 2000ms.

    // Box2D variables
    private val world = World(Vec2(0f, 0f))
    companion object {
        const val PIXELS_PER_METER = 20f
    }
    private var boundariesCreated = false
    private val initialSpeedFactor = 60f
    private var speedMultiplier = 1.0f

    // SoundPool for in-game sound effects
    private val soundPool: SoundPool = SoundPool.Builder().setMaxStreams(5).build()
    private var popSoundId: Int = 0
    private var wrongSoundId: Int = 0

    // List of shapes and target shape text
    private var shapes = mutableListOf<ShapeData>()
    private var targetShape = getRandomShape() // e.g., "Circle", "Square", or "Triangle"

    // Data class for shapes
    data class ShapeData(
        var x: Float,
        var y: Float,
        var size: Float,
        var color: Int,
        var type: String,
        var dx: Float = Random.nextFloat() * 10 - 5,
        var dy: Float = Random.nextFloat() * 10 - 5,
        var baseSpeed: Float = 0f,
        var body: Body? = null,
        var popped: Boolean = false // for pop animation effect
    )

    init {
        popSoundId = soundPool.load(context, R.raw.correct_sound, 1)
        wrongSoundId = soundPool.load(context, R.raw.wrong_sound, 1)
        startCountdown()
    }
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        soundPool.release() // release SoundPool when view is detached
    }

    private fun getRandomShape(): String {
        return listOf("Circle", "Square", "Triangle").random()
    }

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

        val topEdge = EdgeShape().apply {
            set(Vec2(0f, 0f), Vec2(w / PIXELS_PER_METER, 0f))
        }
        wallBody.createFixture(topEdge, 0f)

        val bottomEdge = EdgeShape().apply {
            set(Vec2(0f, h / PIXELS_PER_METER), Vec2(w / PIXELS_PER_METER, h / PIXELS_PER_METER))
        }
        wallBody.createFixture(bottomEdge, 0f)

        val leftEdge = EdgeShape().apply {
            set(Vec2(0f, 0f), Vec2(0f, h / PIXELS_PER_METER))
        }
        wallBody.createFixture(leftEdge, 0f)

        val rightEdge = EdgeShape().apply {
            set(Vec2(w / PIXELS_PER_METER, 0f), Vec2(w / PIXELS_PER_METER, h / PIXELS_PER_METER))
        }
        wallBody.createFixture(rightEdge, 0f)
    }

    // Countdown before the game starts
    private fun startCountdown() {
        // Generate shapes before countdown finishes
        generateRandomShapes()

        handler.post(object : Runnable {
            override fun run() {
                if (countdown > 0) {
                    invalidate()
                    handler.postDelayed(this, 1000)
                } else {
                    countdown = -1
                    gameRunning = true
                    startSpawnRoutine()
                    startGameTimer()
                    moveShapes()
                    invalidate() // Force redraw with pre-generated shapes
                }
                countdown--
            }
        })
    }


    // Spawn routine to generate shapes periodically.
    // If no click has occurred in the last 3000ms, extend the interval.
    private fun startSpawnRoutine() {
        spawnHandler.postDelayed(spawnRunnable, spawnInterval)
    }

    private val spawnRunnable = object : Runnable {
        override fun run() {
            generateRandomShapes()
            difficultyLevel++
            // Decrease spawn interval with difficulty, but adjust if no click recently.
            val currentTime = System.currentTimeMillis()
            val timeSinceLastClick = currentTime - lastClickTime
            // If no click in the last 3000ms, add an extra 2000ms delay.
            val effectiveSpawnInterval = if (timeSinceLastClick > 3000L) spawnInterval + 2000L else spawnInterval
            // Ensure spawnInterval does not fall below minSpawnInterval.
            spawnInterval = max(minSpawnInterval, spawnInterval - 200)
            spawnHandler.postDelayed(this, effectiveSpawnInterval)
        }
    }

    private fun startGameTimer() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (remainingTime > 0 && gameRunning) {
                    remainingTime--
                    invalidate()
                    handler.postDelayed(this, 1000)
                } else {
                    endGame()
                }
            }
        }, 1000)
    }

    // End game delays the transition by 3 seconds to let the final score remain visible.
    private fun endGame() {
        gameRunning = false
        isGameFinished = true
        spawnHandler.removeCallbacks(spawnRunnable)
        handler.postDelayed({
            gameEndListener?.onGameEnd(score)
        }, 3000)
        invalidate()
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw countdown
        if (countdown > 0) {
            paint.color = Color.BLACK
            paint.textSize = 100f
            canvas.drawText(countdown.toString(), width / 2f - 50, height / 2f, paint)
            return
        }

        // Draw game over screen
        if (isGameFinished) {
            paint.color = Color.BLACK
            paint.textSize = 80f
            val finalText = "Game Over! Score: $score"
            val finalWidth = paint.measureText(finalText)
            canvas.drawText(finalText, (width - finalWidth) / 2, height / 2f, paint)
            return
        }

        // Draw HUD: Time, Score, Lives, Combo
        paint.color = Color.BLACK
        paint.textSize = 50f
        canvas.drawText("Time: $remainingTime", 50f, 80f, paint)
        canvas.drawText("Score: $score", width - 250f, 80f, paint)
        canvas.drawText("Lives: $lives", 50f, 140f, paint)
        canvas.drawText("Combo: $combo", width - 250f, 140f, paint)

        // Display target shape text at the bottom
        paint.textSize = 50f
        val targetText = "Target: $targetShape"
        val textWidth = paint.measureText(targetText)
        canvas.drawText(targetText, (width - textWidth) / 2, height - 80f, paint)

        // Draw shapes
        for (shape in shapes) {
            if (shape.popped) {
                paint.color = Color.YELLOW
                canvas.drawCircle(shape.x, shape.y, shape.size, paint)
            } else {
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
    }

//    override fun onTouchEvent(event: MotionEvent): Boolean {
//        if (!gameRunning) return false
//        val x = event.x
//        val y = event.y
//        when (event.action) {
//            MotionEvent.ACTION_DOWN -> {
//                lastClickTime = System.currentTimeMillis() // Update last click time
//                var hitDetected = false
//                for (shape in shapes) {
//                    val hit = when (shape.type) {
//                        "Circle" -> hypot((x - shape.x).toDouble(), (y - shape.y).toDouble()) <= shape.size / 2
//                        "Square" -> x in (shape.x - shape.size / 2)..(shape.x + shape.size / 2) &&
//                                y in (shape.y - shape.size / 2)..(shape.y + shape.size / 2)
//                        "Triangle" -> x in (shape.x - shape.size / 2)..(shape.x + shape.size / 2) &&
//                                y in (shape.y - shape.size / 2)..(shape.y + shape.size / 2)
//                        else -> false
//                    }
//                    if (hit) {
//                        hitDetected = true
//                        if (shape.type == targetShape) {
//                            if (lives < 3) lives++
//                            score += 1 + combo
//                            combo++
//                            remainingTime++
//                            soundPool.play(popSoundId, 1f, 1f, 1, 0, 1f)
//                            shape.popped = true
//                            handler.postDelayed({ shapes.remove(shape) }, 100)
//                        } else {
//                            combo = 0
//                            lives--
//                            soundPool.play(wrongSoundId, 1f, 1f, 1, 0, 1f)
//                            vibrate()
//                            if (lives <= 0) { endGame() }
//                        }
//                        targetShape = getRandomShape()
//                        generateRandomShapes()
//                        break
//                    }
//                }
//                invalidate()
//            }
//        }
//        return true
//    }
override fun onTouchEvent(event: MotionEvent): Boolean {
    if (!gameRunning || event.action != MotionEvent.ACTION_DOWN) return false

    lastClickTime = System.currentTimeMillis()
    val x = event.x
    val y = event.y

    val hitShape = shapes.firstOrNull { shape ->
        when (shape.type) {
            "Circle" -> hypot((x - shape.x).toDouble(), (y - shape.y).toDouble()) <= shape.size / 2
            "Square", "Triangle" -> x in (shape.x - shape.size / 2)..(shape.x + shape.size / 2) &&
                    y in (shape.y - shape.size / 2)..(shape.y + shape.size / 2)
            else -> false
        }
    }

    hitShape?.let { shape ->
        if (shape.type == targetShape) {
            score += 1 + combo
            combo++
            if (lives < 3) lives++
            soundPool.play(popSoundId, 1f, 1f, 1, 0, 1f)
        } else {
            combo = 0
            lives--
            soundPool.play(wrongSoundId, 1f, 1f, 1, 0, 1f)
            vibrate()
            if (lives <= 0) endGame()
        }
        shapes.remove(shape)
        targetShape = getRandomShape()
        // 🚀 Check if any shape type is missing, regenerate if needed
        if (!isValidShapeDistribution()) {
            generateRandomShapes()
        }
    }

    invalidate()
    return true
}
    private fun isValidShapeDistribution(): Boolean {
        val shapeTypes = shapes.map { it.type }.toSet()
        return shapeTypes.contains("Circle") && shapeTypes.contains("Square") && shapeTypes.contains("Triangle")
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
    private fun playSound(mediaPlayer: MediaPlayer) {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.stop()
            mediaPlayer.prepare()
        }
        mediaPlayer.start()
    }
    // Generate new shapes and ensure at least one of each type is present.
//    private fun generateRandomShapes() {
//        // Clear current shapes and destroy Box2D bodies.
//        for (shape in shapes) {
//            shape.body?.let { world.destroyBody(it) }
//        }
//        shapes.clear()
//
//        val numberOfShapes = Random.nextInt(3, 6 + difficultyLevel)
//        val usedPositions = mutableListOf<Pair<Float, Float>>()
//
//        repeat(numberOfShapes) {
//            // Increase base size for bigger shapes.
//            val baseSize = max(50f, 250f - difficultyLevel * 5)
//            val size = Random.nextFloat() * 30f + baseSize
//            var x: Float
//            var y: Float
//            var attempts = 0
//            do {
//                x = Random.nextFloat() * (width - size) + size / 2
//                y = Random.nextFloat() * (height - size) + size / 2
//                attempts++
//            } while (attempts < 10 && usedPositions.any { (px, py) ->
//                    Math.abs(px - x) < size && Math.abs(py - y) < size
//                })
//            usedPositions.add(Pair(x, y))
//            val type = listOf("Circle", "Square", "Triangle").random()
//            val shapeData = ShapeData(x, y, size, getRandomColor(), type)
//            shapeData.body = createBodyForShape(shapeData)
//            shapes.add(shapeData)
//        }
//        // Ensure at least one of each shape is present.
//        val requiredTypes = listOf("Circle", "Square", "Triangle")
//        for (reqType in requiredTypes) {
//            if (shapes.none { it.type == reqType }) {
//                // Create a shape of the missing type.
//                val baseSize = max(70f, 250f - difficultyLevel * 5)
//                val size = baseSize + Random.nextFloat() * 30f
//                val x = Random.nextFloat() * (width - size) + size / 2
//                val y = Random.nextFloat() * (height - size) + size / 2
//                val shapeData = ShapeData(x, y, size, getRandomColor(), reqType)
//                shapeData.body = createBodyForShape(shapeData)
//                shapes.add(shapeData)
//            }
//        }
//        invalidate()
//    }
    private fun generateRandomShapes() {
        shapes.forEach { it.body?.let(world::destroyBody) }
        shapes.clear()

        val shapeTypes = listOf("Circle", "Square", "Triangle")
        val numberOfShapes = Random.nextInt(3, 6 + difficultyLevel)
        val usedPositions = mutableSetOf<Pair<Float, Float>>()

        fun getUniquePosition(size: Float): Pair<Float, Float> {
            var pos: Pair<Float, Float>
            var attempts = 0
            do {
                pos = Random.nextFloat() * (width - size) + size / 2 to
                        Random.nextFloat() * (height - size) + size / 2
                attempts++
            } while (attempts < 10 && usedPositions.any { (px, py) ->
                    Math.abs(px - pos.first) < size && Math.abs(py - pos.second) < size
                })
            usedPositions.add(pos)
            return pos
        }

        repeat(numberOfShapes) {
            val type = shapeTypes.random()
            val baseSize = max(70f, 250f - difficultyLevel * 5) // Increase minimum size from 50f to 70f
            val size = Random.nextFloat() * 30f + baseSize
            val (x, y) = getUniquePosition(size)
            shapes.add(ShapeData(x, y, size, getRandomColor(), type).apply {
                body = createBodyForShape(this)
            })
        }

        // Ensure at least one of each shape is present.
        shapeTypes.forEach { type ->
            if (shapes.none { it.type == type }) {
                val (x, y) = getUniquePosition(70f)
                shapes.add(ShapeData(x, y, 70f, getRandomColor(), type).apply {
                    body = createBodyForShape(this)
                })
            }
        }

        invalidate()
    }


    private fun createBodyForShape(shape: ShapeData): Body {
        val bodyDef = BodyDef().apply {
            type = BodyType.DYNAMIC
            position.set(shape.x / PIXELS_PER_METER, shape.y / PIXELS_PER_METER)
            fixedRotation = true
        }
        val body = world.createBody(bodyDef)
        val initVel = Vec2(
            (shape.dx / PIXELS_PER_METER) * initialSpeedFactor,
            (shape.dy / PIXELS_PER_METER) * initialSpeedFactor
        )
        body.linearVelocity = initVel
        shape.baseSpeed = initVel.length()
        val fixtureDef = FixtureDef().apply {
            density = 1f
            friction = 0f
            restitution = 1f
        }
        when (shape.type) {
            "Circle" -> {
                val circle = CircleShape().apply {
                    m_radius = (shape.size / 2) / PIXELS_PER_METER
                }
                fixtureDef.shape = circle
            }
            "Square" -> {
                val polygon = PolygonShape().apply {
                    val halfSize = (shape.size / 2) / PIXELS_PER_METER
                    setAsBox(halfSize, halfSize)
                }
                fixtureDef.shape = polygon
            }
            "Triangle" -> {
                val polygon = PolygonShape().apply {
                    val halfSize = (shape.size / 2) / PIXELS_PER_METER
                    val vertices = arrayOf(
                        Vec2(0f, -halfSize),
                        Vec2(-halfSize, halfSize),
                        Vec2(halfSize, halfSize)
                    )
                    set(vertices, vertices.size)
                }
                fixtureDef.shape = polygon
            }
        }
        body.createFixture(fixtureDef)
        return body
    }

    private fun getRandomColor(): Int {
        return Color.rgb(Random.nextInt(256), Random.nextInt(256), Random.nextInt(256))
    }

    private fun moveShapes() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (!gameRunning) return
                val timeStep = 1.0f / 30f
                val velocityIterations = 6
                val positionIterations = 2
                world.step(timeStep, velocityIterations, positionIterations)
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
                handler.postDelayed(this, 33)
            }
        }, 33)
    }
}
