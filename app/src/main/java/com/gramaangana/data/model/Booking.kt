package com.gramaangana.data.model

data class Booking(
    val id: String = "",
    val userId: String = "",
    val purpose: String = "",
    val date: String = "",
    val startTime: Int = 0,
    val endTime: Int = 0,
    val status: String = "PENDING"
)

data class DailySchedule(
    val date: String = "",
    val bookings: List<Booking> = emptyList()
)
