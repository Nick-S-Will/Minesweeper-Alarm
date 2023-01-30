package com.example.minesweeperalarm

import android.widget.SeekBar
import android.widget.TextView

class LabeledSlider(private val slider: SeekBar, private val label: TextView, private val minValue: Int = 0, private val valueIncrement: Int = 1) {
    init {
        label.text = MinesweeperSetup.boardWidthRange.first.toString()

        slider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                label.text = getValue().toString()
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })
    }

    private fun getProgression() : IntProgression = minValue..(minValue + (valueIncrement * slider.max)) step valueIncrement

    fun getValue() : Int = minValue + (valueIncrement * slider.progress)
    fun setValue(value: Int) {
        if (value !in getProgression()) return

        slider.progress = (value - minValue) / valueIncrement
    }
}