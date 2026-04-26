package com.lillesand.badetemp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lillesand.badetemp.data.TemperatureSnapshot
import com.lillesand.badetemp.ui.theme.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val HISTORY_ZONE: ZoneId = ZoneId.of("Europe/Oslo")
private val TIME_FMT = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun HistoryContent(history: List<TemperatureSnapshot>, padding: PaddingValues) {
    if (history.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.History,
                    contentDescription = null,
                    tint = LillesandBlueLight,
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    text = "Ingen historikk enn\u00e5",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Temperaturer lagres automatisk\nn\u00e5r appen henter nye data.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    val grouped = history
        .groupBy { snap ->
            Instant.ofEpochMilli(snap.epochMs).atZone(HISTORY_ZONE).toLocalDate()
        }
        .entries
        .sortedByDescending { it.key }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = padding.calculateTopPadding() + 12.dp,
            bottom = padding.calculateBottomPadding() + 12.dp
        ),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        grouped.forEach { (date, snapshots) ->
            item(key = "header_${date}") {
                Text(
                    text = formatHistoryDate(date),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                )
            }
            items(snapshots, key = { it.epochMs }) { snap ->
                HistorySnapshotCard(snap)
            }
        }
    }
}

@Composable
fun HistorySnapshotCard(snap: TemperatureSnapshot) {
    val time = Instant.ofEpochMilli(snap.epochMs)
        .atZone(HISTORY_ZONE)
        .format(TIME_FMT)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Tidspunkt
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = time,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Temperaturer per lokasjon
            Column(
                verticalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.weight(1f)
            ) {
                snap.locations
                    .filter { loc -> EXCLUDED_LOCATIONS.none { loc.name.lowercase().contains(it) } }
                    .forEach { loc ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = loc.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        if (loc.temperature != null) {
                            val color = temperatureColor(loc.temperature)
                            Text(
                                text = "%.1f\u00b0C".format(loc.temperature),
                                style = MaterialTheme.typography.labelMedium,
                                color = color,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                text = "\u2013",
                                style = MaterialTheme.typography.labelMedium,
                                color = LillesandTextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatHistoryDate(date: LocalDate): String {
    val today = LocalDate.now(HISTORY_ZONE)
    return when (date) {
        today -> "I dag"
        today.minusDays(1) -> "I g\u00e5r"
        else -> date.format(DateTimeFormatter.ofPattern("d. MMMM yyyy", Locale("no")))
    }
}
