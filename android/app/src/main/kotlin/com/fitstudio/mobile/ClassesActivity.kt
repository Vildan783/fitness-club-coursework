package com.fitstudio.mobile

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.fitstudio.mobile.databinding.ActivityClassesBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ClassesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityClassesBinding
    private val classes = mutableListOf<GymClass>()
    private val adapter = CardAdapter(
        title = { classes[it].title },
        subtitle = {
            val item = classes[it]
            val free = item.maxParticipants - item.taken
            "${isoDateTime(item.startsAt)} · ${item.trainerName}\n${item.hallName} · свободно $free из ${item.maxParticipants}"
        },
        onClick = {
            startActivity(Intent(this, ClassDetailActivity::class.java).putExtra(EXTRA_ID, classes[it].classId))
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClassesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupBar(binding.toolbar, "Расписание")
        binding.list.layoutManager = LinearLayoutManager(this)
        binding.list.adapter = adapter
        binding.search.setOnClickListener { load() }
        binding.add.setOnClickListener {
            startActivity(Intent(this, ClassFormActivity::class.java).putExtra(EXTRA_ID, 0))
        }
    }

    override fun onResume() {
        super.onResume()
        load()
    }

    private fun load() {
        val query = binding.query.text?.toString().orEmpty()
        binding.progress.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val rows = withContext(Dispatchers.IO) { store().classes(query) }
                classes.clear()
                classes.addAll(rows)
                adapter.submit(classes.size)
                binding.empty.visibility = if (classes.isEmpty()) View.VISIBLE else View.GONE
            } catch (ex: Exception) {
                toast(ex.message ?: "Не удалось загрузить расписание")
            } finally {
                binding.progress.visibility = View.GONE
            }
        }
    }
}
