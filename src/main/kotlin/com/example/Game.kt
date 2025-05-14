package com.example

import javafx.animation.AnimationTimer
import javafx.application.Application
import javafx.event.EventHandler
import javafx.scene.Group
import javafx.scene.Scene
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import javafx.scene.input.KeyCode
import javafx.scene.paint.Color
import javafx.stage.Stage

class Game : Application() {

    companion object {
        private const val WIDTH = 312 // szélesség
        private const val HEIGHT = 512 // magasság
        const val ROWS = 20 // sorok száma
        const val COLS = 13 // oszlopok száma
        private const val CELL_SIZE = 24 // cellák mérete
        private const val OFFSET_X = 0.0 // eltolás szélességben
        private const val OFFSET_Y = 32.0 // eltolás magasságban
        private const val INITIAL_DROP_INTERVAL_MS = 500L // alap esési tempó
        private const val MIN_DROP_INTERVAL_MS = 50L  // Minimális esési tempó
        private const val COOLDOWN_NS = 100_000_000L
    }

    private lateinit var scene: Scene
    private lateinit var graphicsContext: GraphicsContext
    private lateinit var background: Image

    private val board = Array(ROWS) { IntArray(COLS) { -1 } } // -1 jelenti, hogy nincs benne Tetromino
    private var tetromino = Tetromino()
    private var isGameOver = false

    private var lastFrameTime = System.nanoTime()
    private var dropAccumulator = 0L
    private var dropInterval = INITIAL_DROP_INTERVAL_MS // Esési sebesség

    private val activeKeys = mutableSetOf<KeyCode>()
    private var lastHMoveTime = System.nanoTime()

    private var elapsedGameTimeMs = 0L

    override fun start(stage: Stage) {
        stage.title = "Tetris"
        val root = Group()
        scene = Scene(root)
        stage.scene = scene

        val canvas = Canvas(WIDTH.toDouble(), HEIGHT.toDouble())
        canvas.isFocusTraversable = true
        root.children.add(canvas)
        graphicsContext = canvas.graphicsContext2D

        loadResources()

        // Esemény kezelő
        canvas.onKeyPressed = EventHandler { e ->
            activeKeys.add(e.code)
            if (e.code == KeyCode.R && isGameOver) {
                resetGame() // új játék
            }
        }
        canvas.onKeyReleased = EventHandler { e -> activeKeys.remove(e.code) }

        startGameLoop()

        stage.show()
        canvas.requestFocus()
    }


    private fun loadResources() {
        background = Image(javaClass.getResource("/space.png")!!.toExternalForm())
    }

    private fun startGameLoop() {
        // Main loop
        object : AnimationTimer() {
            override fun handle(currentNanoTime: Long) {
                // the time elapsed since the last frame, in nanoseconds
                // can be used for physics calculation, etc
                val elapsedNanos = currentNanoTime - lastFrameTime
                lastFrameTime = currentNanoTime
                // display crude fps counter
                val elapsedMs = elapsedNanos / 1_000_000

                if (!isGameOver) elapsedGameTimeMs += elapsedMs // Idő hozzáadása

                val speedFactor = 1 + elapsedGameTimeMs / 60_000.0
                dropInterval = (INITIAL_DROP_INTERVAL_MS / speedFactor).toLong() // Esési sebesség
                    .coerceAtLeast(MIN_DROP_INTERVAL_MS) // Nem lehet kisebb ennél az esési sebességnél

                update(currentNanoTime, elapsedNanos, elapsedMs)
                render()
            }
        }.start()
    }

