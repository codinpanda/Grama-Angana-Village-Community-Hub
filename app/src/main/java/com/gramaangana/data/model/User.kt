package com.gramaangana.data.model

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "VILLAGER" // "VILLAGER" or "PANCHAYAT"
)
