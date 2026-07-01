package com.example.ui.potholes

import com.example.ui.theme.*
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.PotholeEntity
import com.example.ui.PotholeViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PotholeListScreen(
    viewModel: PotholeViewModel,
    onNavigateToMapWithSelection: (PotholeEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val potholes by viewModel.allPotholes.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedSeverityFilter by remember { mutableStateOf<String?>(null) }
    var selectedStatusFilter by remember { mutableStateOf<String?>(null) }

    // Filter results dynamically
    val filteredPotholes = potholes.filter { pothole ->
        val matchesSearch = pothole.roadName.contains(searchQuery, ignoreCase = true) ||
                pothole.city.contains(searchQuery, ignoreCase = true) ||
                pothole.id.contains(searchQuery, ignoreCase = true)

        val matchesSeverity = selectedSeverityFilter == null || pothole.severity == selectedSeverityFilter
        val matchesStatus = selectedStatusFilter == null || pothole.repairStatus == selectedStatusFilter

        matchesSearch && matchesSeverity && matchesStatus
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CreamBg)
    ) {
        // Search & Filter Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "ROAD REPAIR INDEX",
                style = MaterialTheme.typography.labelSmall,
                color = PurpleMain,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Pothole Registry",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = CharcoalDark
            )

            // Search Text Field
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by street name, city, or ID...", color = SubtitleText) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SubtitleText) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = null, tint = CharcoalDark)
                        }
                    }
                },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedTextColor = CharcoalDark,
                    unfocusedTextColor = CharcoalDark,
                    focusedIndicatorColor = PurpleMain,
                    unfocusedIndicatorColor = BorderGray
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderGray, RoundedCornerShape(12.dp))
                    .testTag("search_field")
            )

            // Severity Filter Chips
            Text("Filter Severity:", style = MaterialTheme.typography.labelSmall, color = SubtitleText, fontWeight = FontWeight.Bold)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val severities = listOf("Critical", "High", "Medium", "Low")
                items(severities) { severity ->
                    val isSelected = selectedSeverityFilter == severity
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedSeverityFilter = if (isSelected) null else severity },
                        label = { Text(severity, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color.White,
                            labelColor = SubtitleText,
                            selectedContainerColor = when(severity) {
                                "Critical" -> AlertRedBg
                                "High" -> Color(0xFFFEF3C7)
                                "Medium" -> Color(0xFFFEF9C3)
                                else -> Color(0xFFDCFCE7)
                            },
                            selectedLabelColor = when(severity) {
                                "Critical" -> AlertRed
                                "High" -> Color(0xFFD97706)
                                "Medium" -> Color(0xFFB45309)
                                else -> Color(0xFF15803D)
                            }
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = BorderGray,
                            selectedBorderColor = when(severity) {
                                "Critical" -> AlertRed
                                "High" -> Color(0xFFD97706)
                                "Medium" -> Color(0xFFB45309)
                                else -> Color(0xFF15803D)
                            },
                            borderWidth = 1.dp,
                            selectedBorderWidth = 1.5.dp
                        ),
                        modifier = Modifier.testTag("chip_severity_$severity")
                    )
                }
            }

            // Repair Status Filter Chips
            Text("Filter Repair Status:", style = MaterialTheme.typography.labelSmall, color = SubtitleText, fontWeight = FontWeight.Bold)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val statuses = listOf("Pending", "Scheduled", "Repaired")
                items(statuses) { status ->
                    val isSelected = selectedStatusFilter == status
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedStatusFilter = if (isSelected) null else status },
                        label = { Text(status, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color.White,
                            labelColor = SubtitleText,
                            selectedContainerColor = when(status) {
                                "Repaired" -> Color(0xFFDCFCE7)
                                "Scheduled" -> Color(0xFFDBEAFE)
                                else -> AlertRedBg
                            },
                            selectedLabelColor = when(status) {
                                "Repaired" -> Color(0xFF15803D)
                                "Scheduled" -> Color(0xFF1E40AF)
                                else -> AlertRed
                            }
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = BorderGray,
                            selectedBorderColor = when(status) {
                                "Repaired" -> Color(0xFF15803D)
                                "Scheduled" -> Color(0xFF1E40AF)
                                else -> AlertRed
                            },
                            borderWidth = 1.dp,
                            selectedBorderWidth = 1.5.dp
                        ),
                        modifier = Modifier.testTag("chip_status_$status")
                    )
                }
            }
        }

        Divider(color = BorderGray)

        // 3. Scrollable List of Pothole Cards
        Box(modifier = Modifier.weight(1f)) {
            if (filteredPotholes.isEmpty()) {
                // Empty state placeholders
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterListOff,
                        contentDescription = null,
                        tint = SubtitleText,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No potholes match criteria",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CharcoalDark
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Try adjusting your search queries or clearing active filters to view all entries.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = BodyText,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredPotholes, key = { it.id }) { pothole ->
                        PotholeCard(
                            pothole = pothole,
                            onClick = { onNavigateToMapWithSelection(pothole) },
                            onDelete = { viewModel.deletePothole(pothole) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PotholeCard(
    pothole: PotholeEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(pothole.timestamp))

    val severityColor = when (pothole.severity) {
        "Critical" -> AlertRed
        "High" -> Color(0xFFD97706)
        "Medium" -> Color(0xFFB45309)
        else -> Color(0xFF15803D)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderGray, RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            // Severity left accent bar
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(severityColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp)
            ) {
                // Top row: Street name and Delete action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = pothole.roadName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CharcoalDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = SubtitleText, modifier = Modifier.size(16.dp))
                    }
                }

                Text(
                    text = "ID: ${pothole.id} • Registered: $dateStr",
                    style = MaterialTheme.typography.labelSmall,
                    color = BodyText,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Dimension table layout
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column {
                        Text("WIDTH", style = MaterialTheme.typography.labelSmall, color = SubtitleText)
                        Text("${pothole.widthCm} cm", style = MaterialTheme.typography.bodySmall, color = CharcoalDark, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("LENGTH", style = MaterialTheme.typography.labelSmall, color = SubtitleText)
                        Text("${pothole.lengthCm} cm", style = MaterialTheme.typography.bodySmall, color = CharcoalDark, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("DEPTH", style = MaterialTheme.typography.labelSmall, color = SubtitleText)
                        Text("${pothole.depthCm} cm", style = MaterialTheme.typography.bodySmall, color = CharcoalDark, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Status badges footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Severity chip
                        Card(
                            colors = CardDefaults.cardColors(containerColor = severityColor.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.border(0.5.dp, severityColor, RoundedCornerShape(4.dp))
                        ) {
                            Text(
                                text = pothole.severity.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = severityColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        // Repair status chip
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = when(pothole.repairStatus) {
                                    "Repaired" -> Color(0xFFDCFCE7)
                                    "Scheduled" -> Color(0xFFDBEAFE)
                                    else -> AlertRedBg
                                }
                            ),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = pothole.repairStatus.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = when(pothole.repairStatus) {
                                    "Repaired" -> Color(0xFF15803D)
                                    "Scheduled" -> Color(0xFF1E40AF)
                                    else -> AlertRed
                                },
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Cloud Sync Badge
                    Icon(
                        imageVector = if (pothole.isSynced) Icons.Default.CloudDone else Icons.Default.CloudOff,
                        contentDescription = null,
                        tint = if (pothole.isSynced) Color(0xFF15803D) else SubtitleText,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
