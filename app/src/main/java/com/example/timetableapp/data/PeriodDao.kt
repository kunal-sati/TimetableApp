package com.example.timetableapp.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface PeriodDao {
    @Query("SELECT * FROM periods ORDER BY dayOfWeek, startTime")
    fun getAllPeriods(): LiveData<List<Period>>

    @Query("SELECT * FROM periods WHERE dayOfWeek = :dayOfWeek ORDER BY startTime")
    fun getPeriodsByDay(dayOfWeek: Int): LiveData<List<Period>>

    @Insert
    suspend fun insert(period: Period): Long

    @Update
    suspend fun update(period: Period)

    @Delete
    suspend fun delete(period: Period)

    @Query("DELETE FROM periods")
    suspend fun deleteAll()
}
