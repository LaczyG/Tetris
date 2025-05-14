package com.example

import kotlin.random.Random

data class Block(val x: Int, val y: Int)

class Tetromino {
    // típusindex: 0=Straight, 1=Square, 2=T, 3=L, 4=Skew
    val typeIndex: Int
    private var shape: List<Block> // alakzat
    private var posX: Int
    private var posY: Int
    var isPlaced = false
        private set

    init {
        val shapes = listOf(
            listOf(Block(0, 0), Block(1, 0), Block(2, 0), Block(3, 0)),  // Straight
            listOf(Block(0, 0), Block(1, 0), Block(0, 1), Block(1, 1)),  // Square
            listOf(Block(1, 0), Block(2, 0), Block(3, 0), Block(2, 1)),  // T
            listOf(Block(0, 1), Block(0, 2), Block(0, 3), Block(1, 3)),  // L
            listOf(Block(0, 1), Block(1, 1), Block(1, 0), Block(2, 0))   // Skew
        )
        typeIndex = Random.nextInt(shapes.size) // kiválaszt random egy alakzatot
        shape = shapes[typeIndex] // változóban tárolja
        // Fent középen kezdődjön:
        posX = Game.COLS / 2 - 1
        posY = 0
    }

    /**
     * @return Visszaadja a tetro blokkok pozízióit a táblán.
     */
    fun getAbsoluteBlocks(): List<Block> =
        shape.map { Block(it.x + posX, it.y + posY) }

    fun move(dx: Int, dy: Int) {
        // mozgatja adott irányba
        posX += dx
        posY += dy
    }

    fun canMove(dx: Int, dy: Int, board: Array<IntArray>): Boolean {
        /**
         *
         * @return ha igaz -> mozgathatóoda,  ha hamis -> nem mozgatható oda
         */
        for (block in getAbsoluteBlocks()) {
            val nx = block.x + dx
            val ny = block.y + dy
            if (nx !in 0 until Game.COLS || ny !in 0 until Game.ROWS || board[ny][nx] != -1) {
                return false
            }
        }
        return true
    }

    /**
     * Elhelyezi a pályán az adott blockot
     */
    fun placeOnBoard(board: Array<IntArray>) {
        for (block in getAbsoluteBlocks()) {
            board[block.y][block.x] = typeIndex
            isPlaced = true
        }
    }

    /**
     * Elforgatást végez
     * @return sikerült -e elforgatni (pl pályahatárt nem lép -e át)
     */
    fun rotate(board: Array<IntArray>): Boolean {
        // (x, y) -> (y, -x)
        val rotatedShape = shape.map { standardBlock ->
            Block(standardBlock.y, -standardBlock.x)
        }
        // új poziciók
        val newPositions = rotatedShape.map { blokk ->
            Block(blokk.x + posX, blokk.y + posY)
        }

        // Validáció (nem megy ki a pályáról és nincs másik blokk)
        val valid = newPositions.all { blokk ->
            blokk.x in 0 until Game.COLS &&
                    blokk.y in 0 until Game.ROWS &&
                    board[blokk.y][blokk.x] == -1
        }

        // Ha minden blokk érvényes, akkor elfogadjuk az új alakzatot
        return if (valid) {
            shape = rotatedShape
            true
        } else {
            false
        }
    }

}
