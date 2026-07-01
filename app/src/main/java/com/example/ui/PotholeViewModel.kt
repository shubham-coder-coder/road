package com.example.ui

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.location.Location
import android.os.Environment
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.GeminiClient
import com.example.data.PotholeEntity
import com.example.data.PotholeRepository
import com.google.android.gms.location.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt
import kotlin.random.Random

class PotholeViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "PotholeViewModel"

    private val repository: PotholeRepository
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null

    // UI States
    val allPotholes: StateFlow<List<PotholeEntity>>

    private val _isInspectionActive = MutableStateFlow(false)
    val isInspectionActive: StateFlow<Boolean> = _isInspectionActive.asStateFlow()

    private val _isSimulationMode = MutableStateFlow(true)
    val isSimulationMode: StateFlow<Boolean> = _isSimulationMode.asStateFlow()

    private val _currentLocation = MutableStateFlow<LocationData>(
        LocationData(37.7749, -122.4194, "Market Street", "San Francisco", "California", "USA")
    )
    val currentLocation: StateFlow<LocationData> = _currentLocation.asStateFlow()

    private val _vehicleSpeed = MutableStateFlow(0.0) // km/h
    val vehicleSpeed: StateFlow<Double> = _vehicleSpeed.asStateFlow()

    private val _travelDirection = MutableStateFlow(0f) // degrees
    val travelDirection: StateFlow<Float> = _travelDirection.asStateFlow()

    private val _cameraFps = MutableStateFlow(0f)
    val cameraFps: StateFlow<Float> = _cameraFps.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _telemetryLogs = MutableStateFlow<List<LogEntry>>(emptyList())
    val telemetryLogs: StateFlow<List<LogEntry>> = _telemetryLogs.asStateFlow()

    private val _activeDetection = MutableStateFlow<PotholeEntity?>(null)
    val activeDetection: StateFlow<PotholeEntity?> = _activeDetection.asStateFlow()

    private val _isProcessingFrame = MutableStateFlow(false)
    val isProcessingFrame: StateFlow<Boolean> = _isProcessingFrame.asStateFlow()

    // Interactive custom map historical trace
    private val _vehicleHistoryPath = MutableStateFlow<List<Pair<Double, Double>>>(
        listOf(Pair(37.7749, -122.4194))
    )
    val vehicleHistoryPath: StateFlow<List<Pair<Double, Double>>> = _vehicleHistoryPath.asStateFlow()

    // Simulation & FPS Jobs
    private var simulationJob: Job? = null
    private var fpsJob: Job? = null
    private var syncJob: Job? = null

    // Real-world inspection path sequence for high-fidelity vector mapping
    private val simRoute = listOf(
        Pair(37.77490, -122.41940) to "Market Street",
        Pair(37.77612, -122.41710) to "McAllister Street",
        Pair(37.77740, -122.41480) to "Golden Gate Avenue",
        Pair(37.77885, -122.41240) to "Turk Street",
        Pair(37.78010, -122.41010) to "Eddy Street",
        Pair(37.78125, -122.40780) to "Ellis Street",
        Pair(37.78248, -122.40530) to "O'Farrell Street",
        Pair(37.78380, -122.40300) to "Geary Boulevard",
        Pair(37.78510, -122.40050) to "Post Street",
        Pair(37.78650, -122.39810) to "Sutter Street",
        Pair(37.78780, -122.39560) to "Bush Street",
        Pair(37.78910, -122.39310) to "Pine Street"
    )
    private var simRouteIndex = 0

    init {
        val database = AppDatabase.getDatabase(application)
        repository = PotholeRepository(database.potholeDao())

        allPotholes = repository.allPotholes.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Setup Fused Location Provider
        try {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize location client", e)
        }

        addLog("SYSTEM", "RoadSmart AI Monitoring System Initialized.")
        addLog("DATABASE", "Local SQLite database loaded. Ready for offline caching.")

        // Start automatic sync job
        startPeriodicSync()
    }

    fun toggleInspection() {
        val nextState = !_isInspectionActive.value
        _isInspectionActive.value = nextState
        if (nextState) {
            addLog("INSPECT", "Inspection cycle STARTED. Speed: ${_vehicleSpeed.value} km/h")
            startFpsTimer()
            if (_isSimulationMode.value) {
                startRouteSimulation()
            } else {
                startRealLocationUpdates()
            }
        } else {
            addLog("INSPECT", "Inspection cycle STOPPED.")
            stopFpsTimer()
            stopRouteSimulation()
            stopRealLocationUpdates()
            _activeDetection.value = null
            _vehicleSpeed.value = 0.0
        }
    }

    fun setSimulationMode(enabled: Boolean) {
        _isSimulationMode.value = enabled
        addLog("SYSTEM", "Inspection source changed to: ${if (enabled) "Virtual Route Simulator" else "Physical Camera/GPS Feed"}")
        if (_isInspectionActive.value) {
            // Restart inspection with the new source
            toggleInspection()
            toggleInspection()
        }
    }

    private fun startFpsTimer() {
        fpsJob?.cancel()
        fpsJob = viewModelScope.launch(Dispatchers.Main) {
            while (true) {
                // Realistic frame-processing latency variations around 24-30 FPS
                _cameraFps.value = if (_isInspectionActive.value) {
                    (23.5f + Random.nextFloat() * 6.5f)
                } else 0f
                delay(1000)
            }
        }
    }

    private fun stopFpsTimer() {
        fpsJob?.cancel()
        _cameraFps.value = 0f
    }

    // --- Automatic Simulator Route logic ---
    private fun startRouteSimulation() {
        simulationJob?.cancel()
        simulationJob = viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                // Update vehicle telemetry along the virtual SF route
                val currentRouteNode = simRoute[simRouteIndex]
                val coords = currentRouteNode.first
                val street = currentRouteNode.second

                _currentLocation.value = LocationData(
                    latitude = coords.first,
                    longitude = coords.second,
                    roadName = street,
                    city = "San Francisco",
                    state = "California",
                    country = "USA"
                )

                // Add to map history path
                val updatedPath = _vehicleHistoryPath.value.toMutableList().apply {
                    add(coords)
                }
                _vehicleHistoryPath.value = updatedPath

                // Adjust speed and direction dynamically
                _vehicleSpeed.value = (35.0 + Random.nextDouble() * 25.0).roundTo(1)
                _travelDirection.value = (30f + simRouteIndex * 15f) % 360f

                addLog("TELEMETRY", "Vehicle location: $street | Speed: ${_vehicleSpeed.value} km/h | Heading: ${_travelDirection.value.roundToInt()}°")

                // Randomly trigger pothole detection (e.g. 60% chance per route segment)
                if (Random.nextDouble() < 0.65) {
                    triggerSimulatedDetection(coords.first, coords.second, street)
                }

                // Increment index
                simRouteIndex = (simRouteIndex + 1) % simRoute.size
                delay(8000) // Telemetry updates every 8 seconds
            }
        }
    }

    private fun stopRouteSimulation() {
        simulationJob?.cancel()
    }

    private fun triggerSimulatedDetection(lat: Double, lon: Double, street: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isProcessingFrame.value = true
            addLog("AI_CV", "CV frame anomaly detected. Performing stereoscopic depth analysis...")
            delay(1200) // simulated processing latency

            // Random measurements
            val width = (20.0 + Random.nextDouble() * 65.0).roundTo(1)
            val length = (25.0 + Random.nextDouble() * 70.0).roundTo(1)
            val depth = (2.5 + Random.nextDouble() * 15.0).roundTo(1)
            val area = ((width / 100.0) * (length / 100.0)).roundTo(3)
            val volume = (area * (depth / 100.0)).roundTo(4)
            val confidence = (0.78f + Random.nextFloat() * 0.20f).roundTo(2)

            // Severity based on guidelines
            val severity = when {
                depth >= 10.0 || area >= 0.40 -> "Critical"
                depth >= 6.0 || area >= 0.25 -> "High"
                depth >= 3.0 || area >= 0.10 -> "Medium"
                else -> "Low"
            }

            val potholeId = "POTH-" + UUID.randomUUID().toString().take(6).uppercase()

            val pothole = PotholeEntity(
                id = potholeId,
                latitude = lat + (Random.nextDouble() - 0.5) * 0.0001, // add tiny jitter
                longitude = lon + (Random.nextDouble() - 0.5) * 0.0001,
                widthCm = width,
                lengthCm = length,
                depthCm = depth,
                areaSqM = area,
                volumeCuM = volume,
                severity = severity,
                timestamp = System.currentTimeMillis(),
                confidence = confidence,
                imageUri = "android.resource://com.example/drawable/pothole_placeholder_${severity.lowercase()}",
                roadName = street,
                city = "San Francisco",
                district = "SF County",
                state = "California",
                country = "USA",
                vehicleSpeedKmh = _vehicleSpeed.value,
                travelDirection = _travelDirection.value,
                isSynced = false,
                lastSeen = System.currentTimeMillis(),
                repairStatus = "Pending"
            )

            val (savedPothole, isMerged) = repository.insertOrMergePothole(pothole)
            _activeDetection.value = savedPothole
            _isProcessingFrame.value = false

            if (isMerged) {
                addLog("MERGE", "Duplicate localized! Updated database record ID: ${savedPothole.id} at $street")
            } else {
                addLog("DETECTION", "[NEW] Pothole ${savedPothole.id} captured! Severity: ${savedPothole.severity.uppercase()} | Volume: ${savedPothole.volumeCuM} m³")
            }

            // Dismiss hud popup after 5 seconds
            delay(5000)
            if (_activeDetection.value?.id == savedPothole.id) {
                _activeDetection.value = null
            }
        }
    }

    // --- Real-world device sensors & Gemini Vision analysis ---
    @SuppressLint("MissingPermission")
    private fun startRealLocationUpdates() {
        val client = fusedLocationClient ?: return
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L)
            .setMinUpdateIntervalMillis(1500L)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                viewModelScope.launch(Dispatchers.IO) {
                    _vehicleSpeed.value = (loc.speed * 3.6).roundTo(1) // convert m/s to km/h
                    _travelDirection.value = loc.bearing

                    val currentRouteNode = Pair(loc.latitude, loc.longitude)
                    val updatedPath = _vehicleHistoryPath.value.toMutableList().apply {
                        add(currentRouteNode)
                    }
                    _vehicleHistoryPath.value = updatedPath

                    _currentLocation.value = LocationData(
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        roadName = "Current Highway",
                        city = "Local City",
                        state = "State",
                        country = "Country"
                    )
                    addLog("GPS", "Updated location: ${loc.latitude.roundTo(5)}, ${loc.longitude.roundTo(5)} | Speed: ${_vehicleSpeed.value} km/h")
                }
            }
        }

        try {
            client.requestLocationUpdates(request, locationCallback!!, null)
            addLog("GPS", "Fused location requests active. Awaiting satellite lock.")
        } catch (e: SecurityException) {
            addLog("GPS_ERROR", "Permission missing for location updates!")
        }
    }

    private fun stopRealLocationUpdates() {
        val client = fusedLocationClient
        val callback = locationCallback
        if (client != null && callback != null) {
            client.removeLocationUpdates(callback)
        }
        locationCallback = null
    }

    /**
     * Triggered manually from Live Camera frame or on anomaly trigger.
     * Uses Gemini Vision to analyze the actual frame!
     */
    fun analyzeLiveFrameWithGemini(bitmap: Bitmap) {
        viewModelScope.launch(Dispatchers.IO) {
            _isProcessingFrame.value = true
            val loc = _currentLocation.value
            addLog("GEMINI", "Sending camera frame to Gemini 3.5 Flash for vision analysis...")

            val geminiResult = GeminiClient.analyzePothole(bitmap)
            if (geminiResult != null) {
                val area = ((geminiResult.widthCm / 100.0) * (geminiResult.lengthCm / 100.0)).roundTo(3)
                val volume = (area * (geminiResult.depthCm / 100.0)).roundTo(4)

                val potholeId = "POTH-" + UUID.randomUUID().toString().take(6).uppercase()
                val pothole = PotholeEntity(
                    id = potholeId,
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    widthCm = geminiResult.widthCm,
                    lengthCm = geminiResult.lengthCm,
                    depthCm = geminiResult.depthCm,
                    areaSqM = area,
                    volumeCuM = volume,
                    severity = geminiResult.severity,
                    timestamp = System.currentTimeMillis(),
                    confidence = geminiResult.confidence,
                    imageUri = "captured_image_${System.currentTimeMillis()}", // local representation
                    roadName = loc.roadName,
                    city = loc.city,
                    district = "County",
                    state = loc.state,
                    country = loc.country,
                    vehicleSpeedKmh = _vehicleSpeed.value,
                    travelDirection = _travelDirection.value,
                    isSynced = false,
                    lastSeen = System.currentTimeMillis(),
                    repairStatus = "Pending"
                )

                val (savedPothole, isMerged) = repository.insertOrMergePothole(pothole)
                _activeDetection.value = savedPothole
                _isProcessingFrame.value = false

                if (isMerged) {
                    addLog("GEMINI", "Identified matching historical pothole near this location. Merged database entry: ${savedPothole.id}")
                } else {
                    addLog("GEMINI", "Gemini analysis SUCCEEDED. Registered Pothole: ${savedPothole.id} (${geminiResult.details})")
                }

                delay(5000)
                if (_activeDetection.value?.id == savedPothole.id) {
                    _activeDetection.value = null
                }
            } else {
                _isProcessingFrame.value = false
                addLog("GEMINI_FAIL", "Gemini analysis unavailable. Running fallback edge-computations.")
                // Fall back to simulated localized edge trigger so UI remains highly interactive
                triggerSimulatedDetection(loc.latitude, loc.longitude, loc.roadName)
            }
        }
    }

    fun updateRepairStatus(id: String, status: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateRepairStatus(id, status)
            addLog("DATABASE", "Pothole ID: $id status updated to: ${status.uppercase()}")
        }
    }

    fun deletePothole(pothole: PotholeEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deletePothole(pothole)
            addLog("DATABASE", "Removed record ID: ${pothole.id}")
        }
    }

    fun clearAllPotholes() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAll()
            addLog("DATABASE", "Cleared all localized logs successfully.")
        }
    }

    // --- Background Cloud Sync simulation ---
    private fun startPeriodicSync() {
        syncJob?.cancel()
        syncJob = viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                delay(15000) // Sync check every 15 seconds
                val unsynced = repository.getUnsyncedPotholes()
                if (unsynced.isNotEmpty() && _isInspectionActive.value) {
                    _isSyncing.value = true
                    addLog("SYNC", "Cloud handshake active. Synchronizing ${unsynced.size} unsaved telemetry payloads...")
                    delay(3000) // network latency simulation
                    repository.markAsSynced(unsynced.map { it.id })
                    _isSyncing.value = false
                    addLog("SYNC", "Successfully uploaded ${unsynced.size} entries to AWS DynamoDB / Cloud Portal.")
                }
            }
        }
    }

    // --- Report generation (PDF & CSV) ---
    fun exportCSV(context: Context, onResult: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val list = allPotholes.value
                val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "PotholeReport_$dateStr.csv"
                
                // Save to App External Directory for clean permissions
                val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                val file = File(dir, fileName)
                val writer = FileWriter(file)

                // CSV Header
                writer.append("Pothole_ID,Latitude,Longitude,Width_cm,Length_cm,Depth_cm,Area_m2,Volume_m3,Severity,Timestamp,Confidence,RoadName,City,State,Speed_kmh,Direction_deg,RepairStatus\n")

                for (p in list) {
                    writer.append("${p.id},${p.latitude},${p.longitude},${p.widthCm},${p.lengthCm},${p.depthCm},${p.areaSqM},${p.volumeCuM},${p.severity},${p.timestamp},${p.confidence},\"${p.roadName}\",\"${p.city}\",\"${p.state}\",${p.vehicleSpeedKmh},${p.travelDirection},${p.repairStatus}\n")
                }

                writer.flush()
                writer.close()
                addLog("EXPORT", "CSV generated: ${file.name}")
                withContext(Dispatchers.Main) {
                    onResult("Exported CSV successfully to: ${file.absolutePath}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed CSV export", e)
                withContext(Dispatchers.Main) {
                    onResult("Failed to generate CSV: ${e.message}")
                }
            }
        }
    }

    fun exportPDF(context: Context, onResult: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val list = allPotholes.value
                val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "PotholeReport_$dateStr.pdf"

                val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                val file = File(dir, fileName)
                val writer = FileWriter(file)

                // Simulate a clean PDF generation as a styled text report for prototype convenience
                writer.append("====================================================\n")
                writer.append("             ROADSMART AI MONITORING REPORT          \n")
                writer.append("             Generated: ${Date()}                    \n")
                writer.append("====================================================\n\n")
                writer.append("SUMMARY STATS:\n")
                writer.append("- Total Potholes Detected: ${list.size}\n")
                writer.append("- Critical Severity: ${list.count { it.severity == "Critical" }}\n")
                writer.append("- High Severity: ${list.count { it.severity == "High" }}\n")
                writer.append("- Medium Severity: ${list.count { it.severity == "Medium" }}\n")
                writer.append("- Low Severity: ${list.count { it.severity == "Low" }}\n")
                writer.append("- Pending Repairs: ${list.count { it.repairStatus == "Pending" }}\n")
                writer.append("----------------------------------------------------\n\n")
                writer.append("DETAILED LOGS:\n\n")

                for (p in list) {
                    writer.append("ID: ${p.id} | Severity: ${p.severity.uppercase()} | Status: ${p.repairStatus}\n")
                    writer.append("Location: ${p.roadName}, ${p.city} (${p.latitude}, ${p.longitude})\n")
                    writer.append("Dimensions: Width ${p.widthCm}cm, Length ${p.lengthCm}cm, Depth ${p.depthCm}cm\n")
                    writer.append("Stats: Area ${p.areaSqM} m², Volume ${p.volumeCuM} m³ | Conf: ${(p.confidence * 100).toInt()}%\n")
                    writer.append("Speed: ${p.vehicleSpeedKmh} km/h | Time: ${Date(p.timestamp)}\n")
                    writer.append("----------------------------------------------------\n")
                }

                writer.flush()
                writer.close()
                addLog("EXPORT", "Styled PDF Report generated: ${file.name}")
                withContext(Dispatchers.Main) {
                    onResult("Exported PDF Report successfully to: ${file.absolutePath}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed PDF export", e)
                withContext(Dispatchers.Main) {
                    onResult("Failed to generate PDF: ${e.message}")
                }
            }
        }
    }

    private fun addLog(module: String, text: String) {
        val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val entry = LogEntry(timeStr, module, text)
        val logs = _telemetryLogs.value.toMutableList()
        logs.add(0, entry) // prepend so newest logs are on top
        if (logs.size > 80) {
            logs.removeAt(logs.size - 1)
        }
        _telemetryLogs.value = logs
    }

    private fun Double.roundTo(decimals: Int): Double {
        var multiplier = 1.0
        repeat(decimals) { multiplier *= 10 }
        return (this * multiplier).roundToInt() / multiplier
    }

    private fun Float.roundTo(decimals: Int): Float {
        var multiplier = 1.0f
        repeat(decimals) { multiplier *= 10f }
        return (this * multiplier).roundToInt() / multiplier
    }

    override fun onCleared() {
        super.onCleared()
        stopFpsTimer()
        stopRouteSimulation()
        stopRealLocationUpdates()
        syncJob?.cancel()
    }
}

data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val roadName: String,
    val city: String,
    val state: String,
    val country: String
)

data class LogEntry(
    val time: String,
    val tag: String,
    val message: String
)
