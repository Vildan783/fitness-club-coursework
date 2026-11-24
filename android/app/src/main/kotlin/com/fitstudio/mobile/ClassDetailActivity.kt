package com.fitstudio.mobile

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.fitstudio.mobile.databinding.ActivityClassDetailBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ClassDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityClassDetailBinding
    private var classId = 0
    private val clients = mutableListOf<Named>()
    private val adapter = EnrollmentAdapter { enrollment ->
        AlertDialog.Builder(this)
            .setTitle("Отменить запись")
            .setMessage(enrollment.clientName)
            .setPositiveButton("Отменить") { _, _ -> cancel(enrollment.enrollmentId) }
            .setNegativeButton("Закрыть", null)
            .show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClassDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        classId = intent.getIntExtra(EXTRA_ID, 0)
        setupBar(binding.toolbar, "Занятие")
        binding.enrollments.layoutManager = LinearLayoutManager(this)
        binding.enrollments.adapter = adapter
        binding.edit.setOnClickListener {
            startActivity(Intent(this, ClassFormActivity::class.java).putExtra(EXTRA_ID, classId))
        }
        binding.enroll.setOnClickListener { enroll() }
    }

    override fun onResume() {
        super.onResume()
        load()
    }

    private fun load() {
        lifecycleScope.launch {
            try {
                val db = store()
                val item = withContext(Dispatchers.IO) { db.gymClass(classId) }
                val people = withContext(Dispatchers.IO) { db.clientOptions() }
                val rows = withContext(Dispatchers.IO) { db.enrollments(classId) }
                val free = item.maxParticipants - item.taken
                binding.info.text = listOf(
                    item.title,
                    isoDateTime(item.startsAt) + ", ${item.durationMinutes} мин",
                    "Тренер: ${item.trainerName}",
                    "Зал: ${item.hallName}",
                    "Свободно $free из ${item.maxParticipants}"
                ).joinToString("\n")
                clients.clear()
                clients.addAll(people)
                binding.clients.adapter = ArrayAdapter(this@ClassDetailActivity, android.R.layout.simple_spinner_dropdown_item, clients)
                adapter.submit(rows)
            } catch (ex: Exception) {
                toast(ex.message ?: "Не удалось открыть занятие")
            }
        }
    }

    private fun enroll() {
        val client = binding.clients.selectedItem as? Named
        if (client == null) {
            toast("Нет клиентов")
            return
        }
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) { store().enroll(classId, client.id) }
                toast("Клиент записан")
                load()
            } catch (ex: Exception) {
                toast(ex.message ?: "Не удалось записать клиента")
            }
        }
    }

    private fun cancel(enrollmentId: Int) {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) { store().cancelEnrollment(enrollmentId) }
                toast("Запись отменена, посещение возвращено")
                load()
            } catch (ex: Exception) {
                toast(ex.message ?: "Не удалось отменить запись")
            }
        }
    }
}
