package com.fitstudio.mobile

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.fitstudio.mobile.databinding.ActivityClientFormBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ClientFormActivity : AppCompatActivity() {
    private lateinit var binding: ActivityClientFormBinding
    private var clientId = 0
    private var birth = "2000-01-01"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClientFormBinding.inflate(layoutInflater)
        setContentView(binding.root)
        clientId = intent.getIntExtra(EXTRA_ID, 0)
        setupBar(binding.toolbar, if (clientId == 0) "Новый клиент" else "Клиент")
        showBirth()
        binding.birthDate.setOnClickListener { pickDate(birth) { birth = it; showBirth() } }
        binding.save.setOnClickListener { save() }
        if (clientId != 0) load()
    }

    private fun showBirth() {
        binding.birthDate.text = "Дата рождения: $birth"
    }

    private fun load() {
        lifecycleScope.launch {
            try {
                val client = withContext(Dispatchers.IO) { store().client(clientId) }
                binding.fullName.setText(client.fullName)
                binding.phone.setText(client.phone)
                binding.email.setText(client.email.orEmpty())
                birth = isoDate(client.birthDate)
                showBirth()
            } catch (ex: Exception) {
                toast(ex.message ?: "Не удалось открыть клиента")
            }
        }
    }

    private fun save() {
        val name = binding.fullName.text?.toString()?.trim().orEmpty()
        val phone = binding.phone.text?.toString()?.trim().orEmpty()
        val email = binding.email.text?.toString()?.trim().orEmpty()
        if (name.length < 3 || phone.length < 6) {
            toast("Укажите ФИО и телефон")
            return
        }
        binding.save.isEnabled = false
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    store().saveClient(clientId, name, phone, email.ifBlank { null }, birth)
                }
                toast("Сохранено")
                finish()
            } catch (ex: Exception) {
                binding.save.isEnabled = true
                toast(ex.message ?: "Не удалось сохранить")
            }
        }
    }
}
