package com.example.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.GeminiParser
import com.example.data.RouteDatabase
import com.example.data.RouteRepository
import com.example.data.Stop
import com.example.data.PlacePrediction
import com.example.data.PlacesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class AppScreen {
    MAIN,
    REUSE_STOPS
}

class RouteViewModel(private val repository: RouteRepository) : ViewModel() {

    // Google Places Autocomplete States
    private val _searchSuggestions = MutableStateFlow<List<PlacePrediction>>(emptyList())
    val searchSuggestions: StateFlow<List<PlacePrediction>> = _searchSuggestions.asStateFlow()

    private val _isSearchingPlaces = MutableStateFlow(false)
    val isSearchingPlaces: StateFlow<Boolean> = _isSearchingPlaces.asStateFlow()

    fun onSearchQueryChanged(query: String) {
        if (query.trim().isEmpty()) {
            _searchSuggestions.value = emptyList()
            return
        }
        viewModelScope.launch {
            _isSearchingPlaces.value = true
            try {
                val results = PlacesRepository.searchPlaces(query)
                _searchSuggestions.value = results
            } catch (e: Exception) {
                Log.e("RouteViewModel", "Error searching places", e)
            } finally {
                _isSearchingPlaces.value = false
            }
        }
    }

    suspend fun resolvePlaceCoordinates(placeId: String): Pair<Double, Double> {
        return try {
            val loc = PlacesRepository.getPlaceCoordinates(placeId)
            Pair(loc.lat, loc.lng)
        } catch (e: Exception) {
            Log.e("RouteViewModel", "Error resolving place coordinates", e)
            Pair(-23.7915, -46.6890) // default
        }
    }

    fun addStopWithCoordinates(address: String, recipientName: String, notes: String, time: String, lat: Double, lon: Double) {
        viewModelScope.launch {
            val currentStops = stops.value
            val nextSeq = (currentStops.maxOfOrNull { it.sequence } ?: 0) + 1

            val newStop = Stop(
                address = address,
                recipientName = recipientName,
                status = "PENDING",
                sequence = nextSeq,
                latitude = lat,
                longitude = lon,
                routeDate = _routeDate.value,
                notes = notes,
                phoneNumber = "+55 11 9" + (10000000 + (Math.random() * 90000000).toLong()).toString(),
                estimatedTime = if (time.isNotEmpty()) time else "15:30"
            )
            repository.insertStop(newStop)
        }
    }

    private val _activeScreen = MutableStateFlow(AppScreen.MAIN)
    val activeScreen: StateFlow<AppScreen> = _activeScreen.asStateFlow()

    private val _routeDate = MutableStateFlow("20-06-2026")
    val routeDate: StateFlow<String> = _routeDate.asStateFlow()

    // Observe stops for the selected date reactively
    val stops: StateFlow<List<Stop>> = _routeDate
        .flatMapLatest { date ->
            repository.getStopsForRoute(date)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allRouteDates: StateFlow<List<String>> = repository.allRouteDates
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = listOf("19-06-2026", "20-06-2026")
        )

    // UI state states
    private val _isOptimizing = MutableStateFlow(false)
    val isOptimizing: StateFlow<Boolean> = _isOptimizing.asStateFlow()

    private val _isNavegando = MutableStateFlow(false)
    val isNavegando: StateFlow<Boolean> = _isNavegando.asStateFlow()

    private val _simulatedProgress = MutableStateFlow(0f)
    val simulatedProgress: StateFlow<Float> = _simulatedProgress.asStateFlow()

    private val _currentSimulatedStopIndex = MutableStateFlow(0)
    val currentSimulatedStopIndex: StateFlow<Int> = _currentSimulatedStopIndex.asStateFlow()

    private val _selectedStopForDetail = MutableStateFlow<Stop?>(null)
    val selectedStopForDetail: StateFlow<Stop?> = _selectedStopForDetail.asStateFlow()

    private val _showAddStopDialog = MutableStateFlow(false)
    val showAddStopDialog: StateFlow<Boolean> = _showAddStopDialog.asStateFlow()

    private val _showImportDialog = MutableStateFlow(false)
    val showImportDialog: StateFlow<Boolean> = _showImportDialog.asStateFlow()

