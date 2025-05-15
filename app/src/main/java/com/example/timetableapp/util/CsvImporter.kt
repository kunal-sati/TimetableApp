package com.example.timetableapp.util

import android.content.Context
import com.example.timetableapp.data.Period
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import java.io.InputStream

class CsvImporter {
    companion object {
        fun importFromCsv(inputStream: InputStream): List<Period> {
            val periods = mutableListOf<Period>()

            try {
                val csvParser = CSVParser(
                    inputStream.reader(),
                    CSVFormat.DEFAULT
                        .withFirstRecordAsHeader()
                        .withIgnoreSurroundingSpaces()
                )

                for (record in csvParser) {
                    try {
                        val subject = record.get("Subject")
                        val startTime = record.get("StartTime")
                        val endTime = record.get("EndTime")
                        val location = record.get("Location")
                        val dayOfWeek = record.get("DayOfWeek").toIntOrNull() ?: 1
                        val notes = record.get("Notes") ?: ""

                        periods.add(
                            Period(
                                subject = subject,
                                startTime = startTime,
                                endTime = endTime,
                                location = location,
                                dayOfWeek = dayOfWeek,
                                notes = notes
                            )
                        )
                    } catch (e: Exception) {
                        // Skip malformed records
                        continue
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return periods
        }
    }
}
