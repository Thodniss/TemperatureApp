package com.lillesand.badetemp

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lillesand.badetemp.data.BathingLocation
import com.lillesand.badetemp.data.BathingRepository
import com.lillesand.badetemp.data.TemperatureCache
import com.lillesand.badetemp.data.TemperatureSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

val OSLO_ZONE: ZoneId = ZoneId.of("Europe/Oslo")
private val UPDATE_HOURS = listOf(2, 6, 10, 14, 18, 22)

/** Lokasjoner som skal filtreres bort fra listen */
private val EXCLUDED_LOCATIONS = setOf("traudalsbekken")

/** Koordinater for kjente badeplasser i Lillesand */
private val LOCATION_COORDS = mapOf(
    "langedalstjønna" to Pair(58.259642, 8.404467),
    "kaldvell"        to Pair(58.270901, 8.415404),
    "springvannsheia" to Pair(58.247301, 8.373382),
    "guttebukta"      to Pair(58.244288, 8.380460),
    "orrehola"        to Pair(58.248615, 8.385632)
)

data class UpdateSchedule(
    val lastUpdate: String,
    val nextUpdate: String,
    val minutesUntilNext: Long
)

sealed class UiState {
    object Loading : UiState()
    data class Success(
        val locations: List<BathingLocation>,
        val fetchedAt: String,
        val schedule: UpdateSchedule,
        val sortByTemp: Boolean = false
    ) : UiState()
    data class Error(val message: String) : UiState()
}

class BathingViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BathingRepository()
    private val cache = TemperatureCache(
        application.getSharedPreferences("bathing_cache", Context.MODE_PRIVATE)
    )
    private val settingsPrefs = application.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _darkMode = MutableStateFlow(settingsPrefs.getBoolean("dark_mode", false))
    val darkMode: StateFlow<Boolean> = _darkMode

    fun toggleDarkMode() {
        val newValue = !_darkMode.value
        _darkMode.value = newValue
        settingsPrefs.edit().putBoolean("dark_mode", newValue).apply()
    }

    private val _showDistance = MutableStateFlow(settingsPrefs.getBoolean("show_distance", false))
    val showDistance: StateFlow<Boolean> = _showDistance

    fun setShowDistance(show: Boolean) {
        _showDistance.value = show
        settingsPrefs.edit().putBoolean("show_distance", show).apply()
        if (!show) _userLocation.value = null
    }

    private val _userLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    val userLocation: StateFlow<Pair<Double, Double>?> = _userLocation

    fun updateUserLocation(lat: Double, lng: Double) {
        _userLocation.value = Pair(lat, lng)
    }

    private var preferredOrder: List<String> = emptyList()
    private var rawLocations: List<BathingLocation> = emptyList()
    private var isSortedByTemp: Boolean = false

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState

    private val _history = MutableStateFlow<List<TemperatureSnapshot>>(emptyList())
    val history: StateFlow<List<TemperatureSnapshot>> = _history

    init {
        _history.value = cache.loadHistory()
        refresh()
    }

    fun refresh() {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            try {
                val now = LocalDateTime.now(OSLO_ZONE)

                val cached = withContext(Dispatchers.IO) { cache.getIfCurrentSlot() }
                val locations = if (cached != null) {
                    cached
                } else {
                    val fetched = withContext(Dispatchers.IO) { repository.fetchLocations() }
                    val epochMs = now.atZone(OSLO_ZONE).toInstant().toEpochMilli()
                    withContext(Dispatchers.IO) { cache.save(epochMs, formatTime(now), fetched) }
                    _history.value = cache.loadHistory()
                    fetched
                }

                rawLocations = enrichWithCoords(locations)
                _uiState.value = UiState.Success(
                    locations = orderedLocations(rawLocations),
                    fetchedAt = formatTime(now),
                    schedule = computeSchedule(now),
                    sortByTemp = isSortedByTemp
                )
            } catch (e: Exception) {
                _uiState.value = UiState.Error(message = e.message ?: "Ukjent feil")
            }
        }
    }

    /** Synkroniserer rekkefølge fra UI etter endt drag. */
    fun syncOrder(orderedNames: List<String>) {
        if (isSortedByTemp) return
        preferredOrder = orderedNames
        val map = rawLocations.associateBy { it.name }
        val ordered = orderedNames.mapNotNull { map[it] }
        val remaining = rawLocations.filter { it.name !in orderedNames }
        rawLocations = ordered + remaining
        val current = _uiState.value as? UiState.Success ?: return
        _uiState.value = current.copy(locations = rawLocations)
    }

    fun toggleSort() {
        isSortedByTemp = !isSortedByTemp
        val current = _uiState.value as? UiState.Success ?: return
        _uiState.value = current.copy(
            locations = orderedLocations(rawLocations),
            sortByTemp = isSortedByTemp
        )
    }

    private fun enrichWithCoords(locations: List<BathingLocation>): List<BathingLocation> =
        locations
            .filter { loc -> EXCLUDED_LOCATIONS.none { loc.name.lowercase().contains(it) } }
            .map { loc ->
                val coords = LOCATION_COORDS.entries
                    .firstOrNull { (key, _) -> loc.name.lowercase().contains(key) }
                    ?.value
                if (coords != null) loc.copy(lat = coords.first, lng = coords.second) else loc
            }

    private fun orderedLocations(locations: List<BathingLocation>): List<BathingLocation> =
        if (isSortedByTemp) locations.sortedByDescending { it.temperature ?: -99.0 }
        else applyPreferredOrder(locations)

    private fun applyPreferredOrder(locations: List<BathingLocation>): List<BathingLocation> {
        if (preferredOrder.isEmpty()) return locations
        val map = locations.associateBy { it.name }
        val ordered = preferredOrder.mapNotNull { map[it] }
        val new = locations.filter { it.name !in preferredOrder }
        return ordered + new
    }

    private fun computeSchedule(now: LocalDateTime): UpdateSchedule {
        val today = now.toLocalDate()
        val slots = (-1..1).flatMap { dayOffset ->
            val date = today.plusDays(dayOffset.toLong())
            UPDATE_HOURS.map { hour -> LocalDateTime.of(date, LocalTime.of(hour, 0)) }
        }.sorted()

        val lastUpdate = slots.lastOrNull { it <= now } ?: slots.first()
        val nextUpdate = slots.firstOrNull { it > now } ?: slots.last()
        val minutesUntilNext = java.time.Duration.between(now, nextUpdate).toMinutes()

        return UpdateSchedule(
            lastUpdate = formatSlot(lastUpdate),
            nextUpdate = formatSlot(nextUpdate),
            minutesUntilNext = minutesUntilNext
        )
    }

    private fun formatSlot(dt: LocalDateTime): String {
        val today = LocalDate.now(OSLO_ZONE)
        val dateLabel = when (dt.toLocalDate()) {
            today -> "i dag"
            today.plusDays(1) -> "i morgen"
            today.minusDays(1) -> "i g\u00e5r"
            else -> dt.format(DateTimeFormatter.ofPattern("d. MMMM", Locale("no")))
        }
        return "${dt.format(DateTimeFormatter.ofPattern("HH:mm"))}, $dateLabel"
    }

    private fun formatTime(dt: LocalDateTime): String =
        dt.format(DateTimeFormatter.ofPattern("HH:mm, d. MMMM", Locale("no")))
}
