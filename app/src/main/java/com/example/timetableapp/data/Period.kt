package com.example.timetableapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "periods")
data class Period(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subject: String,
    val startTime: String,
    val endTime: String,
    val location: String,
    val dayOfWeek: Int, // 1 = Monday, 2 = Tuesday, etc.
    val notes: String = ""
) : Serializable
