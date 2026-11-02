package com.example.taskmanager.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

data class TaskLocation(
    val latitude: Double,
    val longitude: Double,
    val addressName: String
)

class LocationConverter {
    @TypeConverter
    fun fromLocationList(locations: List<TaskLocation>?): String { //zamienia liste lokalizacji na jeden napis
        if (locations.isNullOrEmpty()) return ""
        // "lat,lon,nazwa|lat,lon,nazwa"
        return locations.joinToString(separator = "|") { "${it.latitude},${it.longitude},${it.addressName}" }
    }

    @TypeConverter
    fun toLocationList(data: String?): List<TaskLocation> { //na odwrot
        if (data.isNullOrBlank()) return emptyList()
        return data.split("|").mapNotNull {
            val parts = it.split(",")
            if (parts.size >= 3) {
                TaskLocation(
                    latitude = parts[0].toDoubleOrNull() ?: 0.0,
                    longitude = parts[1].toDoubleOrNull() ?: 0.0,
                    addressName = parts[2]
                )
            } else null
        }
    }
}

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val createdAt: Long,
    val executionTime: Long? = null,
    val status: String,
    val timeNotification: Long? = null,
    val locationNotification: Boolean = false,
    val category: String,
    val priority: String,
    val locations: List<TaskLocation> = emptyList(), // Lista lokalizacji
    val attachmentsPaths: String, //zalaczniki
    val isCyclic: Boolean,
    val cycleType: String? = null
)