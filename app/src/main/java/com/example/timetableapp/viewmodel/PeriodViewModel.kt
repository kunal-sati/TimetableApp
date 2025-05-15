package com.example.timetableapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.timetableapp.data.Period
import com.example.timetableapp.data.PeriodRepository
import com.example.timetableapp.data.TimetableDatabase
import kotlinx.coroutines.launch

class PeriodViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PeriodRepository
    val allPeriods: LiveData<List<Period>>

    init {
        val periodDao = TimetableDatabase.getDatabase(application).periodDao()
        repository = PeriodRepository(periodDao)
        allPeriods = repository.allPeriods
    }

    fun getPeriodsByDay(dayOfWeek: Int): LiveData<List<Period>> {
        return repository.getPeriodsByDay(dayOfWeek)
    }

    fun insert(period: Period) = viewModelScope.launch {
        repository.insert(period)
    }

    fun update(period: Period) = viewModelScope.launch {
        repository.update(period)
    }

    fun delete(period: Period) = viewModelScope.launch {
        repository.delete(period)
    }

    fun deleteAll() = viewModelScope.launch {
        repository.deleteAll()
    }
}
