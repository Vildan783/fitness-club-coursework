package com.fitstudio.mobile

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.fitstudio.mobile.databinding.ItemCardBinding
import com.fitstudio.mobile.databinding.ItemEnrollmentBinding
import com.google.android.material.appbar.MaterialToolbar
import java.util.Calendar

fun AppCompatActivity.toast(text: String) {
    Toast.makeText(this, text, Toast.LENGTH_LONG).show()
}

fun AppCompatActivity.setupBar(toolbar: MaterialToolbar, title: String) {
    setSupportActionBar(toolbar)
    supportActionBar?.title = title
    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    toolbar.setTitleTextColor(getColor(R.color.white))
    toolbar.setNavigationOnClickListener { finish() }
    toolbar.navigationIcon?.setTint(getColor(R.color.white))
}

fun AppCompatActivity.pickDate(current: String, onPick: (String) -> Unit) {
    val calendar = Calendar.getInstance()
    if (current.length >= 10) {
        calendar.set(Calendar.YEAR, current.substring(0, 4).toInt())
        calendar.set(Calendar.MONTH, current.substring(5, 7).toInt() - 1)
        calendar.set(Calendar.DAY_OF_MONTH, current.substring(8, 10).toInt())
    }
    DatePickerDialog(
        this,
        { _, year, month, day -> onPick("%04d-%02d-%02d".format(year, month + 1, day)) },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).show()
}

fun AppCompatActivity.pickTime(current: String, onPick: (String) -> Unit) {
    val hour = current.take(2).toIntOrNull() ?: 18
    val minute = current.drop(3).take(2).toIntOrNull() ?: 0
    TimePickerDialog(this, { _, h, m -> onPick("%02d:%02d".format(h, m)) }, hour, minute, true).show()
}

fun today(): String {
    val calendar = Calendar.getInstance()
    return "%04d-%02d-%02d".format(
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH) + 1,
        calendar.get(Calendar.DAY_OF_MONTH)
    )
}

class CardAdapter(
    private val title: (Int) -> String,
    private val subtitle: (Int) -> String,
    private val onClick: (Int) -> Unit
) : RecyclerView.Adapter<CardAdapter.Holder>() {
    private var count = 0

    fun submit(size: Int) {
        count = size
        notifyDataSetChanged()
    }

    override fun getItemCount() = count

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.binding.title.text = title(position)
        holder.binding.subtitle.text = subtitle(position)
        holder.binding.root.setOnClickListener { onClick(position) }
    }

    class Holder(val binding: ItemCardBinding) : RecyclerView.ViewHolder(binding.root)
}

class EnrollmentAdapter(
    private val onCancel: (Enrollment) -> Unit
) : RecyclerView.Adapter<EnrollmentAdapter.Holder>() {
    private val items = mutableListOf<Enrollment>()

    fun submit(data: List<Enrollment>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemEnrollmentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        holder.binding.title.text = item.clientName
        holder.binding.subtitle.text = "${item.status} · ${isoDateTime(item.enrolledAt)}"
        holder.binding.cancel.setOnClickListener { onCancel(item) }
    }

    class Holder(val binding: ItemEnrollmentBinding) : RecyclerView.ViewHolder(binding.root)
}
