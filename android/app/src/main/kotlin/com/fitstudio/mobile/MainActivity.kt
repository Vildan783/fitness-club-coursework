package com.fitstudio.mobile

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.fitstudio.mobile.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.openClients.setOnClickListener {
            startActivity(Intent(this, ClientsActivity::class.java))
        }
        binding.openClasses.setOnClickListener {
            startActivity(Intent(this, ClassesActivity::class.java))
        }
    }
}
