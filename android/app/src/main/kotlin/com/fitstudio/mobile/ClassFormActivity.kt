package com.fitstudio.mobile

import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.fitstudio.mobile.databinding.ActivityClassFormBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ClassFormActivity : AppCompatActivity() {
    private lateinit var binding: ActivityClassFormBinding
    private var classId = 0
    private var date = today()
    private var time = "18:00"
    private val trainers = mutableListOf<Named>()
    private val halls = mutableListOf<Named>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClassFormBinding.inflate(layoutInflater)
        setContentView(binding.root)
        classId = intent.getIntExtra(EXTRA_ID, 0)
        setupBar(binding.toolbar, if (classId == 0) "Новое занятие" else "Занятие")
        showWhen()
        binding.date.setOnClickListener { pickDate(date) { date = it; showWhen() } }
        binding.time.setOnClickListener { pickTime(time) { time = it; showWhen() } }
        binding.save.setOnClickListener { save() }
        load()
    }

    private fun showWhen() {
        binding.date.text = "Дата: $date"
        binding.time.text = "Время: $time"
    }

    private fun load() {
        lifecycleScope.launch {
            try {
                val db = store()
                val trainerRows = withContext(Dispatchers.IO) { db.trainers() }
                val hallRows = withContext(Dispatchers.IO) { db.halls() }
                trainers.clear()
                trainers.addAll(trainerRows)
                halls.clear()
                halls.addAll(hallRows)
                binding.trainers.adapter = ArrayAdapter(this@ClassFormActivity, android.R.layout.simple_spinner_dropdown_item, trainers)
                binding.halls.adapter = ArrayAdapter(this@ClassFormActivity, android.R.layout.simple_spinner_dropdown_item, halls)
                if (classId != 0) {
                    val item = withContext(Dispatchers.IO) { db.gymClass(classId) }
                    binding.title.setText(item.title)
                    binding.duration.setText(item.durationMinutes.toString())
                    binding.maxParticipants.setText(item.maxParticipants.toString())
                    date = isoDate(item.startsAt)
                    time = isoDateTime(item.startsAt).takeLast(5)
                    showWhen()
                    val trainerIndex = trainers.indexOfFirst { it.id == item.trainerId }
                    val hallIndex = halls.indexOfFirst { it.id == item.hallId }
                    if (trainerIndex >= 0) binding.trainers.setSelection(trainerIndex)
                    if (hallIndex >= 0) binding.halls.setSelection(hallIndex)
                }
            } catch (ex: Exception) {
                toast(ex.message ?: "Не удалось открыть форму")
            }
        }
    }

    private fun save() {
        val title = binding.title.text?.toString()?.trim().orEmpty()
        val trainer = binding.trainers.selectedItem as? Named
        val hall = binding.halls.selectedItem as? Named
        val duration = binding.duration.text?.toString()?.toIntOrNull() ?: 0
        val max = binding.maxParticipants.text?.toString()?.toIntOrNull() ?: 0
        if (title.length < 3 || trainer == null || hall == null || duration <= 0 || max <= 0) {
            toast("Заполните название, тренера, зал, длительность и лимит")
            return
        }
        binding.save.isEnabled = false
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    store().saveClass(classId, title, trainer.id, hall.id, "$date $time:00", duration, max)
                }
                toast("Сохранено")
                finish()
            } catch (ex: Exception) {
                binding.save.isEnabled = true
                toast(ex.message ?: "Не удалось сохранить занятие")
            }
        }
    }
}