    private val _showTransferDialog = MutableStateFlow(false)
    val showTransferDialog: StateFlow<Boolean> = _showTransferDialog.asStateFlow()

    private val _isParsingManifest = MutableStateFlow(false)
    val isParsingManifest: StateFlow<Boolean> = _isParsingManifest.asStateFlow()

    private val _manifestParseError = MutableStateFlow<String?>(null)
    val manifestParseError: StateFlow<String?> = _manifestParseError.asStateFlow()

    fun navigateTo(screen: AppScreen) {
        _activeScreen.value = screen
    }

    fun selectRouteDate(date: String) {
        _routeDate.value = date
        // Reset navigation when switching routes
        _isNavegando.value = false
        _currentSimulatedStopIndex.value = 0
    }

    fun setShowAddStopDialog(show: Boolean) {
        _showAddStopDialog.value = show
    }

    fun setShowImportDialog(show: Boolean) {
        _showImportDialog.value = show
        if (!show) {
            _manifestParseError.value = null
        }
    }

    fun setShowTransferDialog(show: Boolean) {
        _showTransferDialog.value = show
    }

    fun setSelectedStopForDetail(stop: Stop?) {
        _selectedStopForDetail.value = stop
    }

    // CRUD operations
    fun addStop(address: String, recipientName: String, notes: String, time: String) {
        viewModelScope.launch {
            // Jardim Novo Parelheiros central lat/lon
            val baseLat = -23.7915
            val baseLon = -46.6890
            // Randomly offset to plot nicely on the custom canvas map
            val lat = baseLat + (Math.random() - 0.5) * 0.02
            val lon = baseLon + (Math.random() - 0.5) * 0.02

            val currentStops = stops.value
            val nextSeq = (currentStops.maxOfOrNull { it.sequence } ?: 0) + 1

            val newStop = Stop(
                address = address,
                recipientName = recipientName,
                status = "PENDING",
                sequence = nextSeq,
                latitude = lat,
                longitude = lon,
                routeDate = _routeDate.value,
                notes = notes,
                phoneNumber = "+55 11 9" + (10000000 + (Math.random() * 90000000).toLong()).toString(),
                estimatedTime = if (time.isNotEmpty()) time else "15:30"
            )
            repository.insertStop(newStop)
        }
    }

    fun deleteStop(id: Int) {
        viewModelScope.launch {
            repository.deleteStop(id)
            // Re-sequence remaining stops
            delay(100)
            val remainingStops = stops.value
            remainingStops.forEachIndexed { index, stop ->
                if (stop.sequence != index + 1) {
                    repository.updateStop(stop.copy(sequence = index + 1))
                }
            }
        }
    }

    fun updateStopStatus(stop: Stop, status: String) {
        viewModelScope.launch {
            repository.updateStop(stop.copy(status = status))
            if (_selectedStopForDetail.value?.id == stop.id) {
                _selectedStopForDetail.value = stop.copy(status = status)
            }
        }
    }

    fun optimizeRoute() {
        viewModelScope.launch {
            _isOptimizing.value = true
            // Simulate route optimization processing
            delay(1800)
            val currentStops = stops.value
            if (currentStops.isNotEmpty()) {
                // simple simulated nearest-neighbor clustering optimization
                // sort stops geographically from a start point (-23.7915, -46.6890)
                val baseLat = -23.7915
                val baseLon = -46.6890
                val sorted = currentStops.sortedBy { stop ->
                    Math.pow(stop.latitude - baseLat, 2.0) + Math.pow(stop.longitude - baseLon, 2.0)
                }
                sorted.forEachIndexed { index, stop ->
                    repository.updateStop(stop.copy(sequence = index + 1))
                }
            }
            _isOptimizing.value = false
        }
    }

