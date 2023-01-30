package com.example.minesweeperalarm

import java.util.*
import kotlin.concurrent.fixedRateTimer

class Stopwatch(private val millisecondPeriod: Long, private val action: () -> Unit) {

    private var timer: Timer? = null
    private var elapsedMilliseconds = 0L

    private val isRunning : Boolean get() = timer != null

    fun start() {
        if (isRunning) return

        elapsedMilliseconds = 0L
        timer = fixedRateTimer("stopwatch", true, 0L, millisecondPeriod) {
            action()
            elapsedMilliseconds += millisecondPeriod
        }
    }

    fun stop() {
        if (!isRunning) return

        timer!!.cancel()
        timer = null
    }

    fun getElapsedText(): String {
        return String.format("%.2f", elapsedMilliseconds / 1000f)
    }
}