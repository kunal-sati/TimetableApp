package com.example.timetableapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Period::class], version = 1, exportSchema = false)
abstract class TimetableDatabase : RoomDatabase() {

    abstract fun periodDao(): PeriodDao

    companion object {
        @Volatile
        private var INSTANCE: TimetableDatabase? = null

        fun getDatabase(context: Context): TimetableDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TimetableDatabase::class.java,
                    "timetable_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
