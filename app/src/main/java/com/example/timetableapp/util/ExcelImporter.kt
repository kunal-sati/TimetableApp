package com.example.timetableapp.util

import android.util.Log
import com.example.timetableapp.data.Period
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.InputStream

class ExcelImporter {
    companion object {
        fun importFromExcel(inputStream: InputStream): List<Period> {
            val periods = mutableListOf<Period>()

            try {
                val workbook = WorkbookFactory.create(inputStream)
                val sheet = workbook.getSheetAt(0)

                // Get header row
                val headerRow = sheet.getRow(0)
                val headerMap = mutableMapOf<String, Int>()

                // Map column names to indices
                for (i in 0 until headerRow.lastCellNum) {
                    val cell = headerRow.getCell(i)
                    if (cell != null) {
                        headerMap[cell.stringCellValue] = i
                    }
                }

                Log.d("ExcelImporter", "Found headers: $headerMap")

                // Get indices for each column
                val subjectIdx = headerMap["Subject"] ?: -1
                val startTimeIdx = headerMap["StartTime"] ?: -1
                val endTimeIdx = headerMap["EndTime"] ?: -1
                val locationIdx = headerMap["Location"] ?: -1
                val dayOfWeekIdx = headerMap["DayOfWeek"] ?: -1
                val notesIdx = headerMap["Notes"] ?: -1

                // Check if required columns exist
                if (subjectIdx == -1 || startTimeIdx == -1 || endTimeIdx == -1) {
                    Log.e("ExcelImporter", "Required columns missing. Found: Subject=$subjectIdx, StartTime=$startTimeIdx, EndTime=$endTimeIdx")
                    return periods // Return empty list if required columns are missing
                }

                // Process data rows
                for (i in 1..sheet.lastRowNum) {
                    val row = sheet.getRow(i) ?: continue

                    try {
                        val subject = row.getCell(subjectIdx)?.stringCellValue ?: ""
                        val startTime = row.getCell(startTimeIdx)?.stringCellValue ?: ""
                        val endTime = row.getCell(endTimeIdx)?.stringCellValue ?: ""
                        val location = if (locationIdx >= 0) row.getCell(locationIdx)?.stringCellValue ?: "" else ""
                        val dayOfWeek = if (dayOfWeekIdx >= 0) {
                            val cell = row.getCell(dayOfWeekIdx)
                            when (cell?.cellType) {
                                CellType.NUMERIC -> cell.numericCellValue.toInt()
                                CellType.STRING -> cell.stringCellValue.toIntOrNull() ?: 1
                                else -> 1
                            }
                        } else 1
                        val notes = if (notesIdx >= 0) row.getCell(notesIdx)?.stringCellValue ?: "" else ""

                        if (subject.isNotBlank() && startTime.isNotBlank() && endTime.isNotBlank()) {
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
                        }
                    } catch (e: Exception) {
                        Log.e("ExcelImporter", "Error processing row $i: ${e.message}")
                        // Skip malformed rows
                        continue
                    }
                }

                Log.d("ExcelImporter", "Imported ${periods.size} periods from Excel")
                workbook.close()
            } catch (e: Exception) {
                Log.e("ExcelImporter", "Error importing Excel file: ${e.message}")
                e.printStackTrace()
            }

            return periods
        }
    }
}
