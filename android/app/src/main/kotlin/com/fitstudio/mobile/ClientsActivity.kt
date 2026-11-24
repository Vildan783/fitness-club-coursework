package com.fitstudio.mobile

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.fitstudio.mobile.databinding.ActivityClientsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ClientsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityClientsBinding
    private val clients = mutableListOf<Client>()
    private val adapter = CardAdapter(
        title = { clients[it].fullName },
        subtitle = { "${clients[it].phone} · с ${isoDate(clients[it].registeredAt)}" },
        onClick = {
            startActivity(Intent(this, ClientDetailActivity::class.java).putExtra(EXTRA_ID, clients[it].clientId))
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClientsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupBar(binding.toolbar, "Клиенты")
        binding.list.layoutManager = LinearLayoutManager(this)
        binding.list.adapter = adapter
        binding.search.setOnClickListener { load() }
        binding.add.setOnClickListener {
            startActivity(Intent(this, ClientFormActivity::class.java).putExtra(EXTRA_ID, 0))
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
                val rows = withContext(Dispatchers.IO) { store().clients(query) }
                clients.clear()
                clients.addAll(rows)
                adapter.submit(clients.size)
                binding.empty.visibility = if (clients.isEmpty()) View.VISIBLE else View.GONE
            } catch (ex: Exception) {
                toast(ex.message ?: "Не удалось загрузить клиентов")
            } finally {
                binding.progress.visibility = View.GONE
            }
        }
    }
}
