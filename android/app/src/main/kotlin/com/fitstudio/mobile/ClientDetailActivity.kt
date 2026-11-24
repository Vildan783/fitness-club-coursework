package com.fitstudio.mobile

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.fitstudio.mobile.databinding.ActivityClientDetailBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ClientDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityClientDetailBinding
    private var clientId = 0
    private var start = today()
    private val plans = mutableListOf<Plan>()
    private val memberships = mutableListOf<Membership>()
    private val adapter = CardAdapter(
        title = { memberships[it].planName },
        subtitle = {
            val left = memberships[it].visitsLeft?.toString() ?: "безлимит"
            "${memberships[it].status} · ${isoDate(memberships[it].startDate)} — ${isoDate(memberships[it].endDate)} · осталось $left"
        },
        onClick = {}
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClientDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        clientId = intent.getIntExtra(EXTRA_ID, 0)
        setupBar(binding.toolbar, "Карточка клиента")
        binding.memberships.layoutManager = LinearLayoutManager(this)
        binding.memberships.adapter = adapter
        binding.edit.setOnClickListener {
            startActivity(Intent(this, ClientFormActivity::class.java).putExtra(EXTRA_ID, clientId))
        }
        binding.startDate.setOnClickListener {
            pickDate(start) {
                start = it
                showStart()
            }
        }
        binding.issue.setOnClickListener { issue() }
        showStart()
    }

    override fun onResume() {
        super.onResume()
        load()
    }

    private fun showStart() {
        binding.startDate.text = "Дата начала: $start"
    }

    private fun load() {
        lifecycleScope.launch {
            try {
                val db = store()
                val client = withContext(Dispatchers.IO) { db.client(clientId) }
                val planRows = withContext(Dispatchers.IO) { db.plans() }
                val passRows = withContext(Dispatchers.IO) { db.memberships(clientId) }
                binding.info.text = listOf(
                    client.fullName,
                    client.phone,
                    client.email ?: "почта не указана",
                    "Дата рождения: ${isoDate(client.birthDate)}",
                    "В клубе с ${isoDate(client.registeredAt)}"
                ).joinToString("\n")
                plans.clear()
                plans.addAll(planRows)
                binding.plans.adapter = ArrayAdapter(this@ClientDetailActivity, android.R.layout.simple_spinner_dropdown_item, plans)
                memberships.clear()
                memberships.addAll(passRows)
                adapter.submit(memberships.size)
            } catch (ex: Exception) {
                toast(ex.message ?: "Не удалось открыть карточку")
            }
        }
    }

    private fun issue() {
        val plan = binding.plans.selectedItem as? Plan
        if (plan == null) {
            toast("Нет тарифов")
            return
        }
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) { store().issueMembership(clientId, plan.planId, start) }
                toast("Абонемент оформлен")
                load()
            } catch (ex: Exception) {
                toast(ex.message ?: "Не удалось оформить абонемент")
            }
        }
    }
}
