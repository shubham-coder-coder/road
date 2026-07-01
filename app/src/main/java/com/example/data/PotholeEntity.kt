package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "potholes")
data class PotholeEntity(
    @PrimaryKey val id: String,
    val latitude: Double,
    val longitude: Double,
    val widthCm: Double,
    val lengthCm: Double,
    val depthCm: Double,
    val areaSqM: Double,
    val volumeCuM: Double,
    val severity: String, // Low, Medium, High, Critical
    val timestamp: Long,
    val confidence: Float,
    val imageUri: String, // Local storage file path or placeholder URI
    val roadName: String,
    val city: String,
    val district: String,
    val state: String,
    val country: String,
    val vehicleSpeedKmh: Double,
    val travelDirection: Float, // heading degrees
    val isSynced: Boolean,
    val lastSeen: Long,
    val repairStatus: String // Pending, Scheduled, Repaired
)
