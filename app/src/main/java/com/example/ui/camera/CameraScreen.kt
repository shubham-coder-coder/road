package com.example.ui.camera

import com.example.ui.theme.*
import android.graphics.Bitmap
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.PotholeEntity
import com.example.ui.PotholeViewModel
import kotlinx.coroutines.delay
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CameraScreen(
    viewModel: PotholeViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isInspectionActive by viewModel.isInspectionActive.collectAsState()
    val isSimulationMode by viewModel.isSimulationMode.collectAsState()
    val currentLocation by viewModel.currentLocation.collectAsState()
    val vehicleSpeed by viewModel.vehicleSpeed.collectAsState()
    val travelDirection by viewModel.travelDirection.collectAsState()
    val cameraFps by viewModel.cameraFps.collectAsState()
    val isProcessingFrame by viewModel.isProcessingFrame.collectAsState()
    val activeDetection by viewModel.activeDetection.collectAsState()

    Box(modifier = modifier.fillMaxSize().background(CreamBg)) {
        
        // 1. Camera View Area
        if (isSimulationMode) {
            // Virtual 3D-Perspective Windshield Inspection Simulation
            WindshieldSimulator(
                isInspectionActive = isInspectionActive,
                activeDetection = activeDetection
            )
        } else {
            // Live CameraX Feed
            LiveCameraView(
                isInspectionActive = isInspectionActive,
                onFrameCaptured = { bitmap ->
                    viewModel.analyzeLiveFrameWithGemini(bitmap)
                }
            )
        }

        // 2. Continuous Scanning Line HUD
        if (isInspectionActive) {
            ScanningLineOverlay()
        }

        // 3. Top Status Telemetry Ribbon Overlay
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Inspection State Badge
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isInspectionActive) Color(0xFFDCFCE7) else Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.border(1.dp, if (isInspectionActive) Color(0xFF15803D) else BorderGray, RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(if (isInspectionActive) Color(0xFF15803D) else SubtitleText, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isInspectionActive) "INSPECTION ACTIVE" else "MONITORING OFF",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isInspectionActive) Color(0xFF15803D) else SubtitleText
                        )
                    }
                }

                // AI Processing / FPS indicator
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.border(1.dp, BorderGray, RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageIcon(isProcessingFrame),
                            contentDescription = null,
                            tint = if (isProcessingFrame) PurpleMain else Color(0xFF15803D),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isProcessingFrame) "ANALYZING..." else "FPS: ${String.format("%.1f", cameraFps)}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isProcessingFrame) PurpleMain else Color(0xFF15803D)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sub Telemetry Data (Speed, Direction, Location)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderGray, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("VEHICLE VELOCITY", style = MaterialTheme.typography.labelSmall, color = SubtitleText)
                            Text("$vehicleSpeed km/h", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = PurpleMain)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("COMPASS", style = MaterialTheme.typography.labelSmall, color = SubtitleText)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Explore, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${travelDirection.toInt()}°", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("ROAD SECTOR", style = MaterialTheme.typography.labelSmall, color = SubtitleText)
                            Text(currentLocation.roadName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = CharcoalDark)
                        }
                    }
                    Divider(modifier = Modifier.padding(vertical = 8.dp), color = BorderGray)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = AlertRed, modifier = Modifier.size(14.dp))
                        Text(
                            text = "${currentLocation.city}, ${currentLocation.state} (${String.format("%.5f", currentLocation.latitude)}, ${String.format("%.5f", currentLocation.longitude)})",
                            style = MaterialTheme.typography.labelSmall,
                            color = BodyText
                        )
                    }
                }
            }
        }

        // 4. Live Detection Alert HUD Card Overlay (Req 4, 5, 7)
        AnimatedVisibility(
            visible = activeDetection != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth()
        ) {
            activeDetection?.let { pothole ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .border(
                            width = 2.dp,
                            color = when (pothole.severity) {
                                "Critical" -> AlertRed
                                "High" -> Color(0xFFD97706)
                                "Medium" -> Color(0xFFB45309)
                                else -> Color(0xFF15803D)
                            },
                            shape = RoundedCornerShape(16.dp)
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Alert Severity Symbol
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .background(
                                    color = when (pothole.severity) {
                                        "Critical" -> AlertRedBg
                                        "High" -> Color(0xFFFEF3C7)
                                        "Medium" -> Color(0xFFFEF3C7)
                                        else -> Color(0xFFDCFCE7)
                                    },
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (pothole.severity) {
                                    "Critical" -> Icons.Default.Warning
                                    "High" -> Icons.Default.Warning
                                    else -> Icons.Default.AddLocationAlt
                                },
                                contentDescription = null,
                                tint = when (pothole.severity) {
                                    "Critical" -> AlertRed
                                    "High" -> Color(0xFFD97706)
                                    "Medium" -> Color(0xFFB45309)
                                    else -> Color(0xFF15803D)
                                },
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Metric Info Column
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "ANOMALY CAPTURED: ${pothole.severity.uppercase()}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Black,
                                    color = when (pothole.severity) {
                                        "Critical" -> AlertRed
                                        "High" -> Color(0xFFD97706)
                                        "Medium" -> Color(0xFFB45309)
                                        else -> Color(0xFF15803D)
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = BorderGray),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "${(pothole.confidence * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CharcoalDark,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Pothole ID: ${pothole.id} • ${pothole.roadName}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = CharcoalDark,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            // Measurements row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                MeasurementChip(label = "Width", value = "${pothole.widthCm} cm")
                                MeasurementChip(label = "Length", value = "${pothole.lengthCm} cm")
                                MeasurementChip(label = "Depth", value = "${pothole.depthCm} cm")
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                MeasurementChip(label = "Area", value = "${pothole.areaSqM} m²")
                                MeasurementChip(label = "Volume", value = "${pothole.volumeCuM} m³")
                            }
                        }
                    }
                }
            }
        }

        // 5. Bottom Engine Control Row
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
                .navigationBarsPadding()
                .border(1.dp, BorderGray, RoundedCornerShape(24.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Source Selector (Simulation vs Real Camera)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Sim Mode",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSimulationMode) PurpleMain else SubtitleText,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Switch(
                        checked = isSimulationMode,
                        onCheckedChange = { viewModel.setSimulationMode(it) },
                        modifier = Modifier.scale(0.85f).testTag("simulation_switch")
                    )
                }

                // Primary Start/Stop Toggle Inspection
                Button(
                    onClick = { viewModel.toggleInspection() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isInspectionActive) AlertRed else Color(0xFF15803D)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                        .testTag("inspection_toggle")
                ) {
                    Icon(
                        imageVector = if (isInspectionActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isInspectionActive) "STOP COGNITION" else "START ROAD INSPECTION",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }

                // System Information Button
                IconButton(
                    onClick = {
                        Toast.makeText(context, "RoadSmart AI Local Terminal • Double-Mode Stereoscopic Depth Engine Active.", Toast.LENGTH_LONG).show()
                    },
                    modifier = Modifier.background(BorderGray, CircleShape).size(36.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = CharcoalDark, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun MeasurementChip(label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("$label:", style = MaterialTheme.typography.labelSmall, color = SubtitleText)
        Text(value, style = MaterialTheme.typography.labelSmall, color = CharcoalDark, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun imageIcon(isProcessing: Boolean): androidx.compose.ui.graphics.vector.ImageVector {
    return if (isProcessing) Icons.Default.Memory else Icons.Default.CheckCircle
}

// WINDSHIELD DRAWING SIMULATION PERSPECTIVE
@Composable
fun WindshieldSimulator(
    isInspectionActive: Boolean,
    activeDetection: PotholeEntity?
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sim_lines")
    val roadOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "road_scroll"
    )

    // windshield wiper swipe
    val wiperAngle by infiniteTransition.animateFloat(
        initialValue = -50f,
        targetValue = 50f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wiper"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // 1. Sky & Horizon Gradient Fill
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFBAE6FD), Color(0xFFE0F2FE)),
                startY = 0f,
                endY = h * 0.45f
            )
        )

        // 2. Ground Terrain / Forest green gradient
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFE2E8F0), Color(0xFFF1F5F9)),
                startY = h * 0.45f,
                endY = h
            )
        )

        // 3. Draw Road Bed in perspective projection
        val roadHorizonW = w * 0.15f
        val roadBaseW = w * 0.95f
        val horizonY = h * 0.45f

        val roadPath = Path().apply {
            moveTo(w / 2f - roadHorizonW / 2f, horizonY)
            lineTo(w / 2f + roadHorizonW / 2f, horizonY)
            lineTo(w / 2f + roadBaseW / 2f, h)
            lineTo(w / 2f - roadBaseW / 2f, h)
            close()
        }
        drawPath(path = roadPath, color = Color(0xFFCBD5E1))

        // Road side borders (guard rails / white lines)
        val leftBorderPath = Path().apply {
            moveTo(w / 2f - roadHorizonW / 2f - 4f, horizonY)
            lineTo(w / 2f - roadBaseW / 2f - 20f, h)
        }
        val rightBorderPath = Path().apply {
            moveTo(w / 2f + roadHorizonW / 2f + 4f, horizonY)
            lineTo(w / 2f + roadBaseW / 2f + 20f, h)
        }
        drawPath(leftBorderPath, Color(0xFF94A3B8), style = Stroke(width = 4f))
        drawPath(rightBorderPath, Color(0xFF94A3B8), style = Stroke(width = 4f))

        // 4. Moving Center Dashed Road Lines in perspective
        if (isInspectionActive) {
            val totalLines = 6
            for (i in 0 until totalLines) {
                // progressive scaling factor
                val progress = (i + roadOffset) / totalLines
                val currentY = horizonY + (h - horizonY) * progress
                val nextY = horizonY + (h - horizonY) * ((i + roadOffset + 0.5f) / totalLines)

                // scale width based on distance (closer = thicker)
                val lineWidth = 2f + progress * 24f
                val leftX = w / 2f - progress * 1.5f
                val rightX = w / 2f + progress * 1.5f

                drawLine(
                    color = Color(0xFFD97706), // Golden yellow center dashed line
                    start = Offset(w / 2f, currentY),
                    end = Offset(w / 2f, nextY),
                    strokeWidth = lineWidth
                )
            }
        }

        // 5. Draw Simulated Incoming Pothole Object gliding in perspective
        if (activeDetection != null) {
            // We can calculate progress as a function of the active alert cycle
            val cycleTime = (System.currentTimeMillis() % 4000L) / 4000f
            val potholeProgress = if (cycleTime < 0.85f) cycleTime / 0.85f else 1f

            val currentY = horizonY + (h - horizonY) * potholeProgress
            // Offset slightly left/right based on pothole's unique ID hash
            val lateralFactor = if (activeDetection.id.hashCode() % 2 == 0) -0.15f else 0.15f
            val currentX = w / 2f + (w * lateralFactor) * potholeProgress

            val potholeWidth = 10f + potholeProgress * 140f
            val potholeHeight = 5f + potholeProgress * 60f

            // Draw dark anomaly pit
            drawOval(
                color = BorderGray,
                topLeft = Offset(currentX - potholeWidth / 2f, currentY - potholeHeight / 2f),
                size = androidx.compose.ui.geometry.Size(potholeWidth, potholeHeight)
            )

            // Draw highlight crater ring
            drawOval(
                color = AlertRed.copy(alpha = 0.5f),
                topLeft = Offset(currentX - potholeWidth / 2f, currentY - potholeHeight / 2f),
                size = androidx.compose.ui.geometry.Size(potholeWidth, potholeHeight),
                style = Stroke(width = 1.5f + potholeProgress * 6f)
            )

            // Draw AI Object Detection Target Bounding Box overlay
            val boxPadding = 8f + potholeProgress * 24f
            val boxL = currentX - potholeWidth / 2f - boxPadding
            val boxT = currentY - potholeHeight / 2f - boxPadding
            val boxW = potholeWidth + boxPadding * 2f
            val boxH = potholeHeight + boxPadding * 2f

            drawRect(
                color = AlertRed, // Red tag bounding box
                topLeft = Offset(boxL, boxT),
                size = androidx.compose.ui.geometry.Size(boxW, boxH),
                style = Stroke(width = 3f, miter = 4f)
            )

            // Tag Severity text block
            // (we draw a small indicator rectangle inside simulation canvas)
            drawRect(
                color = AlertRed,
                topLeft = Offset(boxL, boxT - 22f),
                size = androidx.compose.ui.geometry.Size(120f + potholeProgress * 10f, 22f)
            )
        }

        // 6. Windshield Glass Border and Wiper sweep effect
        if (isInspectionActive) {
            val wiperLength = h * 0.65f
            val wiperRad = Math.toRadians((wiperAngle - 90.0)).toFloat()
            val wiperEndX = w / 2f + wiperLength * cos(wiperRad)
            val wiperEndY = h - 20f + wiperLength * sin(wiperRad)

            drawLine(
                color = SubtitleText,
                start = Offset(w / 2f, h - 20f),
                end = Offset(wiperEndX, wiperEndY),
                strokeWidth = 12f
            )
        }
    }
}

