package com.example.timetableapp.data

import androidx.lifecycle.LiveData
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PeriodDao {
    @Query("SELECT * FROM periods ORDER BY dayOfWeek, startTime")
    fun getAllPeriods(): LiveData<List<Period>>

    @Query("SELECT * FROM periods WHERE dayOfWeek = :dayOfWeek ORDER BY startTime")
    fun getPeriodsByDay(dayOfWeek: Int): LiveData<List<Period>>

    @Insert
    fun insert(period: Period): Long

    @Update
    fun update(period: Period)

    @Delete
    fun delete(period: Period)

    @Query("DELETE FROM periods")
    fun deleteAll()

    @Query("SELECT * FROM periods ORDER BY dayOfWeek, startTime")
    fun getAllPeriodsSync(): Flow<List<Period>>
}