    // Reuse/Copy stops (Shown in Image 2)
    fun copyStops(
        fromDate: String,
        toDate: String,
        copyPending: Boolean,
        copySkipped: Boolean,
        copyCompleted: Boolean
    ) {
        viewModelScope.launch {
            // Get source stops
            repository.getStopsForRoute(fromDate).collect { sourceStops ->
                if (sourceStops.isNotEmpty()) {
                    val filtered = sourceStops.filter { stop ->
                        (stop.status == "PENDING" && copyPending) ||
                        (stop.status == "SKIPPED" && copySkipped) ||
                        (stop.status == "COMPLETED" && copyCompleted)
                    }

                    if (filtered.isNotEmpty()) {
                        // Find current max sequence on target route to append
                        repository.getStopsForRoute(toDate).collect { targetStops ->
                            var seq = targetStops.maxOfOrNull { it.sequence } ?: 0
                            val stopsToInsert = filtered.map { stop ->
                                seq++
                                Stop(
                                    address = stop.address,
                                    recipientName = stop.recipientName,
                                    status = "PENDING", // Resets to pending for the new route!
                                    sequence = seq,
                                    latitude = stop.latitude + (Math.random() - 0.5) * 0.002, // slightly vary
                                    longitude = stop.longitude + (Math.random() - 0.5) * 0.002,
                                    routeDate = toDate,
                                    notes = stop.notes,
                                    phoneNumber = stop.phoneNumber,
                                    estimatedTime = stop.estimatedTime
                                )
                            }
                            repository.insertStops(stopsToInsert)
                        }
                    }
                }
                // Return to main
                _activeScreen.value = AppScreen.MAIN
            }
        }
    }

    // Active Simulator
    fun toggleNavegacao() {
        if (_isNavegando.value) {
            _isNavegando.value = false
        } else {
            val pendingStops = stops.value.filter { it.status == "PENDING" }
            if (pendingStops.isNotEmpty()) {
                _isNavegando.value = true
                _currentSimulatedStopIndex.value = stops.value.indexOf(pendingStops.first())
                startSimulationLoop()
            }
        }
    }

    private fun startSimulationLoop() {
        viewModelScope.launch {
            while (_isNavegando.value) {
                _simulatedProgress.value = 0f
                // Increment progress over time
                for (i in 1..100) {
                    if (!_isNavegando.value) break
                    delay(80)
                    _simulatedProgress.value = i / 100f
                }
                if (_isNavegando.value) {
                    // Trigger arrival at the current stop
                    val currentStop = stops.value.getOrNull(_currentSimulatedStopIndex.value)
                    if (currentStop != null) {
                        _selectedStopForDetail.value = currentStop
                    }
                    // Pause on arrival
                    delay(3000)
                    // Auto-advance or wait for user action
                    val nextPending = stops.value.filterIndexed { index, stop ->
                        index > _currentSimulatedStopIndex.value && stop.status == "PENDING"
                    }
                    if (nextPending.isNotEmpty()) {
                        _currentSimulatedStopIndex.value = stops.value.indexOf(nextPending.first())
                    } else {
                        _isNavegando.value = false
                    }
                }
            }
        }
    }

    // Manifest text import via Gemini
    fun parseAndAddManifestText(text: String) {
        viewModelScope.launch {
            _isParsingManifest.value = true
            _manifestParseError.value = null
            try {
                val parsed = GeminiParser.parseManifestText(text)
                if (parsed.isNotEmpty()) {
                    val currentStops = stops.value
                    var seq = currentStops.maxOfOrNull { it.sequence } ?: 0

                    val baseLat = -23.7915
                    val baseLon = -46.6890

                    val stopsToInsert = parsed.map { parsedStop ->
                        seq++
                        Stop(
                            address = parsedStop.address,
                            recipientName = parsedStop.recipientName,
                            status = "PENDING",
                            sequence = seq,
                            latitude = baseLat + (Math.random() - 0.5) * 0.015,
                            longitude = baseLon + (Math.random() - 0.5) * 0.015,
                            routeDate = _routeDate.value,
                            notes = parsedStop.notes,
                            phoneNumber = "+55 11 9" + (10000000 + (Math.random() * 90000000).toLong()).toString(),
                            estimatedTime = parsedStop.estimatedTime
                        )
                    }
                    repository.insertStops(stopsToInsert)
                    _showImportDialog.value = false
                } else {
                    _manifestParseError.value = "Chave da API ausente ou inválida. Simulando importação..."
                    simulateFallbackImport()
                }
            } catch (e: Exception) {
                Log.e("RouteViewModel", "Error parsing manifest text", e)
                _manifestParseError.value = "Erro ao conectar com a IA: ${e.localizedMessage}"
                simulateFallbackImport()
            } finally {
                _isParsingManifest.value = false
            }
        }
    }

