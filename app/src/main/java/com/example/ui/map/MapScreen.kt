package com.example.ui.map

import com.example.ui.theme.*
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.PotholeEntity
import com.example.ui.PotholeViewModel
import com.example.ui.components.VectorMap

@Composable
fun MapScreen(
    viewModel: PotholeViewModel,
    modifier: Modifier = Modifier
) {
    val potholes by viewModel.allPotholes.collectAsState()
    val currentLocation by viewModel.currentLocation.collectAsState()
    val travelDirection by viewModel.travelDirection.collectAsState()
    val historyPath by viewModel.vehicleHistoryPath.collectAsState()

    var selectedPothole by remember { mutableStateOf<PotholeEntity?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Interactive Vector Map Component
        VectorMap(
            potholes = potholes,
            vehicleLat = currentLocation.latitude,
            vehicleLon = currentLocation.longitude,
            vehicleBearing = travelDirection,
            historyPath = historyPath,
            selectedPothole = selectedPothole,
            onPotholeSelected = { selectedPothole = it },
            modifier = Modifier.fillMaxSize()
        )

        // Floating Control Compass indicator
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .statusBarsPadding()
                .size(44.dp)
                .background(Color.White.copy(alpha = 0.9f), CircleShape)
                .border(1.dp, BorderGray, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Navigation, contentDescription = null, tint = PurpleMain, modifier = Modifier.size(20.dp))
        }

        // 2. Sliding Bottom Details Card Overlay
        AnimatedVisibility(
            visible = selectedPothole != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
                .navigationBarsPadding()
        ) {
            selectedPothole?.let { pothole ->
                // Make sure the state stays up-to-date if database is updated
                val activePothole = potholes.find { it.id == pothole.id } ?: pothole

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderGray, RoundedCornerShape(24.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Header section
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = activePothole.severity.uppercase(),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Black,
                                        color = when (activePothole.severity) {
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
                                            text = "ID: ${activePothole.id}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = CharcoalDark,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(activePothole.roadName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = CharcoalDark)
                            }

                            IconButton(
                                onClick = { selectedPothole = null },
                                modifier = Modifier.background(BorderGray, CircleShape).size(30.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = CharcoalDark, modifier = Modifier.size(16.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Captured Image illustration
                            Box(
                                modifier = Modifier
                                    .size(110.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CreamBg)
                                    .border(1.dp, BorderGray, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                // Draw a custom styled vector representation of a pothole crater as our image backup!
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    drawCircle(Color.White, radius = 40f)
                                    drawCircle(
                                        color = when (activePothole.severity) {
                                            "Critical" -> AlertRed
                                            "High" -> Color(0xFFD97706)
                                            "Medium" -> Color(0xFFB45309)
                                            else -> Color(0xFF15803D)
                                        }.copy(alpha = 0.5f),
                                        radius = 42f,
                                        style = Stroke(width = 4f)
                                    )
                                }
                                Text("CAMERA\nSHOT", style = MaterialTheme.typography.labelSmall, color = SubtitleText, fontWeight = FontWeight.Bold)
                            }

                            // Measurements info
                            Column(modifier = Modifier.weight(1f)) {
                                Text("STEREOSCOPIC DIMENSIONS", style = MaterialTheme.typography.labelSmall, color = SubtitleText, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Width: ${activePothole.widthCm} cm", style = MaterialTheme.typography.bodyMedium, color = CharcoalDark)
                                Text("Length: ${activePothole.lengthCm} cm", style = MaterialTheme.typography.bodyMedium, color = CharcoalDark)
                                Text("Depth: ${activePothole.depthCm} cm", style = MaterialTheme.typography.bodyMedium, color = CharcoalDark)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Surface Area: ${activePothole.areaSqM} m²", style = MaterialTheme.typography.bodySmall, color = PurpleMain, fontWeight = FontWeight.Bold)
                                Text("Damage Volume: ${activePothole.volumeCuM} m³", style = MaterialTheme.typography.bodySmall, color = PurpleMain, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Status Badge Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("REPAIR STATUS:", style = MaterialTheme.typography.labelSmall, color = SubtitleText, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(6.dp))
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = when (activePothole.repairStatus) {
                                            "Repaired" -> Color(0xFFDCFCE7)
                                            "Scheduled" -> Color(0xFFDBEAFE)
                                            else -> AlertRedBg
                                        }
                                    ),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = activePothole.repairStatus.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = when (activePothole.repairStatus) {
                                            "Repaired" -> Color(0xFF15803D)
                                            "Scheduled" -> Color(0xFF1E40AF)
                                            else -> AlertRed
                                        },
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Text(
                                text = "Confidence: ${(activePothole.confidence * 100).toInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF15803D)
                            )
                        }

                        Divider(modifier = Modifier.padding(vertical = 12.dp), color = BorderGray)

                        // Quick Actions to manage maintenance (Req 14)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (activePothole.repairStatus != "Scheduled") {
                                Button(
                                    onClick = { viewModel.updateRepairStatus(activePothole.id, "Scheduled") },
                                    colors = ButtonDefaults.buttonColors(containerColor = PurpleMain),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).testTag("schedule_repair")
                                ) {
                                    Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Schedule Fix", style = MaterialTheme.typography.labelMedium, color = Color.White)
                                }
                            }

                            if (activePothole.repairStatus != "Repaired") {
                                Button(
                                    onClick = { viewModel.updateRepairStatus(activePothole.id, "Repaired") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDCFCE7)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).testTag("mark_repaired")
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF15803D), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Complete Repair", style = MaterialTheme.typography.labelMedium, color = Color(0xFF15803D))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun Modifier.size(size: Int) = this.then(
    Modifier.size(size.dp)
)
