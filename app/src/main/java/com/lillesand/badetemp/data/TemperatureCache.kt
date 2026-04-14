package com.lillesand.badetemp.data

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

private val OSLO_ZONE = ZoneId.of("Europe/Oslo")
private val UPDATE_HOURS = listOf(2, 6, 10, 14, 18, 22)
private const val MAX_HISTORY = 126   // ~3 uker ved 4t-intervaller
private const val KEY_CACHE = "cache_v1"
private const val KEY_HISTORY = "history_v1"

class TemperatureCache(private val prefs: SharedPreferences) {

    /**
     * Returnerer cachet data hvis det ble hentet i den nåværende 4-timers sloten.
     * Null betyr at vi må hente fra nettverket.
     */
    fun getIfCurrentSlot(): List<BathingLocation>? {
        val json = prefs.getString(KEY_CACHE, null) ?: return null
        return try {
            val obj = JSONObject(json)
            val epochMs = obj.getLong("epochMs")
            if (!isInCurrentSlot(epochMs)) return null
            parseLocations(obj.getJSONArray("locations"))
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Lagrer et nytt snapshot. Legger til i historikk kun hvis temperaturene har endret seg.
     */
    fun save(epochMs: Long, slotLabel: String, locations: List<BathingLocation>) {
        val cacheObj = JSONObject().apply {
            put("epochMs", epochMs)
            put("locations", locationsToJson(locations))
        }
        prefs.edit().putString(KEY_CACHE, cacheObj.toString()).apply()

        val history = loadHistory().toMutableList()
        if (history.isEmpty() || hasTempsChanged(history.first().locations, locations)) {
            history.add(0, TemperatureSnapshot(epochMs, slotLabel, locations))
            if (history.size > MAX_HISTORY) history.subList(MAX_HISTORY, history.size).clear()
            prefs.edit().putString(KEY_HISTORY, historyToJson(history)).apply()
        }
    }

    fun loadHistory(): List<TemperatureSnapshot> {
        val json = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                TemperatureSnapshot(
                    epochMs = obj.getLong("epochMs"),
                    slotLabel = obj.getString("slotLabel"),
                    locations = parseLocations(obj.getJSONArray("locations"))
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun isInCurrentSlot(epochMs: Long): Boolean {
        val now = LocalDateTime.now(OSLO_ZONE)
        val today = now.toLocalDate()
        val slots = (-1..0).flatMap { offset ->
            val date = today.plusDays(offset.toLong())
            UPDATE_HOURS.map { h -> LocalDateTime.of(date, LocalTime.of(h, 0)) }
        }.sorted()
        val slotStart = slots.lastOrNull { it <= now } ?: return false
        val slotStartMs = slotStart.atZone(OSLO_ZONE).toInstant().toEpochMilli()
        return epochMs >= slotStartMs
    }

    private fun hasTempsChanged(old: List<BathingLocation>, new: List<BathingLocation>): Boolean {
        if (old.size != new.size) return true
        val oldMap = old.associateBy { it.name }
        return new.any { loc -> oldMap[loc.name]?.temperature != loc.temperature }
    }

    private fun locationsToJson(locations: List<BathingLocation>): JSONArray =
        JSONArray().also { arr ->
            locations.forEach { loc ->
                arr.put(JSONObject().apply {
                    put("name", loc.name)
                    if (loc.temperature != null) put("temperature", loc.temperature)
                    else put("temperature", JSONObject.NULL)
                })
            }
        }

    private fun parseLocations(arr: JSONArray): List<BathingLocation> =
        (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            BathingLocation(
                name = obj.getString("name"),
                temperature = if (obj.isNull("temperature")) null else obj.getDouble("temperature")
            )
        }

    private fun historyToJson(snapshots: List<TemperatureSnapshot>): String =
        JSONArray().also { arr ->
            snapshots.forEach { snap ->
                arr.put(JSONObject().apply {
                    put("epochMs", snap.epochMs)
                    put("slotLabel", snap.slotLabel)
                    put("locations", locationsToJson(snap.locations))
                })
            }
        }.toString()
}
