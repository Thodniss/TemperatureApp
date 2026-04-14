package com.lillesand.badetemp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lillesand.badetemp.data.BathingLocation
import com.lillesand.badetemp.ui.theme.*
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import kotlin.math.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BathingApp()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BathingApp(vm: BathingViewModel = viewModel()) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    val darkMode by vm.darkMode.collectAsStateWithLifecycle()
    val showDistance by vm.showDistance.collectAsStateWithLifecycle()
    val userLocation by vm.userLocation.collectAsStateWithLifecycle()
    val isRefreshing = state is UiState.Loading
    var selectedTab by remember { mutableIntStateOf(0) }
    var showSettings by remember { mutableStateOf(false) }

    val context = LocalContext.current

    fun fetchLocation() {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        try {
            val loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            if (loc != null) vm.updateUserLocation(loc.latitude, loc.longitude)
        } catch (_: SecurityException) {}
    }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) fetchLocation()
        else vm.setShowDistance(false)
    }

    // Hent plassering ved oppstart hvis tillatelse allerede er gitt og avstand er aktivert
    LaunchedEffect(showDistance) {
        if (showDistance && ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fetchLocation()
        }
    }

    LillesandBadetempTheme(darkTheme = darkMode) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                BathingTopBar(
                    onRefresh = vm::refresh,
                    onSettings = { showSettings = true }
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = {
                            Icon(
                                if (selectedTab == 0) Icons.Filled.WaterDrop else Icons.Outlined.WaterDrop,
                                contentDescription = null
                            )
                        },
                        label = { Text("N\u00e5") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = LillesandBlue,
                            selectedTextColor = LillesandBlue,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Outlined.History, contentDescription = null) },
                        label = { Text("Historikk") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = LillesandBlue,
                            selectedTextColor = LillesandBlue,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            when (selectedTab) {
                0 -> PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = vm::refresh,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    AnimatedContent(
                        targetState = state,
                        transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                        label = "state_transition"
                    ) { currentState ->
                        when (currentState) {
                            is UiState.Loading -> LoadingScreen()
                            is UiState.Success -> SuccessScreen(
                                locations = currentState.locations,
                                fetchedAt = currentState.fetchedAt,
                                schedule = currentState.schedule,
                                sortByTemp = currentState.sortByTemp,
                                userLocation = if (showDistance) userLocation else null,
                                onSyncOrder = vm::syncOrder,
                                onToggleSort = vm::toggleSort
                            )
                            is UiState.Error -> ErrorScreen(
                                message = currentState.message,
                                onRetry = vm::refresh
                            )
                        }
                    }
                }
                1 -> HistoryContent(history = history, padding = padding)
            }
        }

        if (showSettings) {
            SettingsBottomSheet(
                darkMode = darkMode,
                onToggleDarkMode = vm::toggleDarkMode,
                showDistance = showDistance,
                onToggleDistance = {
                    if (!showDistance) {
                        // Skrur på: sjekk/be om tillatelse
                        vm.setShowDistance(true)
                        if (ContextCompat.checkSelfPermission(
                                context, Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            fetchLocation()
                        } else {
                            locationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                    } else {
                        vm.setShowDistance(false)
                    }
                },
                onDismiss = { showSettings = false }
            )
        }
    }
}

@Composable
fun BathingTopBar(onRefresh: () -> Unit, onSettings: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(colors = listOf(LillesandBlueDark, LillesandBlue)))
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Badetemperaturer",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge.copy(
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.2f),
                            offset = Offset(0f, 1f),
                            blurRadius = 3f
                        )
                    )
                )
                Text(
                    text = "Lillesand Kommune",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal
                )
            }
            Row {
                IconButton(
                    onClick = onRefresh,
                    colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.refresh), tint = Color.White)
                }
                IconButton(
                    onClick = onSettings,
                    colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Innstillinger", tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(color = LillesandBlue, strokeWidth = 3.dp, modifier = Modifier.size(48.dp))
            Text(
                text = stringResource(R.string.loading),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ErrorScreen(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(Icons.Filled.WaterDrop, contentDescription = null, tint = LillesandBlueLight, modifier = Modifier.size(64.dp))
            Text(text = stringResource(R.string.error_loading), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
            Text(text = message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = LillesandBlue)) {
                Text(stringResource(R.string.retry), color = Color.White)
            }
        }
    }
}

