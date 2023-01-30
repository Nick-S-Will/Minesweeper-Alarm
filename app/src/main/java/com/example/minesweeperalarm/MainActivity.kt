package com.example.minesweeperalarm

import android.app.ActionBar.LayoutParams
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.*
import java.time.*

class MainActivity : AppCompatActivity() {
    private val alarmDataArrayFileName = "alarmDataArray.json"

    private lateinit var nextAlarmRemainingTimeText: TextView
    private lateinit var nextAlarmDateText: TextView
    private lateinit var alarmParent: LinearLayout

    private val alarms: ArrayList<Alarm> = arrayListOf()
    private val getNewAlarmData = registerForActivityResult(AlarmSetup.AlarmContract(), ::addAlarm)
    private val editAlarmData = registerForActivityResult(AlarmSetup.AlarmContract(), ::editAlarm)
    private var editIndex = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        nextAlarmRemainingTimeText = findViewById(R.id.nextAlarmRemainingTimeText)
        nextAlarmDateText = findViewById(R.id.nextAlarmDateText)
        alarmParent = findViewById(R.id.alarmParent)

        val addAlarmButton: FloatingActionButton = findViewById(R.id.addAlarmButton)
        addAlarmButton.setOnClickListener { getNewAlarmData.launch(null) }

        val setupGameButton: Button = findViewById(R.id.setupGameButton)
        setupGameButton.setOnClickListener{ openMinesweeperSetup() }

