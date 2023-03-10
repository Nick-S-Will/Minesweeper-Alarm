package com.example.minesweeperalarm

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class MinesweeperSetup : AppCompatActivity() {
    companion object {
        const val boardWidthKey = "boardWidth"
        const val boardHeightKey = "boardHeight"
        val boardWidthRange = 7..11
        val boardHeightRange = 7..21
    }

    private lateinit var widthSlider : LabeledSlider
    private lateinit var heightSlider : LabeledSlider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_minesweeper_setup)

        widthSlider = LabeledSlider(findViewById(R.id.widthSlider), findViewById(R.id.widthText), boardWidthRange.first, 2)
        heightSlider = LabeledSlider(findViewById(R.id.heightSlider), findViewById(R.id.heightText), boardHeightRange.first, 2)

        val playButton: Button = findViewById(R.id.playButton)
        playButton.setOnClickListener{ openMinesweeper() }
    }

    private fun openMinesweeper() {
        val intent = Intent(this, Minesweeper::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION
        intent.putExtra(boardWidthKey, widthSlider.getValue())
        intent.putExtra(boardHeightKey, heightSlider.getValue())
        startActivity(intent)
    }
}