@Composable
fun SuccessScreen(
    locations: List<BathingLocation>,
    fetchedAt: String,
    schedule: UpdateSchedule,
    sortByTemp: Boolean,
    userLocation: Pair<Double, Double>?,
    onSyncOrder: (List<String>) -> Unit,
    onToggleSort: () -> Unit
) {
    // Lokal liste for umiddelbare drag-oppdateringer uten å gå via ViewModel
    val localLocations = remember { mutableStateListOf(*locations.toTypedArray()) }
    var dragInProgress by remember { mutableStateOf(false) }

    // Synk fra ViewModel når listen endres utenfra (refresh, sortering), men ikke under drag
    LaunchedEffect(locations) {
        if (!dragInProgress) {
            localLocations.clear()
            localLocations.addAll(locations)
        }
    }

    var selectedLocation by remember { mutableStateOf<BathingLocation?>(null) }

    val lazyListState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
        val fromIdx = localLocations.indexOfFirst { it.name == from.key }
        val toIdx = localLocations.indexOfFirst { it.name == to.key }
        if (fromIdx >= 0 && toIdx >= 0) {
            dragInProgress = true
            localLocations.add(toIdx, localLocations.removeAt(fromIdx))
        }
    }

    // Synk endelig rekkefølge til ViewModel når drag er ferdig
    LaunchedEffect(reorderState.isAnyItemDragging) {
        if (!reorderState.isAnyItemDragging && dragInProgress) {
            dragInProgress = false
            onSyncOrder(localLocations.map { it.name })
        }
    }

    val withTemp = localLocations.filter { it.temperature != null }
    val hottestName = withTemp.maxByOrNull { it.temperature!! }?.takeIf { withTemp.size > 1 }?.name
    val coldestName = withTemp.minByOrNull { it.temperature!! }?.takeIf { withTemp.size > 1 }?.name
    val avgTemp = withTemp.map { it.temperature!! }.takeIf { it.isNotEmpty() }?.average()

    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "__header__") {
            HeroHeader(
                locationCount = localLocations.size,
                fetchedAt = fetchedAt,
                schedule = schedule,
                averageTemp = avgTemp,
                sortByTemp = sortByTemp,
                onToggleSort = onToggleSort
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        items(localLocations, key = { it.name }) { location ->
            ReorderableItem(reorderState, key = location.name) { isDragging ->
                val distText = if (userLocation != null && location.lat != null && location.lng != null) {
                    formatDistance(distanceMeters(userLocation.first, userLocation.second, location.lat, location.lng))
                } else null

                BathingLocationCard(
                    location = location,
                    isDragging = isDragging,
                    isHottest = location.name == hottestName,
                    isColdest = location.name == coldestName,
                    dragEnabled = !sortByTemp,
                    distanceText = distText,
                    dragHandleModifier = if (sortByTemp) Modifier else Modifier.draggableHandle(),
                    onClick = { if (!isDragging) selectedLocation = location }
                )
            }
        }

        item(key = "__footer__") {
            Spacer(modifier = Modifier.height(8.dp))
            SourceNotice()
        }
    }

    selectedLocation?.let { location ->
        LocationDetailSheet(
            location = location,
            onDismiss = { selectedLocation = null }
        )
    }
}

@Composable
fun HeroHeader(
    locationCount: Int,
    fetchedAt: String,
    schedule: UpdateSchedule,
    averageTemp: Double?,
    sortByTemp: Boolean,
    onToggleSort: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LillesandBlue),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(LillesandBlueDark, LillesandOcean),
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.WaterDrop, contentDescription = null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$locationCount badeplasser overvåkes",
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.8.sp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = "Badetemperaturer", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Lillesand – den hvite by ved Skagerrak",
                        color = Color.White.copy(alpha = 0.75f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (averageTemp != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.15f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "%.1f°C".format(averageTemp), color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(text = "snitt", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.25f))
                Spacer(modifier = Modifier.height(10.dp))
                UpdateScheduleInfo(fetchedAt = fetchedAt, schedule = schedule)
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Sortering:", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SortChip(label = "Egendefinert", selected = !sortByTemp, onClick = { if (sortByTemp) onToggleSort() })
                        SortChip(label = "Varmest f\u00f8rst", selected = sortByTemp, onClick = { if (!sortByTemp) onToggleSort() })
                    }
                }
            }
        }
    }
}

