package com.example.timetableapp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.timetableapp.adapter.PeriodAdapter
import com.example.timetableapp.data.Period
import com.example.timetableapp.util.CsvImporter
import com.example.timetableapp.util.ExcelImporter
import com.example.timetableapp.viewmodel.PeriodViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var tvCurrentDate: TextView
    private lateinit var tvCurrentPeriod: TextView
    private lateinit var tvNextPeriod: TextView
    private lateinit var rvTimetable: RecyclerView
    private lateinit var fabAddPeriod: FloatingActionButton

    private lateinit var periodViewModel: PeriodViewModel
    private lateinit var periodAdapter: PeriodAdapter

    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            updateCurrentTimeAndPeriods()
            handler.postDelayed(this, 60000) // Update every minute
        }
    }

    private val openCsvLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                importFromCsv(uri)
            }
        }
    }

    private val openExcelLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                importFromExcel(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        tvCurrentDate = findViewById(R.id.tvCurrentDate)
        tvCurrentPeriod = findViewById(R.id.tvCurrentPeriod)
        tvNextPeriod = findViewById(R.id.tvNextPeriod)
        rvTimetable = findViewById(R.id.rvTimetable)
        fabAddPeriod = findViewById(R.id.fabAddPeriod)

        // Setup RecyclerView
        periodAdapter = PeriodAdapter { period ->
            showEditPeriodDialog(period)
        }
        rvTimetable.layoutManager = LinearLayoutManager(this)
        rvTimetable.adapter = periodAdapter

        // Setup ViewModel
        periodViewModel = ViewModelProvider(this)[PeriodViewModel::class.java]

        // Observe LiveData - FIXED: Now properly updates the adapter with new data
        periodViewModel.allPeriods.observe(this) { periods ->
            Log.d("MainActivity", "Observed periods: ${periods.size}")
            periodAdapter.submitList(periods)
            updateCurrentTimeAndPeriods()
        }

        // Update current time
        updateCurrentTimeAndPeriods()

        // Setup FAB click listener
        fabAddPeriod.setOnClickListener {
            showAddPeriodDialog()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_import_csv -> {
                openCsvFile()
                true
            }
            R.id.action_import_excel -> {
                openExcelFile()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        handler.post(updateRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(updateRunnable)
    }

    private fun updateCurrentTimeAndPeriods() {
        val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val currentDate = Date()

        tvCurrentDate.text = dateFormat.format(currentDate)

        // Find current and next periods
        val currentTime = timeFormat.format(currentDate)
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1 // Adjust to 1-7 (Monday-Sunday)

        val periods = periodViewModel.allPeriods.value ?: listOf()
        Log.d("MainActivity", "Current periods count: ${periods.size}")

        val todayPeriods = periods.filter { it.dayOfWeek == dayOfWeek }

        var currentPeriod: Period? = null
        var nextPeriod: Period? = null

        for (period in todayPeriods) {
            if (currentTime >= period.startTime && currentTime <= period.endTime) {
                currentPeriod = period
            } else if (currentTime < period.startTime &&
                (nextPeriod == null || period.startTime < nextPeriod.startTime)) {
                nextPeriod = period
            }
        }

        if (currentPeriod != null) {
            tvCurrentPeriod.text = "${currentPeriod.subject} (${currentPeriod.startTime}-${currentPeriod.endTime}, ${currentPeriod.location})"
        } else {
            tvCurrentPeriod.text = "None"
        }

        if (nextPeriod != null) {
            tvNextPeriod.text = "${nextPeriod.subject} (${nextPeriod.startTime}-${nextPeriod.endTime}, ${nextPeriod.location})"
        } else {
            tvNextPeriod.text = "None"
        }
    }

    private fun showAddPeriodDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_period, null)

        val etSubject = dialogView.findViewById<TextInputEditText>(R.id.etSubject)
        val etStartTime = dialogView.findViewById<TextInputEditText>(R.id.etStartTime)
        val etEndTime = dialogView.findViewById<TextInputEditText>(R.id.etEndTime)
        val etLocation = dialogView.findViewById<TextInputEditText>(R.id.etLocation)
        val spinnerDay = dialogView.findViewById<Spinner>(R.id.spinnerDay)
        val etNotes = dialogView.findViewById<TextInputEditText>(R.id.etNotes)

        // Setup spinner
        val days = arrayOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, days)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDay.adapter = adapter

        val dialog = AlertDialog.Builder(this)
            .setTitle("Add New Period")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val subject = etSubject.text.toString().trim()
                val startTime = etStartTime.text.toString().trim()
                val endTime = etEndTime.text.toString().trim()
                val location = etLocation.text.toString().trim()
                val dayOfWeek = spinnerDay.selectedItemPosition + 1 // 1-7 (Monday-Sunday)
                val notes = etNotes.text.toString().trim()

                if (subject.isNotEmpty() && startTime.isNotEmpty() && endTime.isNotEmpty()) {
                    val period = Period(
                        subject = subject,
                        startTime = startTime,
                        endTime = endTime,
                        location = location,
                        dayOfWeek = dayOfWeek,
                        notes = notes
                    )
                    Log.d("MainActivity", "Adding new period: $period")
                    periodViewModel.insert(period)
                    Toast.makeText(this, "Period added", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun showEditPeriodDialog(period: Period) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_period, null)

        val etSubject = dialogView.findViewById<TextInputEditText>(R.id.etSubject)
        val etStartTime = dialogView.findViewById<TextInputEditText>(R.id.etStartTime)
        val etEndTime = dialogView.findViewById<TextInputEditText>(R.id.etEndTime)
        val etLocation = dialogView.findViewById<TextInputEditText>(R.id.etLocation)
        val spinnerDay = dialogView.findViewById<Spinner>(R.id.spinnerDay)
        val etNotes = dialogView.findViewById<TextInputEditText>(R.id.etNotes)

        // Fill with existing data
        etSubject.setText(period.subject)
        etStartTime.setText(period.startTime)
        etEndTime.setText(period.endTime)
        etLocation.setText(period.location)
        etNotes.setText(period.notes)

        // Setup spinner
        val days = arrayOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, days)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDay.adapter = adapter
        spinnerDay.setSelection(period.dayOfWeek - 1)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Edit Period")
            .setView(dialogView)
            .setPositiveButton("Update") { _, _ ->
                val subject = etSubject.text.toString().trim()
                val startTime = etStartTime.text.toString().trim()
                val endTime = etEndTime.text.toString().trim()
                val location = etLocation.text.toString().trim()
                val dayOfWeek = spinnerDay.selectedItemPosition + 1
                val notes = etNotes.text.toString().trim()

                if (subject.isNotEmpty() && startTime.isNotEmpty() && endTime.isNotEmpty()) {
                    val updatedPeriod = Period(
                        id = period.id,
                        subject = subject,
                        startTime = startTime,
                        endTime = endTime,
                        location = location,
                        dayOfWeek = dayOfWeek,
                        notes = notes
                    )
                    Log.d("MainActivity", "Updating period: $updatedPeriod")
                    periodViewModel.update(updatedPeriod)
                    Toast.makeText(this, "Period updated", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Delete") { _, _ ->
                showDeleteConfirmationDialog(period)
            }
            .create()

        dialog.show()
    }

    private fun showDeleteConfirmationDialog(period: Period) {
        AlertDialog.Builder(this)
            .setTitle("Delete Period")
            .setMessage("Are you sure you want to delete ${period.subject}?")
            .setPositiveButton("Delete") { _, _ ->
                Log.d("MainActivity", "Deleting period: $period")
                periodViewModel.delete(period)
                Toast.makeText(this, "Period deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openCsvFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/csv"
        }
        openCsvLauncher.launch(intent)
    }

    private fun openExcelFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/vnd.ms-excel"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                "application/vnd.ms-excel",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            ))
        }
        openExcelLauncher.launch(intent)
    }

    private fun importFromCsv(uri: Uri) {
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val periods = CsvImporter.importFromCsv(inputStream)
                if (periods.isNotEmpty()) {
                    showImportConfirmationDialog(periods)
                } else {
                    Toast.makeText(this, "No valid data found in CSV file", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error importing CSV: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun importFromExcel(uri: Uri) {
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val periods = ExcelImporter.importFromExcel(inputStream)
                if (periods.isNotEmpty()) {
                    showImportConfirmationDialog(periods)
                } else {
                    Toast.makeText(this, "No valid data found in Excel file", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error importing Excel: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showImportConfirmationDialog(periods: List<Period>) {
        AlertDialog.Builder(this)
            .setTitle("Import Timetable")
            .setMessage("Found ${periods.size} periods. Do you want to add them to your timetable?")
            .setPositiveButton("Add") { _, _ ->
                for (period in periods) {
                    periodViewModel.insert(period)
                }
                Toast.makeText(this, "${periods.size} periods imported", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
