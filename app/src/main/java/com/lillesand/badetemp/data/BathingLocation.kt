package com.lillesand.badetemp.data

data class BathingLocation(
    val name: String,
    val temperature: Double?,
    val lastUpdated: String? = null,
    val lat: Double? = null,
    val lng: Double? = null
)