@Composable
fun SortChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) Color.White.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f))
            .then(if (!selected) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else Color.White.copy(alpha = 0.6f),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
fun UpdateScheduleInfo(fetchedAt: String, schedule: UpdateSchedule) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = "Kilde oppdatert: ${schedule.lastUpdate}", color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.labelMedium)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(4.dp))
            val nextLabel = if (schedule.minutesUntilNext < 60) {
                "om ${schedule.minutesUntilNext} min"
            } else {
                val h = schedule.minutesUntilNext / 60
                val m = schedule.minutesUntilNext % 60
                if (m == 0L) "om ${h}t" else "om ${h}t ${m}m"
            }
            Text(text = "Neste oppdatering: ${schedule.nextUpdate} ($nextLabel)", color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.labelMedium)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(modifier = Modifier.width(17.dp))
            Text(text = "Hentet av appen: $fetchedAt", color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun BathingLocationCard(
    location: BathingLocation,
    isDragging: Boolean,
    isHottest: Boolean = false,
    isColdest: Boolean = false,
    dragEnabled: Boolean = true,
    distanceText: String? = null,
    dragHandleModifier: Modifier,
    onClick: () -> Unit = {}
) {
    val tempColor = location.temperature?.let { temperatureColor(it) } ?: LillesandBlueLight
    val tempLabel = location.temperature?.let { temperatureLabel(it, location.name) } ?: "—"
    val elevation = if (isDragging) 10.dp else 2.dp

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDragging) MaterialTheme.colorScheme.surfaceVariant
                             else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, top = 12.dp, end = 16.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Drag handle
            IconButton(onClick = {}, modifier = dragHandleModifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = "Dra for å endre rekkefølge",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (dragEnabled) 0.5f else 0.15f),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Temperature icon circle
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(tempColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier.size(34.dp).clip(CircleShape).background(tempColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.WaterDrop, contentDescription = null, tint = tempColor, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Location name + phrase
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = location.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (isHottest) {
                        Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(TempHot.copy(alpha = 0.12f)).padding(horizontal = 5.dp, vertical = 2.dp)) {
                            Text(text = "Varmest", style = MaterialTheme.typography.labelSmall, color = TempHot, fontWeight = FontWeight.Bold)
                        }
                    } else if (isColdest) {
                        Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(TempVeryCold.copy(alpha = 0.12f)).padding(horizontal = 5.dp, vertical = 2.dp)) {
                            Text(text = "Kaldest", style = MaterialTheme.typography.labelSmall, color = TempVeryCold, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = tempLabel, style = MaterialTheme.typography.labelMedium, color = tempColor, fontWeight = FontWeight.Medium)
                if (distanceText != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            Icons.Outlined.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = distanceText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Temperature badge
            if (location.temperature != null) {
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(tempColor).padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "%.1f°C".format(location.temperature), color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            } else {
                Text(text = "–", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun SourceNotice() {
    Text(
        text = "Data hentet fra Lillesand Kommune · lillesand.kommune.no",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
    )
}

private fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val R = 6_371_000.0
    val phi1 = Math.toRadians(lat1)
    val phi2 = Math.toRadians(lat2)
    val dPhi = Math.toRadians(lat2 - lat1)
    val dLam = Math.toRadians(lon2 - lon1)
    val a = sin(dPhi / 2).pow(2) + cos(phi1) * cos(phi2) * sin(dLam / 2).pow(2)
    return R * 2 * atan2(sqrt(a), sqrt(1 - a))
}

private fun formatDistance(meters: Double): String = when {
    meters < 1000 -> "${meters.toInt()} m"
    else -> "%.1f km".format(meters / 1000)
}

/** Picks a consistent phrase per location name so the same spot always gets the same phrase. */
fun temperatureLabel(temp: Double, locationName: String): String {
    val phrases = when {
        temp < 8.0 -> listOf(
            "Jysla kaldt!",
            "Sabla kaldt!",
            "Spiggent!",
            "Fryser a meg te-ane!",
            "Jæklig kaldt – ikkje for pyser!",
            "Grusomt kaldt dette!",
            "Ikkje for alle!"
        )
        temp < 14.0 -> listOf(
            "Ikkje optimalt...",
            "Bedre i båden!",
            "Ikkje heilt smeig",
            "Litt spiggent, men duganes",
            "Kaldt, men ikkje spiggent",
            "Treng litt mot!",
            "Sprek-bading dette!"
        )
        temp < 18.0 -> listOf(
            "Bedre med ei bryggesleng!",
            "Ikkje heilt smeig ennå",
            "Godt for faula!",
            "Duganes badetemperatur",
            "Kan gå an!",
            "Nærmar seg smeig!",
            "Hopp i – du overlever!"
        )
        temp < 22.0 -> listOf(
            "Sabla nyd!",
            "Jysla smeig!",
            "Ikkje naudent å vente lenger!",
            "Godt og smeigt!",
            "No e det bare å hoppe i!",
            "Klart for bryggesleng!",
            "Dette e nyd det!"
        )
        else -> listOf(
            "E kovner!",
            "Jysla varmt – nyt det!",
            "Sabla nyd, dette her!",
            "Nå edde heilt Texas!",
            "Badebasseng-temperatur!",
            "Smeig som fy!",
            "E smeltår nesten!"
        )
    }
    val index = Math.abs(locationName.hashCode()) % phrases.size
    return phrases[index]
}
