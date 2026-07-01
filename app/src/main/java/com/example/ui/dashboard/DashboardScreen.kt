package com.example.ui.dashboard

import com.example.ui.theme.*
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.PotholeEntity
import com.example.ui.PotholeViewModel

@Composable
fun DashboardScreen(
    viewModel: PotholeViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val potholes by viewModel.allPotholes.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val logs by viewModel.telemetryLogs.collectAsState()

    // Aggregate statistics
    val totalCount = potholes.size
    val criticalCount = potholes.count { it.severity == "Critical" }
    val highCount = potholes.count { it.severity == "High" }
    val mediumCount = potholes.count { it.severity == "Medium" }
    val lowCount = potholes.count { it.severity == "Low" }

    val pendingCount = potholes.count { it.repairStatus == "Pending" }
    val scheduledCount = potholes.count { it.repairStatus == "Scheduled" }
    val repairedCount = potholes.count { it.repairStatus == "Repaired" }
    val syncedCount = potholes.count { it.isSynced }

    var showClearDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CreamBg)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Header Banner
        item {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = "CONTROL DESK & PORTAL",
                    style = MaterialTheme.typography.labelSmall,
                    color = PurpleMain,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Edge Analytics Engine",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = CharcoalDark
                )
            }
        }

        // 1. Core Statistics Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "TOTAL DETECTED",
                    value = "$totalCount",
                    subtext = "${syncedCount} Synced to Cloud",
                    icon = Icons.Default.GridView,
                    color = PurpleMain,
                    modifier = Modifier.weight(1f).testTag("stat_total")
                )
                StatCard(
                    title = "CRITICAL ALERTS",
                    value = "$criticalCount",
                    subtext = "Immediate action needed",
                    icon = Icons.Default.Warning,
                    color = AlertRed,
                    modifier = Modifier.weight(1f).testTag("stat_critical")
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "REPAIRED SECTORS",
                    value = "$repairedCount",
                    subtext = "$scheduledCount Scheduled",
                    icon = Icons.Default.TaskAlt,
                    color = Color(0xFF15803D),
                    modifier = Modifier.weight(1f).testTag("stat_repaired")
                )
                StatCard(
                    title = "CLOUD PERSIST",
                    value = "${if (totalCount > 0) ((syncedCount.toFloat()/totalCount)*100).toInt() else 100}%",
                    subtext = if (isSyncing) "Handshaking..." else "Offline sync standby",
                    icon = Icons.Default.CloudUpload,
                    color = PurpleMain,
                    modifier = Modifier.weight(1f).testTag("stat_sync")
                )
            }
        }

        // 2. Charts & Data Distribution
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderGray, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "DAMAGE SEVERITY RATIOS",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = CharcoalDark
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Horizontal Custom Bars
                    SeverityBar(label = "Critical (depth >10cm)", count = criticalCount, total = totalCount, color = AlertRed)
                    Spacer(modifier = Modifier.height(10.dp))
                    SeverityBar(label = "High (depth 6-10cm)", count = highCount, total = totalCount, color = Color(0xFFD97706))
                    Spacer(modifier = Modifier.height(10.dp))
                    SeverityBar(label = "Medium (depth 3-6cm)", count = mediumCount, total = totalCount, color = Color(0xFFB45309))
                    Spacer(modifier = Modifier.height(10.dp))
                    SeverityBar(label = "Low (depth <3cm)", count = lowCount, total = totalCount, color = Color(0xFF15803D))
                }
            }
        }

        // Repair Metrics circular status doughnut
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderGray, RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1.2f)) {
                        Text(
                            text = "REPAIR RESOLUTIONS",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = CharcoalDark
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).background(Color(0xFF15803D), CircleShape))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Repaired: $repairedCount", style = MaterialTheme.typography.labelSmall, color = SubtitleText)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).background(Color(0xFF1E40AF), CircleShape))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Scheduled: $scheduledCount", style = MaterialTheme.typography.labelSmall, color = SubtitleText)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).background(SubtitleText, CircleShape))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pending: $pendingCount", style = MaterialTheme.typography.labelSmall, color = SubtitleText)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(90.dp)) {
                            val strokeWidth = 14f
                            val totalFloat = totalCount.toFloat()
                            val repairPct = if (totalFloat > 0) (repairedCount / totalFloat) * 360f else 0f
                            val schedPct = if (totalFloat > 0) (scheduledCount / totalFloat) * 360f else 0f

                            // Pending base background
                            drawArc(
                                color = BorderGray,
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = strokeWidth)
                            )

                            // Scheduled arc
                            if (schedPct > 0) {
                                drawArc(
                                    color = Color(0xFF1E40AF),
                                    startAngle = -90f,
                                    sweepAngle = schedPct,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth)
                                )
                            }

                            // Repaired arc
                            if (repairPct > 0) {
                                drawArc(
                                    color = Color(0xFF15803D),
                                    startAngle = -90f + schedPct,
                                    sweepAngle = repairPct,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth)
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val resPct = if (totalCount > 0) ((repairedCount.toFloat() / totalCount) * 100).toInt() else 0
                            Text("$resPct%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = CharcoalDark)
                            Text("DONE", style = MaterialTheme.typography.labelSmall, color = SubtitleText)
                        }
                    }
                }
            }
        }

        // 3. Live Log Terminal Feed (Req 14)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .border(1.dp, BorderGray, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(6.dp).background(AlertRed, CircleShape))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("LIVE LOG CONSOLE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = AlertRed)
                        }
                        Text("inspection_stream.sh", style = MaterialTheme.typography.labelSmall, color = SubtitleText, fontFamily = FontFamily.Monospace)
                    }
                    Divider(modifier = Modifier.padding(vertical = 8.dp), color = BorderGray)

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (logs.isEmpty()) {
                            item {
                                Text(
                                    text = "Awaiting cognition engine launch to stream frames...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SubtitleText,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        } else {
                            items(logs) { log ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(text = "[${log.time}]", style = MaterialTheme.typography.labelSmall, color = SubtitleText, fontFamily = FontFamily.Monospace)
                                    Text(
                                        text = "${log.tag}: ${log.message}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = when(log.tag) {
                                            "DETECTION" -> Color(0xFFD97706)
                                            "MERGE" -> PurpleMain
                                            "AI_CV" -> Color(0xFF1E3A8A)
                                            "SYNC" -> Color(0xFF15803D)
                                            "GPS_ERROR" -> AlertRed
                                            else -> BodyText
                                        },
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Downloadable Reports Action Row (Req 15)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderGray, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "DOWNLOAD EXPORTS & MAINTENANCE",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = CharcoalDark
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.exportPDF(context) { msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("export_pdf")
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = AlertRed, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("PDF Report", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = CharcoalDark)
                        }

                        Button(
                            onClick = {
                                viewModel.exportCSV(context) { msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("export_csv")
                        ) {
                            Icon(Icons.Default.TableChart, contentDescription = null, tint = Color(0xFF15803D), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Excel CSV", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = CharcoalDark)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Database wipe button
                    Button(
                        onClick = { showClearDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEF2F2)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFFCA5A5), RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, tint = AlertRed)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Wipe Local Storage Logs", color = AlertRed, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Wipe Local Logs?", color = CharcoalDark) },
            text = { Text("Are you sure you want to clear the SQLite registry? This cannot be undone.", color = SubtitleText) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllPotholes()
                        showClearDialog = false
                    }
                ) {
                    Text("YES, WIPE", color = AlertRed, fontWeight = FontWeight.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("CANCEL", color = CharcoalDark)
                }
            },
            containerColor = Color.White
        )
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    subtext: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.border(1.dp, BorderGray, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = SubtitleText,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = CharcoalDark
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtext,
                style = MaterialTheme.typography.labelSmall,
                color = BodyText
            )
        }
    }
}

@Composable
fun SeverityBar(label: String, count: Int, total: Int, color: Color) {
    val ratio = if (total > 0) count.toFloat() / total else 0f
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = SubtitleText)
            Text("$count (${(ratio * 100).toInt()}%)", style = MaterialTheme.typography.labelSmall, color = CharcoalDark, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(BorderGray, RoundedCornerShape(4.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(ratio)
                    .background(color, RoundedCornerShape(4.dp))
            )
        }
    }
}
