package com.example.myapplication

data class HealthData(
    val heart_rate: Int,
    val spo2: Int,
    val status: String,
    val confidence: Double,
    val ecg: List<Float> = emptyList()
)