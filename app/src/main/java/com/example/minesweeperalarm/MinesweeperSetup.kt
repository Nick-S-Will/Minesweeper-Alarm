package com.example.minesweeperalarm

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class MinesweeperSetup : AppCompatActivity() {
    companion object {
        val boardWidthRange = 7..11
        val boardHeightRange = 7..21

        fun open(sourceContext: Context) {
            val intent = Intent(sourceContext, MinesweeperSetup::class.java)
            sourceContext.startActivity(intent)
        }
    }

    private lateinit var widthSlider : LabeledSlider
    private lateinit var heightSlider : LabeledSlider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_minesweeper_setup)

        widthSlider = LabeledSlider(findViewById(R.id.widthSlider), findViewById(R.id.widthText), boardWidthRange.first, 2)
        heightSlider = LabeledSlider(findViewById(R.id.heightSlider), findViewById(R.id.heightText), boardHeightRange.first, 2)

        val playButton: Button = findViewById(R.id.playButton)
        playButton.setOnClickListener{ Minesweeper.open(this, widthSlider.getValue(), heightSlider.getValue(), true) }
    }
}