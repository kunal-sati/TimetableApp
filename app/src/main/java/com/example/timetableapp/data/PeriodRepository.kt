package com.example.timetableapp.data

import androidx.lifecycle.LiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PeriodRepository(private val periodDao: PeriodDao) {

    val allPeriods: LiveData<List<Period>> = periodDao.getAllPeriods()

    fun getPeriodsByDay(dayOfWeek: Int): LiveData<List<Period>> {
        return periodDao.getPeriodsByDay(dayOfWeek)
    }

    suspend fun insert(period: Period): Long {
        return withContext(Dispatchers.IO) {
            periodDao.insert(period)
        }
    }

    suspend fun update(period: Period) {
        withContext(Dispatchers.IO) {
            periodDao.update(period)
        }
    }

    suspend fun delete(period: Period) {
        withContext(Dispatchers.IO) {
            periodDao.delete(period)
        }
    }

    suspend fun deleteAll() {
        withContext(Dispatchers.IO) {
            periodDao.deleteAll()
        }
    }
}
