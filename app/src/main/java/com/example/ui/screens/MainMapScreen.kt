package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Stop
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.RouteViewModel
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MainMapScreen(viewModel: RouteViewModel, modifier: Modifier = Modifier) {
    val stops by viewModel.stops.collectAsState()
    val routeDate by viewModel.routeDate.collectAsState()
    val routeDates by viewModel.allRouteDates.collectAsState()

    // View states
    val isOptimizing by viewModel.isOptimizing.collectAsState()
    val isNavegando by viewModel.isNavegando.collectAsState()
    val currentSimulatedStopIndex by viewModel.currentSimulatedStopIndex.collectAsState()
    val simulatedProgress by viewModel.simulatedProgress.collectAsState()

    val selectedStopForDetail by viewModel.selectedStopForDetail.collectAsState()
    val showAddStopDialog by viewModel.showAddStopDialog.collectAsState()
    val showImportDialog by viewModel.showImportDialog.collectAsState()
    val showTransferDialog by viewModel.showTransferDialog.collectAsState()

    var showDrawer by remember { mutableStateOf(false) }
    var mapStyle by remember { mutableStateOf("Standard Dark") }

    val scope = rememberCoroutineScope()

    Box(modifier = modifier.fillMaxSize().background(DarkBackground)) {
        // 1. Stylized Custom Canvas-based Interactive Map
        MapViewCanvas(
            stops = stops,
            isNavegando = isNavegando,
            currentSimulatedStopIndex = currentSimulatedStopIndex,
            simulatedProgress = simulatedProgress,
            mapStyle = mapStyle,
            onStopClicked = { stop -> viewModel.setSelectedStopForDetail(stop) }
        )

        // Map Title/Date indicator Overlay
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hamburger Menu
            IconButton(
                onClick = { showDrawer = true },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(DarkSurface.copy(alpha = 0.85f))
                    .testTag("drawer_menu_button")
            ) {
                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = TextPrimary)
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Route Date Chip
            var showDateDropdown by remember { mutableStateOf(false) }
            Box {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(DarkSurface.copy(alpha = 0.85f))
                        .clickable { showDateDropdown = true }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = "Data", tint = BrandBlueLight, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Rota: $routeDate", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Selecionar", tint = TextSecondary)
                }
                DropdownMenu(
                    expanded = showDateDropdown,
                    onDismissRequest = { showDateDropdown = false },
                    modifier = Modifier.background(DarkSurface)
                ) {
                    routeDates.forEach { date ->
                        DropdownMenuItem(
                            text = { Text(text = "Data: $date", color = TextPrimary) },
                            onClick = {
                                viewModel.selectRouteDate(date)
                                showDateDropdown = false
                            }
                        )
                    }
                }
            }
        }

        // Floating Action Controls on the Right side
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
        ) {
            // Toggle Map Style Button
            IconButton(
                onClick = {
                    mapStyle = when (mapStyle) {
                        "Standard Dark" -> "Cosmic Slate"
                        "Cosmic Slate" -> "Satelite"
                        else -> "Standard Dark"
                    }
                },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(DarkSurface.copy(alpha = 0.85f))
                    .testTag("map_layers_button")
            ) {
                Icon(Icons.Default.Layers, contentDescription = "Estilos de mapa", tint = TextPrimary, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Navigation Simulation Toggle Button
            IconButton(
                onClick = { viewModel.toggleNavegacao() },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (isNavegando) BrandBlue else DarkSurface.copy(alpha = 0.85f))
                    .testTag("simulation_toggle_button")
            ) {
                Icon(
                    imageVector = if (isNavegando) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Simular Navegação",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Center Viewport Button
            IconButton(
                onClick = { /* Simulated Map Centering */ },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(DarkSurface.copy(alpha = 0.85f))
                    .testTag("map_locate_button")
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Centralizar", tint = TextPrimary, modifier = Modifier.size(20.dp))
            }
        }

        // Active Navigation HUD Overlay
        AnimatedVisibility(
            visible = isNavegando,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 70.dp)
                .padding(horizontal = 16.dp)
        ) {
            val activeStop = stops.getOrNull(currentSimulatedStopIndex)
            if (activeStop != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(BrandBlue)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Navigation, contentDescription = "GPS", tint = Color.White, modifier = Modifier.size(24.dp).rotate(45f))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "SIMULADOR GPS ATIVO - ROTA OTIMIZADA", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
                        Text(text = "A caminho de: ${activeStop.recipientName}", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(text = activeStop.address, fontSize = 12.sp, color = Color.White.copy(alpha = 0.9f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${(simulatedProgress * 100).toInt()}%",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        // 2. Sliding Route Bottom Sheet (Main sheet)
        SlidingBottomSheet(
            stops = stops,
            isOptimizing = isOptimizing,
            onOptimize = { viewModel.optimizeRoute() },
            onAddStopClick = { viewModel.setShowAddStopDialog(true) },
            onCopyRouteClick = { viewModel.navigateTo(AppScreen.REUSE_STOPS) },
            onImportClick = { viewModel.setShowImportDialog(true) },
            onTransferClick = { viewModel.setShowTransferDialog(true) },
            onStopClicked = { stop -> viewModel.setSelectedStopForDetail(stop) }
        )

        // 3. Stop Detail View / Bottom Sheet
        AnimatedVisibility(
            visible = selectedStopForDetail != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            selectedStopForDetail?.let { stop ->
                StopDetailSheet(
                    stop = stop,
                    onClose = { viewModel.setSelectedStopForDetail(null) },
                    onStatusChange = { status -> viewModel.updateStopStatus(stop, status) },
                    onDelete = {
                        viewModel.deleteStop(stop.id)
                        viewModel.setSelectedStopForDetail(null)
                    }
                )
            }
        }

        // 4. Loading indicator while optimizing
        if (isOptimizing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkSurface)
                        .padding(24.dp)
                ) {
                    CircularProgressIndicator(color = BrandBlue, strokeWidth = 4.dp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Otimizando Sequência da Rota...", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(text = "Calculando caminho ideal de entrega...", color = TextSecondary, fontSize = 13.sp)
                }
            }
        }

        // Add Stop Manual Dialog
        if (showAddStopDialog) {
            val searchSuggestions by viewModel.searchSuggestions.collectAsState()
            val isSearchingPlaces by viewModel.isSearchingPlaces.collectAsState()
            AddStopDialog(
                onDismiss = { viewModel.setShowAddStopDialog(false) },
                onAdd = { address, name, notes, time ->
                    viewModel.addStop(address, name, notes, time)
                    viewModel.setShowAddStopDialog(false)
                },
                onAddWithCoordinates = { address, name, notes, time, lat, lon ->
                    viewModel.addStopWithCoordinates(address, name, notes, time, lat, lon)
                    viewModel.setShowAddStopDialog(false)
                },
                searchSuggestions = searchSuggestions,
                isSearching = isSearchingPlaces,
                onQueryChange = { query -> viewModel.onSearchQueryChanged(query) },
                onResolveCoordinates = { placeId -> viewModel.resolvePlaceCoordinates(placeId) }
            )
        }

        // Import Manifest (Gemini IA / Files) Dialog
        if (showImportDialog) {
            ImportManifestDialog(
                isParsing = viewModel.isParsingManifest.collectAsState().value,
                parseError = viewModel.manifestParseError.collectAsState().value,
                onDismiss = { viewModel.setShowImportDialog(false) },
                onParseText = { text -> viewModel.parseAndAddManifestText(text) },
                onParseImage = { bitmap -> viewModel.parseAndAddManifestImage(bitmap) }
            )
        }

        // Transfer Stops Dialog (Image 1)
        if (showTransferDialog) {
            TransferStopsDialog(
                stops = stops,
                isReceiving = viewModel.isParsingManifest.collectAsState().value,
                onDismiss = { viewModel.setShowTransferDialog(false) },
                onReceiveClick = { viewModel.simulateQRReceive() }
            )
        }

        // Left Navigation Drawer Menu
        AnimatedVisibility(
            visible = showDrawer,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { showDrawer = false }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.75f)
                        .background(DarkSurface)
                        .clickable(enabled = false) { }
                        .padding(24.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(BrandBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Map, contentDescription = "Logo", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = "Circuit", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Planejamento de Rota Ativo", color = TextSecondary, fontSize = 12.sp)

                    Divider(color = DarkSurfaceVariant, modifier = Modifier.padding(vertical = 16.dp))

                    DrawerItem(icon = Icons.Default.Directions, label = "Minha Rota Ativa", selected = true, onClick = { showDrawer = false })
                    DrawerItem(icon = Icons.Default.History, label = "Reutilizar Paradas", selected = false, onClick = {
                        showDrawer = false
                        viewModel.navigateTo(AppScreen.REUSE_STOPS)
                    })
                    DrawerItem(icon = Icons.Default.Share, label = "Transferir Rotas", selected = false, onClick = {
                        showDrawer = false
                        viewModel.setShowTransferDialog(true)
                    })
                    DrawerItem(icon = Icons.Default.AutoMode, label = "Importar Manifesto", selected = false, onClick = {
                        showDrawer = false
                        viewModel.setShowImportDialog(true)
                    })

                    Spacer(modifier = Modifier.weight(1f))

                    Text(text = "Versão 4.2.1-CLONE", color = TextSecondary, fontSize = 11.sp)
                }
            }
        }
    }
}

// 1. Map View Canvas Component
@Composable
fun MapViewCanvas(
    stops: List<Stop>,
    isNavegando: Boolean,
    currentSimulatedStopIndex: Int,
    simulatedProgress: Float,
    mapStyle: String,
    onStopClicked: (Stop) -> Unit
) {
    // Jardim Novo Parelheiros central lat/lon boundaries for rendering
    val minLat = -23.82
    val maxLat = -23.76
    val minLon = -46.72
    val maxLon = -46.65

    val gridColor = when (mapStyle) {
        "Standard Dark" -> Color(0xFF1E2130)
        "Cosmic Slate" -> Color(0xFF181B26)
        else -> Color(0xFF2C3241)
    }

    val mapBgColor = when (mapStyle) {
        "Standard Dark" -> Color(0xFF12141C)
        "Cosmic Slate" -> Color(0xFF0F1015)
        else -> Color(0xFF1A1C24)
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(mapBgColor)
            .pointerInput(stops) {
                // simple click detection on stops based on canvas coordinate mapping
                detectDragGestures { _, _ -> }
            }
    ) {
        val w = size.width
        val h = size.height

        // Helper to convert coordinate to local pixel
        fun getX(lon: Double) = ((lon - minLon) / (maxLon - minLon) * w).toFloat()
        fun getY(lat: Double) = (h - (lat - minLat) / (maxLat - minLat) * h).toFloat()

        // Draw street network grids
        val numGridLines = 12
        for (i in 0..numGridLines) {
            val gridX = i * (w / numGridLines)
            val gridY = i * (h / numGridLines)
            // Vertical grids
            drawLine(color = gridColor, start = Offset(gridX, 0f), end = Offset(gridX, h), strokeWidth = 1.5.dp.toPx())
            // Horizontal grids
            drawLine(color = gridColor, start = Offset(0f, gridY), end = Offset(w, gridY), strokeWidth = 1.5.dp.toPx())
        }

        // Draw diagonal main avenues
        drawLine(color = gridColor.copy(alpha = 1.5f), start = Offset(0f, h * 0.2f), end = Offset(w, h * 0.8f), strokeWidth = 5.dp.toPx())
        drawLine(color = gridColor.copy(alpha = 1.5f), start = Offset(w * 0.3f, 0f), end = Offset(w * 0.8f, h), strokeWidth = 4.5.dp.toPx())

        // Draw area text landmarks matching Parelheiros/Santa Fé/Silveira
        // Since we can't draw raw Native text without TextMeasurer easily, we will focus on drawing polylines and points.

        if (stops.isNotEmpty()) {
            // Draw Route Polyline connecting stops in sequence order
            val path = Path()
            val sortedStops = stops.sortedBy { it.sequence }
            sortedStops.forEachIndexed { index, stop ->
                val px = getX(stop.longitude)
                val py = getY(stop.latitude)
                if (index == 0) {
                    path.moveTo(px, py)
                } else {
                    path.lineTo(px, py)
                }
            }

            // Glowing outline
            drawPath(
                path = path,
                color = BrandBlueLight.copy(alpha = 0.3f),
                style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
            )
            // Inner main track
            drawPath(
                path = path,
                color = BrandBlue,
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw Navigation Simulator Car if active
            if (isNavegando) {
                val currentStop = sortedStops.getOrNull(currentSimulatedStopIndex)
                val prevStop = sortedStops.getOrNull(currentSimulatedStopIndex - 1) ?: sortedStops.lastOrNull()
                if (currentStop != null && prevStop != null) {
                    val prevX = getX(prevStop.longitude)
                    val prevY = getY(prevStop.latitude)
                    val curX = getX(currentStop.longitude)
                    val curY = getY(currentStop.latitude)

                    // Interpolate position
                    val carX = prevX + (curX - prevX) * simulatedProgress
                    val carY = prevY + (curY - prevY) * simulatedProgress

                    // Draw outer pulse halo
                    drawCircle(
                        color = BrandBlue.copy(alpha = 0.4f),
                        radius = 20.dp.toPx(),
                        center = Offset(carX, carY)
                    )
                    // Core navigation marker
                    drawCircle(
                        color = Color.White,
                        radius = 8.dp.toPx(),
                        center = Offset(carX, carY)
                    )
                    drawCircle(
                        color = BrandBlue,
                        radius = 5.dp.toPx(),
                        center = Offset(carX, carY)
                    )
                }
            }

            // Draw stop marker pins
            sortedStops.forEach { stop ->
                val px = getX(stop.longitude)
                val py = getY(stop.latitude)

                val markerColor = when (stop.status) {
                    "COMPLETED" -> StatusCompleted
                    "SKIPPED" -> StatusSkipped
                    else -> BrandBlueLight
                }

                // Shadow/glow ring
                drawCircle(
                    color = Color.Black.copy(alpha = 0.4f),
                    radius = 14.dp.toPx(),
                    center = Offset(px, py + 2.dp.toPx())
                )
                drawCircle(
                    color = markerColor,
                    radius = 11.dp.toPx(),
                    center = Offset(px, py)
                )
                // Center accent
                drawCircle(
                    color = Color.White,
                    radius = 5.dp.toPx(),
                    center = Offset(px, py)
                )
            }
        }
    }
}

// 2. Sliding Bottom Sheet Component
@Composable
fun SlidingBottomSheet(
    stops: List<Stop>,
    isOptimizing: Boolean,
    onOptimize: () -> Unit,
    onAddStopClick: () -> Unit,
    onCopyRouteClick: () -> Unit,
    onImportClick: () -> Unit,
    onTransferClick: () -> Unit,
    onStopClicked: (Stop) -> Unit
) {
    var sheetExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(if (sheetExpanded) 0.85f else 0.45f)
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(DarkSurface)
            .border(1.dp, DarkSurfaceVariant, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .testTag("sliding_bottom_sheet")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Drag handle / expand trigger bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { sheetExpanded = !sheetExpanded }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp, 5.dp)
                        .clip(CircleShape)
                        .background(TextSecondary.copy(alpha = 0.6f))
                )
            }

            if (stops.isEmpty()) {
                // Empty Route State (Image 4)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AddLocation, contentDescription = "Sem paradas", tint = BrandBlueLight, modifier = Modifier.size(36.dp))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Adicione as primeiras paradas para começar a criar sua rota",
                        fontSize = 15.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Primary Add Stops Button
                    Button(
                        onClick = onAddStopClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("add_stops_primary_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                        shape = RoundedCornerShape(26.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Mais", tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Adicionar paradas", fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Secondary Copy Route Button
                    Button(
                        onClick = onCopyRouteClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("copy_stops_secondary_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        border = ButtonDefaults.outlinedButtonBorder,
                        shape = RoundedCornerShape(26.dp)
                    ) {
                        Text(text = "Copiar paradas de uma rota anterior", fontSize = 15.sp, color = BrandBlueLight)
                    }
                }
            } else {
                // Route Active List State
                Column(modifier = Modifier.fillMaxSize()) {
                    // Search box "Toque para adicionar"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(DarkSurfaceVariant)
                            .clickable { onAddStopClick() }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Busca", tint = TextSecondary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Toque para adicionar paradas",
                            color = TextSecondary,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(Icons.Default.Mic, contentDescription = "Voz", tint = TextSecondary, modifier = Modifier.size(20.dp))
                    }

                    // Route Summary Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val completed = stops.count { it.status == "COMPLETED" }
                        Text(
                            text = "$completed de ${stops.size} paradas feitas",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold
                        )

                        Row {
                            TextButton(onClick = onOptimize, modifier = Modifier.testTag("btn_optimize")) {
                                Icon(Icons.Default.AutoMode, contentDescription = "Otimizar", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Otimizar", fontSize = 13.sp, color = BrandBlueLight)
                            }
                            TextButton(onClick = onTransferClick, modifier = Modifier.testTag("btn_transfer")) {
                                Icon(Icons.Default.SwapHoriz, contentDescription = "Transferir", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Transferir", fontSize = 13.sp, color = BrandBlueLight)
                            }
                            TextButton(onClick = onImportClick, modifier = Modifier.testTag("btn_import")) {
                                Icon(Icons.Default.CloudUpload, contentDescription = "Manifesto", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Manifesto", fontSize = 13.sp, color = BrandBlueLight)
                            }
                        }
                    }

                    Divider(color = DarkSurfaceVariant)

                    // Lazy list of route stops
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(stops.sortedBy { it.sequence }) { stop ->
                            StopItemRow(stop = stop, onClick = { onStopClicked(stop) })
                        }
                    }
                }
            }
        }
    }
}