        val alarmData = loadAlarmData()
        if (alarmData != null) for (data in alarmData) createAlarmView(data)
        updateNextAlarmTexts()
    }

    //region Get time
    /**
     * Calculates the shortest duration between startTime and the alarms' alertTime.
     * If alarms is empty, null is returned
     */
    private fun getNextAlarmTime(startTime: LocalTime = LocalTime.now()) : LocalTime? {
        var index = -1
        var minDuration = Duration.ofDays(10L)
        for (i in 0 until alarms.count()) {
            var duration = Duration.between(startTime, alarms[i].data.alertTime.toLocalTime())
            if (duration.toMinutes() <= 0L) duration = Duration.ofDays(1L).plus(duration)

            if (duration.toMinutes() < minDuration.toMinutes()) {
                index = i
                minDuration = duration
            }
        }

        return if (index >= 0) alarms[index].data.alertTime.toLocalTime() else null
    }

    private fun getFormattedRemainingTime(time1: LocalTime, time2: LocalTime) : String {
        var remainingTime = Duration.between(time1, time2)
        if (remainingTime.isNegative) remainingTime = remainingTime.plus(Duration.ofDays(1L))

        val remainingMinutes = remainingTime.toMinutes()
        val remainingHours = remainingMinutes / 60L

        return getString(R.string.remaining_time_to_alarm, remainingHours, remainingMinutes - (60 * remainingHours))
    }

    private fun getFormattedTime(time: LocalTime) : String {
        var hour = time.hour
        val minute = String.format("%02d", time.minute)
        val amPM = if (hour < 12) "AM" else "PM"
        if (hour > 12) hour -= 12
        else if (hour == 0) hour = 12

        return "$hour:$minute $amPM"
    }

    private fun getFormattedDateTime(date: LocalDate, time: LocalTime) : String {
        val textRange = 0..2
        var weekDay = date.dayOfWeek.toString().substring(textRange).lowercase()
        weekDay = weekDay.substring(0, 1).uppercase().plus(weekDay.substring(1))
        var month = date.month.toString().substring(textRange).lowercase()
        month = month.substring(0, 1).uppercase().plus(month.substring(1))
        val day = date.dayOfMonth.toString()

        return "$weekDay, $month $day, ${getFormattedTime(time)}"
    }
    //endregion

    //region Update views
    private fun updateNextAlarmTexts() {
        if (alarms.isEmpty()) {
            nextAlarmRemainingTimeText.text = getString(R.string.no_alarms)
            nextAlarmDateText.text = ""
        }
        else if (alarms.all { alarm -> !alarm.isEnabled }) {
            nextAlarmRemainingTimeText.text = getString(R.string.no_alarms_enabled)
            nextAlarmDateText.text = ""
        }
        else {
            val currentTime = LocalTime.now()
            val nextAlarmTime = getNextAlarmTime(currentTime)
            val daysTo = if (Duration.between(currentTime, nextAlarmTime!!).toMinutes() < 0) 1 else 0

            nextAlarmRemainingTimeText.text = getFormattedRemainingTime(currentTime, nextAlarmTime)
            nextAlarmDateText.text = getFormattedDateTime(LocalDate.now().plus(Period.ofDays(daysTo)), nextAlarmTime)
        }
    }

    private fun updateAlarmView(alarm: Alarm) {
        alarm.nameText.text = alarm.data.name
        alarm.timeText.text = getFormattedTime(alarm.data.alertTime.toLocalTime())
    }
    //endregion

    //region Create Alarm
    private fun createAlarmView(alarmData: AlarmSetup.AlarmData) {
        val alarmCard = CardView(this)
        alarmCard.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, resources.getDimensionPixelSize(R.dimen.alarm_card_height))
        alarmCard.radius = resources.getDimension(R.dimen.alarm_card_radius)
        val padding = resources.getDimensionPixelSize(R.dimen.alarm_card_content_padding)
        alarmCard.setContentPadding(padding, padding, padding, padding)
        alarmCard.setCardBackgroundColor(getColor(R.color.teal_200))
        alarmParent.addView(alarmCard)

        val space = Space(this)
        space.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, resources.getDimensionPixelSize(R.dimen.alarm_card_spacing))
        alarmParent.addView(space)

        val constraintLayout = ConstraintLayout(this)
        constraintLayout.id = View.generateViewId()
        constraintLayout.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        alarmCard.addView(constraintLayout)

        val alarmNameText = TextView(this)
        alarmNameText.id = View.generateViewId()
        constraintLayout.addView(alarmNameText)
        alarmNameText.layoutParams = ConstraintLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        alarmNameText.setTextColor(getColor(R.color.black))
        alarmNameText.textSize = resources.getDimension(R.dimen.alarm_name_text_size)

        val alarmTimeText = TextView(this)
        alarmTimeText.id = View.generateViewId()
        constraintLayout.addView(alarmTimeText)
        alarmTimeText.layoutParams = ConstraintLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        alarmTimeText.setTextColor(getColor(R.color.black))
        alarmTimeText.textSize = resources.getDimension(R.dimen.alarm_time_text_size)

        val alarm = Alarm(alarmData, alarmNameText, alarmTimeText)
        alarms.add(alarm)
        alarmCard.setOnLongClickListener{ promptEditAlarm(alarm, alarmCard, space) }
        updateAlarmView(alarm)

        val enabledSwitch = SwitchCompat(this)
        enabledSwitch.id = View.generateViewId()
        constraintLayout.addView(enabledSwitch)
        enabledSwitch.layoutParams = ConstraintLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        enabledSwitch.isChecked = true
        enabledSwitch.setOnCheckedChangeListener{ _, checked ->
            alarm.isEnabled = checked
            updateNextAlarmTexts()
        }

        val constraintSet = ConstraintSet()
        constraintSet.clone(constraintLayout)
        constraintSet.connect(alarmNameText.id, ConstraintSet.START, constraintLayout.id, ConstraintSet.START)
        constraintSet.connect(alarmNameText.id, ConstraintSet.TOP, constraintLayout.id, ConstraintSet.TOP)
        constraintSet.connect(alarmTimeText.id, ConstraintSet.START, constraintLayout.id, ConstraintSet.START)
        constraintSet.connect(alarmTimeText.id, ConstraintSet.BOTTOM, constraintLayout.id, ConstraintSet.BOTTOM)
        constraintSet.connect(enabledSwitch.id, ConstraintSet.TOP, constraintLayout.id, ConstraintSet.TOP)
        constraintSet.connect(enabledSwitch.id, ConstraintSet.END, constraintLayout.id, ConstraintSet.END)
        constraintSet.connect(enabledSwitch.id, ConstraintSet.BOTTOM, constraintLayout.id, ConstraintSet.BOTTOM)
        constraintSet.applyTo(constraintLayout)
    }

    private fun addAlarm(newAlarm: AlarmSetup.AlarmData?) {
        if (newAlarm == null || alarms.any{ alarm -> newAlarm.alertTime == alarm.data.alertTime }) return

        createAlarmView(newAlarm)
        updateNextAlarmTexts()
        saveAlarmData()
    }
    //endregion

    //region Edit Alarm
    // Based on https://www.digitalocean.com/community/tutorials/android-alert-dialog-using-kotlin
    private fun promptEditAlarm(alarm: Alarm, alarmCard: CardView, space: Space) : Boolean {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.alarm_delete_prompt_title))
        builder.setMessage(getString(R.string.alarm_delete_prompt_message, alarm.data.name))

        builder.setPositiveButton(getString(R.string.alarm_delete_prompt_positive)) { _, _ ->
            alarms.remove(alarm)
            alarmParent.removeView(alarmCard)
            alarmParent.removeView(space)
            Toast.makeText(this, getString(R.string.alarm_delete_prompt_confirmed, alarm.data.name), Toast.LENGTH_SHORT).show()
            updateNextAlarmTexts()
        }

        builder.setNeutralButton(getString(R.string.alarm_edit_prompt)) { _, _ ->
            editIndex = alarms.indexOf(alarm)
            editAlarmData.launch(alarm.data)
        }

        builder.setNegativeButton(R.string.return_text) { _, _ -> }

        builder.show()
        return true
    }

    private fun editAlarm(alarmData: AlarmSetup.AlarmData?) {
        if (alarmData == null) return

        alarms[editIndex].data = alarmData
        updateAlarmView(alarms[editIndex])
        editIndex = -1
        saveAlarmData()
    }
    //endregion

    //region Save/Load AlarmData
    private fun saveAlarmData() {
        val alarmDataArray = alarms.map { it.data }.toTypedArray()
        val jsonString = Json.encodeToString(alarmDataArray)

        //try {
        val fileOutStream: FileOutputStream = openFileOutput(alarmDataArrayFileName, MODE_PRIVATE)
        fileOutStream.write(jsonString.toByteArray())
        fileOutStream.close()
        //}
        //catch (_: FileNotFoundException) { }
        //catch (_: IOException) { }
    }

    private fun loadAlarmData() : Array<AlarmSetup.AlarmData>? {
        var jsonString = ""
        try {
            val fileInStream: FileInputStream = openFileInput(alarmDataArrayFileName)
            val inStreamReader = InputStreamReader(fileInStream)
            val bufferedReader = BufferedReader(inStreamReader)
            val stringBuilder = StringBuilder()
            var line: String?
            while (bufferedReader.readLine().also { line = it } != null) stringBuilder.append(line)
            jsonString = stringBuilder.toString()
        }
        catch (_: FileNotFoundException) {}
        catch (_: IOException) {}

        return if (jsonString.isEmpty()) null else Json.decodeFromString(jsonString)
    }
    //endregion

    private fun openMinesweeperSetup() {
        val intent = Intent(this, MinesweeperSetup::class.java)
        startActivity(intent)
    }

    data class Alarm(var data: AlarmSetup.AlarmData, val nameText: TextView, val timeText: TextView, var isEnabled: Boolean = true)
}