    private fun update(now: Long, elapsedNanos: Long, elapsedMs: Long) {
        if (isGameOver) return // Ha vége a játéknak nem frissitünk semmit.

        if (now - lastHMoveTime > COOLDOWN_NS) // Azért kell, mert késleltetni tudjuk így a mozgást.
        {
            when {
                KeyCode.LEFT in activeKeys && tetromino.canMove(-1, 0, board) -> {
                    tetromino.move(-1, 0)
                    lastHMoveTime = now
                }
                KeyCode.RIGHT in activeKeys && tetromino.canMove(1, 0, board) -> {
                    tetromino.move(1, 0)
                    lastHMoveTime = now
                }
                KeyCode.UP in activeKeys  -> {
                    if (tetromino.rotate(board)) lastHMoveTime = now
                }
            }
        }

        if (KeyCode.DOWN in activeKeys && tetromino.canMove(0, 1, board)) {
            tetromino.move(0, 1)
        }

        dropAccumulator += elapsedMs
        if (dropAccumulator >= dropInterval) { // ha elég idő telt le akkor esik lejebb
            dropAccumulator = 0

            if (tetromino.canMove(0, 1, board)) {
                tetromino.move(0, 1) // ha tud lejebb mozgatjuk
            } else {
                tetromino.placeOnBoard(board) // ha nem elhelyezzük a pályán
                clearFullLines() // töröljük ha van teli sor
                tetromino = Tetromino() // új tetrominot hívunk
                if (!tetromino.canMove(0, 0, board)) {
                    isGameOver = true  // ha az nem tud mozogni akkor gameover
                }
            }
        }


    }

    private fun clearFullLines() { // ha tele van akkor töröljük a sort
        val newRows = board.filter { row -> row.any { it == -1 } }.toMutableList() // a nem teljes sorok mutablelistázva
        while (newRows.size < ROWS) {
            newRows.add(0, IntArray(COLS) { -1 }) // hozzáad ameddig nincs elég sor
        }
        for (i in 0 until  board.size) {
            board[i] = newRows[i] // visszaadja a sorokat egyesével
        }
    }

    private fun render() {
        graphicsContext.clearRect(0.0, 0.0, WIDTH.toDouble(), HEIGHT.toDouble())
        graphicsContext.drawImage(background, 0.0, 0.0) // Háttérkép

        drawBoard()
        drawTetromino(tetromino.typeIndex)
        tetromino.getAbsoluteBlocks().forEach { b ->
            graphicsContext.fillRect(
                OFFSET_X + b.x * CELL_SIZE,
                OFFSET_Y + b.y * CELL_SIZE,
                CELL_SIZE.toDouble(), CELL_SIZE.toDouble()
            )
        }
        val totalSeconds = elapsedGameTimeMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        graphicsContext.fill = Color.WHITE
        graphicsContext.fillText(
            String.format("Time %02d:%02d", minutes, seconds),
            WIDTH - 80.0,
            OFFSET_Y - 8
        )

        if (isGameOver) {
            val message = "Game Over"
            val timeMessage = String.format("Time: %02d:%02d", minutes, seconds)
            val boxWidth = 120.0
            val boxHeight = 70.0
            val boxX = WIDTH / 2.0 - boxWidth / 2
            val boxY = HEIGHT / 2.0 - boxHeight / 2

            graphicsContext.fill = Color.WHITE
            graphicsContext.fillRect(boxX, boxY, boxWidth, boxHeight)

            graphicsContext.fill = Color.RED
            graphicsContext.fillText(message, boxX + 15, boxY + 20)
            graphicsContext.fillText(timeMessage, boxX + 15, boxY + 40)

            graphicsContext.fillText("Press R to restart", boxX + 15, boxY + 60)
        }
    }

    private fun drawBoard() {
        for (y in 0 until ROWS) for (x in 0 until COLS) {
            val t = board[y][x]
            if (t != -1) {
                drawTetromino(t)
                graphicsContext.fillRect(
                    OFFSET_X + x * CELL_SIZE,
                    OFFSET_Y + y * CELL_SIZE,
                    CELL_SIZE.toDouble(), CELL_SIZE.toDouble()
                )
            }
        }
    }

    private fun drawTetromino(typeIndex: Int) {
        // Egy adott Tetromino kirajzolása
        graphicsContext.fill = when (typeIndex) {
            0 -> Color.BLUE
            1 -> Color.YELLOW
            2 -> Color.PURPLE
            3 -> Color.ORANGE
            4 -> Color.GREEN
            else -> Color.WHITE
        }
    }

    private fun resetGame() {
        // Játék resetálása
        board.forEach { row -> row.fill(-1) }
        tetromino = Tetromino()
        isGameOver = false
        elapsedGameTimeMs = 0L
        dropInterval = INITIAL_DROP_INTERVAL_MS
    }
}