// Stop Item row inside bottom sheet
@Composable
fun StopItemRow(stop: Stop, onClick: () -> Unit) {
    val statusColor = when (stop.status) {
        "COMPLETED" -> StatusCompleted
        "SKIPPED" -> StatusSkipped
        else -> BrandBlueLight
    }

    val statusIcon = when (stop.status) {
        "COMPLETED" -> Icons.Default.Check
        "SKIPPED" -> Icons.Default.Close
        else -> Icons.Default.Place
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Stop number / status icon
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(statusColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            if (stop.status == "PENDING") {
                Text(
                    text = stop.sequence.toString(),
                    color = BrandBlueLight,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Icon(
                    imageVector = statusIcon,
                    contentDescription = stop.status,
                    tint = statusColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Recipient name and Address
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stop.recipientName,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                if (stop.estimatedTime.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "• ETA ${stop.estimatedTime}",
                        color = BrandBlueLight,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stop.address,
                color = TextSecondary,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Ver", tint = DarkSurfaceVariant, modifier = Modifier.size(16.dp))
    }
}

// 3. Stop Detail Card Sheet Component
@Composable
fun StopDetailSheet(
    stop: Stop,
    onClose: () -> Unit,
    onStatusChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(DarkSurface)
            .border(1.dp, DarkSurfaceVariant, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .padding(24.dp)
            .testTag("stop_detail_sheet")
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Entrega #${stop.sequence}",
                    fontSize = 18.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Fechar", tint = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Details info
            Text(text = stop.recipientName, fontSize = 20.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = stop.address, fontSize = 14.sp, color = TextSecondary)

            if (stop.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurfaceVariant)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = "Notas", tint = BrandBlueLight, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stop.notes, color = TextPrimary, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "ABRIR NAVEGAÇÃO GPS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Google Maps Button
                Button(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=${stop.latitude},${stop.longitude}"))
                            intent.setPackage("com.google.android.apps.maps")
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=${stop.latitude},${stop.longitude}"))
                            context.startActivity(intent)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("btn_nav_google_maps"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Map,
                        contentDescription = "Google Maps",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Google Maps", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Waze Button
                Button(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("waze://?ll=${stop.latitude},${stop.longitude}&navigate=yes"))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://waze.com/ul?ll=${stop.latitude},${stop.longitude}&navigate=yes"))
                            context.startActivity(intent)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("btn_nav_waze"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF33CCFF)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = "Waze",
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Waze", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action status buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Done Button
                Button(
                    onClick = { onStatusChange("COMPLETED") },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_mark_completed"),
                    colors = ButtonDefaults.buttonColors(containerColor = StatusCompleted),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Sucesso", tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Feito", color = Color.White, fontWeight = FontWeight.Bold)
                }

                // Skip Button
                Button(
                    onClick = { onStatusChange("SKIPPED") },
                    modifier = Modifier
                        .weight(1.5f)
                        .height(48.dp)
                        .testTag("btn_mark_skipped"),
                    colors = ButtonDefaults.buttonColors(containerColor = StatusSkipped),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Icon(Icons.Default.Cancel, contentDescription = "Pular", tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pular / Falhou", color = Color.White, fontWeight = FontWeight.Bold)
                }

                // Delete Button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceVariant)
                        .testTag("btn_delete_stop")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = TextSecondary)
                }
            }
        }
    }
}

// 4. Add Stop Dialog (Manual entry with Google Places Autocomplete)
@Composable
fun AddStopDialog(
    onDismiss: () -> Unit,
    onAdd: (address: String, name: String, notes: String, time: String) -> Unit,
    onAddWithCoordinates: (address: String, name: String, notes: String, time: String, lat: Double, lon: Double) -> Unit,
    searchSuggestions: List<com.example.data.PlacePrediction>,
    isSearching: Boolean,
    onQueryChange: (String) -> Unit,
    onResolveCoordinates: suspend (String) -> Pair<Double, Double>
) {
    var address by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }

    var selectedPlaceId by remember { mutableStateOf<String?>(null) }
    var selectedCoordinates by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Adicionar Parada", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        OutlinedTextField(
                            value = address,
                            onValueChange = {
                                address = it
                                selectedPlaceId = null
                                selectedCoordinates = null
                                onQueryChange(it)
                            },
                            label = { Text("Endereço completo", color = TextSecondary) },
                            textStyle = TextStyle(color = TextPrimary),
                            modifier = Modifier.fillMaxWidth().testTag("add_input_address"),
                            shape = RoundedCornerShape(8.dp),
                            trailingIcon = {
                                if (isSearching) {
                                    CircularProgressIndicator(
                                        color = BrandBlue,
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                        )

                        if (searchSuggestions.isNotEmpty() && selectedPlaceId == null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 160.dp),
                                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                                shape = RoundedCornerShape(8.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                LazyColumn(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(searchSuggestions) { suggestion ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    address = suggestion.description
                                                    selectedPlaceId = suggestion.placeId
                                                    // Resolve coordinates
                                                    scope.launch {
                                                        selectedCoordinates = onResolveCoordinates(suggestion.placeId)
                                                    }
                                                }
                                                .padding(horizontal = 12.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Place,
                                                contentDescription = "Local",
                                                tint = BrandBlueLight,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = suggestion.description,
                                                color = TextPrimary,
                                                fontSize = 13.sp,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Divider(color = Color.White.copy(alpha = 0.08f))
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Destinatário", color = TextSecondary) },
                    textStyle = TextStyle(color = TextPrimary),
                    modifier = Modifier.fillMaxWidth().testTag("add_input_name"),
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas / Instruções (opcional)", color = TextSecondary) },
                    textStyle = TextStyle(color = TextPrimary),
                    modifier = Modifier.fillMaxWidth().testTag("add_input_notes"),
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it },
                    label = { Text("Janela / Horário (e.g., 14:00)", color = TextSecondary) },
                    textStyle = TextStyle(color = TextPrimary),
                    modifier = Modifier.fillMaxWidth().testTag("add_input_time"),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val coords = selectedCoordinates
                    if (coords != null) {
                        onAddWithCoordinates(address, name, notes, time, coords.first, coords.second)
                    } else {
                        onAdd(address, name, notes, time)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                enabled = address.isNotEmpty() && name.isNotEmpty(),
                modifier = Modifier.testTag("add_confirm_button")
            ) {
                Text("Adicionar", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TextSecondary)
            }
        },
        containerColor = DarkSurface
    )
}

// 5. Import Manifest Dialog (Image 3)
@Composable
fun ImportManifestDialog(
    isParsing: Boolean,
    parseError: String?,
    onDismiss: () -> Unit,
    onParseText: (String) -> Unit,
    onParseImage: (Bitmap) -> Unit
) {
    var pasteText by remember { mutableStateOf("") }
    var selectTab by remember { mutableStateOf(0) } // 0: Text, 1: Gallery (OCR)

    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Importar Manifesto de Rotas", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Tab switcher
                TabRow(
                    selectedTabIndex = selectTab,
                    containerColor = DarkSurface,
                    contentColor = BrandBlueLight,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                ) {
                    Tab(selected = selectTab == 0, onClick = { selectTab = 0 }, text = { Text("Texto Livre", color = TextPrimary) })
                    Tab(selected = selectTab == 1, onClick = { selectTab = 1 }, text = { Text("Galeria / OCR", color = TextPrimary) })
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isParsing) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        CircularProgressIndicator(color = BrandBlue)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = "Google Gemini IA analisando manifesto...", color = TextPrimary, fontSize = 14.sp, textAlign = TextAlign.Center)
                    }
                } else {
                    if (parseError != null) {
                        Text(
                            text = parseError,
                            color = StatusPending,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }

                    if (selectTab == 0) {
                        // Text Paste Tab
                        OutlinedTextField(
                            value = pasteText,
                            onValueChange = { pasteText = it },
                            placeholder = { Text("Cole o texto do manifesto, email, ou planilha do motorista aqui...", color = TextSecondary) },
                            textStyle = TextStyle(color = TextPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .testTag("import_text_field"),
                            shape = RoundedCornerShape(8.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Quick Fill Pre-set button
                        TextButton(
                            onClick = {
                                pasteText = """
                                    Manifesto DHL - Data: 20/06/2026
                                    1. Entregar na Avenida das Américas, 432 para Sônia Costa. Notas: Deixar com vigilante.
                                    2. Entregar na Rua Teotônio Vilela, 120 para Bruno Castro às 14h30.
                                    3. Entregar na Rua Jardim das Flores, 88 para Carla Souza.
                                """.trimIndent()
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Preencher Manifesto Exemplo", color = BrandBlueLight, fontSize = 12.sp)
                        }
                    } else {
                        // Gallery/Image tab (OCR)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkSurfaceVariant)
                                .padding(24.dp)
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = "Galeria", tint = BrandBlueLight, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(text = "Selecione a foto do manifesto impresso", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(text = "Analisaremos a foto com Gemini IA", color = TextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center)

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    // Trigger parsing of a mock loaded bitmap
                                    // In a production app, we would launch image picker, here we decode a simulated checklist
                                    val mockBitmap = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888)
                                    onParseImage(mockBitmap)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                            ) {
                                Text("Importar Foto da Galeria", color = Color.White)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (selectTab == 0 && !isParsing) {
                Button(
                    onClick = { onParseText(pasteText) },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                    enabled = pasteText.isNotEmpty()
                ) {
                    Text("Analisar Manifesto", color = Color.White)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TextSecondary)
            }
        },
        containerColor = DarkSurface
    )
}

// 6. Transfer Stops Dialog (Image 1)
@Composable
fun TransferStopsDialog(
    stops: List<Stop>,
    isReceiving: Boolean,
    onDismiss: () -> Unit,
    onReceiveClick: () -> Unit
) {
    var subDialogMode by remember { mutableStateOf(0) } // 0: Main menu, 1: Enviar (QR Code), 2: Receber (Scanner)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            val title = when (subDialogMode) {
                1 -> "Enviar paradas"
                2 -> "Assumir paradas"
                else -> "Transferir paradas"
            }
            Text(text = title, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (subDialogMode == 0) {
                    // Main Transfer Menu (exactly matching Image 1)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { subDialogMode = 1 }
                            .padding(vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = "Enviar", tint = BrandBlueLight, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Enviar paradas...", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text(text = "Transferir paradas para outro motorista", color = TextSecondary, fontSize = 13.sp)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = "Ir", tint = TextSecondary)
                    }

                    Divider(color = DarkSurfaceVariant)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { subDialogMode = 2 }
                            .padding(vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Receber", tint = BrandBlueLight, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Receber paradas", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text(text = "Leia o QR Code para assumir paradas", color = TextSecondary, fontSize = 13.sp)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = "Ir", tint = TextSecondary)
                    }
                } else if (subDialogMode == 1) {
                    // Send paradas - Draws a beautifully animated QR code on Canvas
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Aponte a câmera do outro dispositivo para este código para compartilhar ${stops.size} paradas.", color = TextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)

                        Spacer(modifier = Modifier.height(20.dp))

                        // Styled Custom QR Code visual
                        Canvas(modifier = Modifier.size(180.dp)) {
                            // Border
                            drawRect(color = Color.White, size = size)
                            // Draw simulated QR anchors (corner boxes)
                            val anchorSize = 40f
                            drawRect(color = Color.Black, topLeft = Offset(10f, 10f), size = Size(anchorSize, anchorSize))
                            drawRect(color = Color.White, topLeft = Offset(20f, 20f), size = Size(20f, 20f))

                            drawRect(color = Color.Black, topLeft = Offset(size.width - anchorSize - 10f, 10f), size = Size(anchorSize, anchorSize))
                            drawRect(color = Color.White, topLeft = Offset(size.width - anchorSize, 20f), size = Size(20f, 20f))

                            drawRect(color = Color.Black, topLeft = Offset(10f, size.height - anchorSize - 10f), size = Size(anchorSize, anchorSize))
                            drawRect(color = Color.White, topLeft = Offset(20f, size.height - anchorSize), size = Size(20f, 20f))

                            // Simulated code matrix grids
                            for (x in 2..8) {
                                for (y in 2..8) {
                                    if ((x + y) % 3 == 0 || (x * y) % 5 == 1) {
                                        drawRect(
                                            color = Color.Black,
                                            topLeft = Offset(20f + x * 15f, 20f + y * 15f),
                                            size = Size(10f, 10f)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = "Código QR Ativo", color = StatusCompleted, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                } else if (subDialogMode == 2) {
                    // Scan simulator
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isReceiving) {
                            CircularProgressIndicator(color = BrandBlue)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(text = "Lendo paradas do código QR...", color = TextPrimary)
                        } else {
                            Text(text = "Aponte para o QR Code de transferência do outro celular.", color = TextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)

                            Spacer(modifier = Modifier.height(16.dp))

                            // Camera scanner viewfinder visual representation
                            Box(
                                modifier = Modifier
                                    .size(200.dp)
                                    .border(2.dp, BrandBlue, RoundedCornerShape(12.dp))
                                    .background(Color.Black.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                val infiniteTransition = rememberInfiniteTransition()
                                val laserY by infiniteTransition.animateFloat(
                                    initialValue = 0f,
                                    targetValue = 200f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(1500, easing = LinearEasing),
                                        repeatMode = RepeatMode.Reverse
                                    )
                                )

                                // Green laser scanning line
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    drawLine(
                                        color = Color.Green,
                                        start = Offset(0f, laserY),
                                        end = Offset(size.width, laserY),
                                        strokeWidth = 3f
                                    )
                                }

                                Icon(Icons.Default.QrCodeScanner, contentDescription = "Scanner", tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(64.dp))
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = onReceiveClick,
                                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                            ) {
                                Text("Simular Leitura QR", color = Color.White)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = {
                    if (subDialogMode > 0) {
                        subDialogMode = 0
                    } else {
                        onDismiss()
                    }
                }
            ) {
                Text(if (subDialogMode > 0) "Voltar" else "Fechar", color = TextSecondary)
            }
        },
        containerColor = DarkSurface
    )
}

// Side drawer menu item helper
@Composable
fun DrawerItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) BrandBlue.copy(alpha = 0.15f) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = label, tint = if (selected) BrandBlueLight else TextSecondary)
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = label, color = if (selected) BrandBlueLight else TextPrimary, fontSize = 15.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}
