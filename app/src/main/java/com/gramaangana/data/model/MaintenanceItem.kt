package com.gramaangana.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "maintenance_items")
data class MaintenanceItem(
    @PrimaryKey val id: Int = 0,
    val itemName: String = "",
    val description: String = "",
    val targetAmount: Double = 0.0,
    val collectedAmount: Double = 0.0,
    val status: String = "NEEDED"
)
