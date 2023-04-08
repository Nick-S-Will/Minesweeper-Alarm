package com.example.minesweeperalarm

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContract
import java.time.LocalTime

class AlarmSetup : AppCompatActivity() {
    companion object {
        const val editAlarmKey = "editAlarm"
    }

    private lateinit var timePicker: TimePicker
    private lateinit var alarmNameInput: EditText
    private lateinit var widthSlider : LabeledSlider
    private lateinit var heightSlider : LabeledSlider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm_setup)

        timePicker = findViewById(R.id.timePicker)
        alarmNameInput = findViewById(R.id.alarmNameInput)
        widthSlider = LabeledSlider(findViewById(R.id.widthSliderAlarm), findViewById(R.id.widthTextAlarm), MinesweeperSetup.boardWidthRange.first, 2)
        heightSlider = LabeledSlider(findViewById(R.id.heightSliderAlarm), findViewById(R.id.heightTextAlarm), MinesweeperSetup.boardHeightRange.first, 2)

        val cancelButton: Button = findViewById(R.id.cancelButton)
        cancelButton.setOnClickListener { finish() }

        val saveButton: Button = findViewById(R.id.saveButton)
        saveButton.setOnClickListener { saveAlarm() }

        val givenAlarm = intent.extras?.getSerializable(editAlarmKey) as AlarmData?
        if (givenAlarm != null) {
            timePicker.hour = givenAlarm.alertTime.hour
            timePicker.minute = givenAlarm.alertTime.minute
            alarmNameInput.setText(givenAlarm.name)
            widthSlider.setValue(givenAlarm.boardWidth)
            heightSlider.setValue(givenAlarm.boardHeight)
        }
        else {
            val now = LocalTime.now()
            timePicker.hour = now.hour
            timePicker.minute = now.minute
        }
    }

    private fun saveAlarm() {
        val alarmName = alarmNameInput.text.toString()
        if (alarmName.isEmpty()) {
            Toast.makeText(this, getString(R.string.unnamed_alarm_message), Toast.LENGTH_SHORT).show()
            return
        }

        val inputTime = AlarmTime(timePicker.hour, timePicker.minute)
        val alarmData = AlarmData(alarmName, inputTime, widthSlider.getValue(), heightSlider.getValue())

        setResult(RESULT_OK, Intent().putExtra(editAlarmKey, alarmData))
        finish()
    }

    // Serializable wrapper for LocalTime
    @kotlinx.serialization.Serializable
    data class AlarmTime(val hour: Int, val minute: Int) : java.io.Serializable {
        fun toLocalTime() : LocalTime = LocalTime.of(hour, minute)
    }

    @kotlinx.serialization.Serializable
    data class AlarmData(var name: String, var alertTime: AlarmTime, var boardWidth: Int, var boardHeight: Int, var requestCode: Int = Int.MIN_VALUE, var isEnabled: Boolean = true) : java.io.Serializable

    class AlarmContract : ActivityResultContract<AlarmData?, AlarmData?>() {
        override fun createIntent(context: Context, input: AlarmData?): Intent {
            val intent = Intent(context, AlarmSetup::class.java)
            if (input != null) intent.putExtra(editAlarmKey, input)
            return intent
        }

        override fun parseResult(resultCode: Int, intent: Intent?): AlarmData? {
            val serializable = intent?.getSerializableExtra(editAlarmKey)
            return if (serializable == null) null else serializable as AlarmData
        }
    }
}