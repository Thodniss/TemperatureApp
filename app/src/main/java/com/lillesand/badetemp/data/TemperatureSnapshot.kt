package com.lillesand.badetemp.data

data class TemperatureSnapshot(
    val epochMs: Long,
    val slotLabel: String,
    val locations: List<BathingLocation>
)