    // Image/Gallery Manifest OCR parser via Gemini
    fun parseAndAddManifestImage(bitmap: Bitmap, promptExtra: String = "") {
        viewModelScope.launch {
            _isParsingManifest.value = true
            _manifestParseError.value = null
            try {
                val parsed = GeminiParser.parseManifestImage(bitmap, promptExtra)
                if (parsed.isNotEmpty()) {
                    val currentStops = stops.value
                    var seq = currentStops.maxOfOrNull { it.sequence } ?: 0

                    val baseLat = -23.7915
                    val baseLon = -46.6890

                    val stopsToInsert = parsed.map { parsedStop ->
                        seq++
                        Stop(
                            address = parsedStop.address,
                            recipientName = parsedStop.recipientName,
                            status = "PENDING",
                            sequence = seq,
                            latitude = baseLat + (Math.random() - 0.5) * 0.015,
                            longitude = baseLon + (Math.random() - 0.5) * 0.015,
                            routeDate = _routeDate.value,
                            notes = parsedStop.notes,
                            phoneNumber = "+55 11 9" + (10000000 + (Math.random() * 90000000).toLong()).toString(),
                            estimatedTime = parsedStop.estimatedTime
                        )
                    }
                    repository.insertStops(stopsToInsert)
                    _showImportDialog.value = false
                } else {
                    _manifestParseError.value = "IA indisponível (Chave API não configurada). Importando dados simulados..."
                    simulateFallbackImport()
                }
            } catch (e: Exception) {
                Log.e("RouteViewModel", "Error parsing manifest image", e)
                _manifestParseError.value = "Erro ao processar imagem: ${e.localizedMessage}"
                simulateFallbackImport()
            } finally {
                _isParsingManifest.value = false
            }
        }
    }

    private suspend fun simulateFallbackImport() {
        delay(2000) // Simulate processing time
        val mockParsed = listOf(
            Stop(
                address = "Avenida Senador Teotônio Vilela, 5000 - Jardim Novo Parelheiros",
                recipientName = "Diego dos Santos (Via Manifesto)",
                status = "PENDING",
                sequence = (stops.value.maxOfOrNull { it.sequence } ?: 0) + 1,
                latitude = -23.7880,
                longitude = -46.6930,
                routeDate = _routeDate.value,
                notes = "Deixar com o segurança",
                phoneNumber = "+55 11 98888-7777",
                estimatedTime = "15:45"
            ),
            Stop(
                address = "Rua Jardim das Fontes, 12 - Recanto Campo Belo",
                recipientName = "Mariana Luz",
                status = "PENDING",
                sequence = (stops.value.maxOfOrNull { it.sequence } ?: 0) + 2,
                latitude = -23.7950,
                longitude = -46.6840,
                routeDate = _routeDate.value,
                notes = "Entrada pelos fundos",
                phoneNumber = "+55 11 98888-5555",
                estimatedTime = "16:20"
            )
        )
        repository.insertStops(mockParsed)
        _showImportDialog.value = false
    }

    // QR-based Transfer simulator
    fun simulateQRReceive() {
        viewModelScope.launch {
            _isParsingManifest.value = true
            delay(1500)
            val mockScanned = listOf(
                Stop(
                    address = "Rua Alfredo Hanisch, 84 - Jardim Santa Fe",
                    recipientName = "Pedro Henrique (Transferência)",
                    status = "PENDING",
                    sequence = (stops.value.maxOfOrNull { it.sequence } ?: 0) + 1,
                    latitude = -23.7865,
                    longitude = -46.6800,
                    routeDate = _routeDate.value,
                    notes = "Entregue via código QR",
                    phoneNumber = "+55 11 97777-6666",
                    estimatedTime = "13:50"
                )
            )
            repository.insertStops(mockScanned)
            _isParsingManifest.value = false
            _showTransferDialog.value = false
        }
    }
}

class ViewModelFactory(private val repository: RouteRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RouteViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RouteViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
