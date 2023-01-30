package com.example.minesweeperalarm

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Point
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.plus
import kotlin.math.round

class Minesweeper : AppCompatActivity() {
    companion object {
        val spriteNames : Array<String> = arrayOf("empty", "one", "two", "three", "four", "five", "six", "seven", "eight", "plain", "flag", "mine")
    }

    private lateinit var tileSprites: Array<Drawable>
    private lateinit var mineText: TextView
    private lateinit var timeText: TextView
    private lateinit var boardParent: ViewGroup

    private lateinit var gameStopwatch: Stopwatch
    private var boardWidth = 0
    private var boardHeight = 0
    private var mineCount = 0

    private val plainTileSprite: Drawable get() = tileSprites[9]
    private val flagTileSprite: Drawable get() = tileSprites[10]
    private val mineTileSprite: Drawable get() = tileSprites[11]

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_minesweeper)

        tileSprites = Array(spriteNames.size) { i -> getDrawable(spriteNames[i]) }
        mineText = findViewById(R.id.mineText)
        timeText = findViewById(R.id.timeText)
        boardParent = findViewById(R.id.boardParentView)

        gameStopwatch = Stopwatch(30L, ::updateTimeText)
        boardWidth = intent.extras!!.getInt(MinesweeperSetup.boardWidthKey)
        boardHeight = intent.extras!!.getInt(MinesweeperSetup.boardHeightKey)
        mineCount = round(0.1f * boardWidth * boardHeight).toInt()

        updateMineText(0)
        updateTimeText()

        val board = Board(this)
        board.flagEvent += ::updateMineText
        board.sweepMineEvent += ::gameOver
        board.winEvent += ::win

        gameStopwatch.start()
    }

    private fun getDrawable(name: String) : Drawable {
        val id = resources.getIdentifier(name, "drawable", packageName)
        return ResourcesCompat.getDrawable(resources, id, null)!!
    }

    private fun updateMineText(flaggedMineCount: Int) {
        val mineText = "${getString(R.string.mine_text)} ${mineCount - flaggedMineCount}"
        this.mineText.text = mineText
    }

    private fun updateTimeText() {
        runOnUiThread {
            timeText.text = getString(R.string.time_text).plus(" ").plus(gameStopwatch.getElapsedText())
        }
    }

    private fun gameOver() {
        gameStopwatch.stop()
        promptGameEndOptions(getString(R.string.game_over_title), getString(R.string.game_over_message))
    }

    private fun win() {
        gameStopwatch.stop()
        promptGameEndOptions(getString(R.string.win_title), getString(R.string.win_message, boardWidth, boardHeight, gameStopwatch.getElapsedText()))
    }

    private fun replay() {
        val intent = Intent(this, Minesweeper::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION
        intent.putExtra(MinesweeperSetup.boardWidthKey, boardWidth)
        intent.putExtra(MinesweeperSetup.boardHeightKey, boardHeight)
        this.startActivity(intent)

        this.finish()
    }

    // Based on https://www.digitalocean.com/community/tutorials/android-alert-dialog-using-kotlin
    private fun promptGameEndOptions(title: String, message: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
        builder.setMessage(message)

        builder.setPositiveButton(R.string.replay_text) { _, _ ->
            Toast.makeText(this, R.string.replay_text, Toast.LENGTH_SHORT).show()
            replay()
        }

        builder.setNegativeButton(R.string.return_text) { _, _ ->
            // Toast.makeText(this, R.string.return_text, Toast.LENGTH_SHORT).show()
            finish()
        }

        builder.show()
    }

    private class Tile constructor(val button: ImageButton, var isMine: Boolean = false, var digState: State = State.Full) {
        enum class State { Clear, Full, Flag }

        val isCleared : Boolean get() { return digState == State.Clear }
        // val isFull : Boolean get() { return digState == State.Full }
        val isFlagged : Boolean get() { return digState == State.Flag }
    }

    private class Board constructor(private val minesweeper: Minesweeper) {
        val flagEvent = Event<Int>()
        val sweepMineEvent = UnitEvent()
        val winEvent = UnitEvent()

        private val tiles : Array<Array<Tile>>
        private val xRange = 0 until minesweeper.boardWidth
        private val yRange = 0 until minesweeper.boardHeight
        private var coveredCount = minesweeper.boardWidth * minesweeper.boardHeight
        private var flagCount = 0

        init {
            tiles = createBoardLayout()
            setMines()

            sweepMineEvent += ::revealTiles
            winEvent += ::disableTiles
        }

        //region Creation
        private fun createTile(column: LinearLayout, x: Int, y: Int) : Tile {
            val tileButton = ImageButton(minesweeper)
            // tileButton.tag = "Tile [${x}, ${y}]"
            tileButton.setPadding(0, 0, 0, 0)
            tileButton.setImageDrawable(minesweeper.plainTileSprite)
            tileButton.setOnClickListener{ sweepTile(x, y) }
            tileButton.setOnLongClickListener{ toggleFlag(x, y) }
            column.addView(tileButton)

            return Tile(tileButton)
        }

        // Loosely based on https://www.youtube.com/watch?v=B1VsrIYwKNY
        private fun createColumnLayout(x: Int) : Array<Tile> {
            val column = LinearLayout(minesweeper)
            column.orientation = LinearLayout.VERTICAL
            column.setPadding(0, 0, 0, 0)

            val buttons = Array(minesweeper.boardHeight) { y -> createTile(column, x, y) }
            minesweeper.boardParent.addView(column)

            return buttons
        }

        private fun createBoardLayout() : Array<Array<Tile>> {
            return Array(minesweeper.boardWidth) { x -> createColumnLayout(x) }
        }
        //endregion

        //region Setup
        private fun setMines() {
            repeat(minesweeper.mineCount) {
                while (true) {
                    val x = xRange.random()
                    val y = yRange.random()
                    if (!tiles[x][y].isMine) {
//                        tiles[x][y].button.setImageDrawable(minesweeper.mineTileSprite) // For testing
                        tiles[x][y].isMine = true
                        break
                    }
                }
            }
        }
        //endregion

        //region Surrounding Tiles
        private fun getSurroundingPoints(x: Int, y: Int) : Array<Point> {
            val point = Point(x, y)
            val offsets = arrayOf(Point(-1, -1), Point(-1, 0), Point(-1, 1), Point(0, -1), Point(0, 1), Point(1, -1), Point(1, 0), Point(1, 1))
            val points = mutableListOf<Point>()

            for (offset in offsets) {
                val samplePoint = point + offset
                if (samplePoint.x in xRange && samplePoint.y in yRange) points.add(samplePoint)
            }

            return points.toTypedArray()
        }

        private fun surroundingMinesAt(x: Int, y: Int) : Int {
            var mineCount = 0
            for (samplePoint in getSurroundingPoints(x, y)) {
                if (tiles[samplePoint.x][samplePoint.y].isMine) mineCount++
            }

            return mineCount
        }
        //endregion

        //region Tiles Actions
        private fun sweepTile(x: Int, y: Int) {
            if (tiles[x][y].isCleared || tiles[x][y].isFlagged) return
            else if (tiles[x][y].isMine) {
                sweepMineEvent.invoke()
                return
            }
            tiles[x][y].digState = Tile.State.Clear

            val surroundingMineCount = surroundingMinesAt(x, y)
            val button = tiles[x][y].button
            button.setImageDrawable(minesweeper.tileSprites[surroundingMineCount])
            button.isEnabled = false

            if (surroundingMineCount == 0) for (point in getSurroundingPoints(x, y)) sweepTile(point.x, point.y)

            coveredCount--
            if (coveredCount == minesweeper.mineCount) winEvent.invoke()
        }

        private fun toggleFlag(x: Int, y: Int) : Boolean {
            if (tiles[x][y].isCleared) return false

            // Cap number of flags
            var isFlagged = tiles[x][y].isFlagged
            if (!isFlagged && flagCount == minesweeper.mineCount) return true

            // Toggle flag
            tiles[x][y].digState = if (isFlagged) Tile.State.Full else Tile.State.Flag
            isFlagged = tiles[x][y].isFlagged

            tiles[x][y].button.setImageDrawable(if (isFlagged) minesweeper.flagTileSprite else minesweeper.plainTileSprite)
            if (isFlagged) flagCount++ else flagCount--

            flagEvent.invoke(flagCount)
            return true
        }
        //endregion

        //region Final States
        private fun revealTiles() {
            for (x in xRange) {
                for (y in yRange) {
                    val tile = tiles[x][y].button
                    if (!tile.isEnabled) continue

                    tile.isEnabled = false
                    if (tiles[x][y].isMine) tile.setImageDrawable(minesweeper.mineTileSprite)
                }
            }
        }

        private fun disableTiles() {
            for (x in xRange) {
                for (y in yRange) {
                    tiles[x][y].button.isEnabled = false
                }
            }
        }
        //endregion
    }

    class Event<T> {
        private val listeners = mutableSetOf<(T) -> Unit>()

        operator fun plusAssign(observer: (T) -> Unit) {
            listeners.add(observer)
        }

        operator fun minusAssign(observer: (T) -> Unit) {
            listeners.remove(observer)
        }

        operator fun invoke(value: T) {
            for (observer in listeners) observer(value)
        }
    }

    class UnitEvent {
        private val listeners = mutableSetOf<() -> Unit>()

        operator fun plusAssign(observer: () -> Unit) {
            listeners.add(observer)
        }

        operator fun minusAssign(observer: () -> Unit) {
            listeners.remove(observer)
        }

        operator fun invoke() {
            for (observer in listeners) observer()
        }
    }
}