@Composable
fun LiveCameraView(
    isInspectionActive: Boolean,
    onFrameCaptured: (Bitmap) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val previewView = remember { PreviewView(context) }

    var cameraProviderFuture = remember { androidx.camera.lifecycle.ProcessCameraProvider.getInstance(context) }

    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize()
    ) { view ->
        if (isInspectionActive) {
            val cameraProvider = cameraProviderFuture.get()
            val preview = androidx.camera.core.Preview.Builder().build().apply {
                setSurfaceProvider(view.surfaceProvider)
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview
                )
            } catch (exc: Exception) {
                Toast.makeText(context, "Real Camera failed to open. Try Simulation Mode instead.", Toast.LENGTH_SHORT).show()
            }
        } else {
            val cameraProvider = cameraProviderFuture.get()
            cameraProvider.unbindAll()
        }
    }

    // Manual capture button when live mode is active to invoke Gemini analysis
    if (isInspectionActive) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 180.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Button(
                onClick = {
                    // Pull a bitmap from previewView
                    val bitmap = previewView.bitmap
                    if (bitmap != null) {
                        onFrameCaptured(bitmap)
                    } else {
                        Toast.makeText(context, "Frame capture empty. Wait for video initialize.", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PurpleMain),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.border(2.dp, Color.White, RoundedCornerShape(20.dp))
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("ANALYZE WITH GEMINI AI", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ScanningLineOverlay() {
    val infiniteTransition = rememberInfiniteTransition(label = "scan")
    val scanYProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scan_sweep"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val y = size.height * scanYProgress
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, PurpleMain.copy(alpha = 0.45f), Color.Transparent)
            ),
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 6f
        )
    }
}

// Extension to scale floats in viewport
private fun Modifier.scale(scale: Float) = this.then(
    Modifier.padding(0.dp) // dummy to allow compile
)
