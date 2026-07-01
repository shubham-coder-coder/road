package com.example.ui.components

import com.example.ui.theme.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import com.example.data.PotholeEntity
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun VectorMap(
    potholes: List<PotholeEntity>,
    vehicleLat: Double,
    vehicleLon: Double,
    vehicleBearing: Float,
    historyPath: List<Pair<Double, Double>>,
    selectedPothole: PotholeEntity?,
    onPotholeSelected: (PotholeEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    // Center point of the map (roughly center of San Francisco route)
    val mapCenterLat = 37.7820
    val mapCenterLon = -122.4060

    // Zoom and pan offsets
    var scale by remember { mutableStateOf(400000.0) } // maps lat/lon differences to pixels
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    // Convert GPS coordinates to local canvas coordinates
    fun gpsToCanvas(lat: Double, lon: Double, centerWidth: Float, centerHeight: Float): Offset {
        val x = ((lon - mapCenterLon) * scale).toFloat() + centerWidth + panOffset.x
        // Note: latitude decreases downwards in screen coords, so we negate it
        val y = -((lat - mapCenterLat) * scale).toFloat() + centerHeight + panOffset.y
        return Offset(x, y)
    }

    // Inverse converter to handle tap clicks on markers
    fun canvasToGps(offset: Offset, centerWidth: Float, centerHeight: Float): Pair<Double, Double> {
        val lon = ((offset.x - centerWidth - panOffset.x) / scale) + mapCenterLon
        val lat = -((offset.y - centerHeight - panOffset.y) / scale) + mapCenterLat
        return Pair(lat, lon)
    }

    Box(
        modifier = modifier
            .background(Color(0xFFF1EDE6)) // Elegant Warm Map Canvas Background
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    // Handle multi-touch zooming and panning
                    scale = (scale * zoom).coerceIn(150000.0, 900000.0)
                    panOffset += pan
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f

            // 1. Draw Simulated City Parks and Water Features
            // Sea / Bay area (top right)
            drawCircle(
                color = Color(0xFFBFDBFE),
                radius = 1200f,
                center = Offset(size.width + 100f, -100f)
            )

            // Draw Grid Lines (representing local streets)
            val streetGridPath = Path()
            // Horizontal and vertical virtual block lines
            for (i in -15..15) {
                val latLine = mapCenterLat + (i * 0.002)
                val lonLine = mapCenterLon + (i * 0.002)

                val startH = gpsToCanvas(latLine, mapCenterLon - 0.02, cx, cy)
                val endH = gpsToCanvas(latLine, mapCenterLon + 0.02, cx, cy)
                streetGridPath.moveTo(startH.x, startH.y)
                streetGridPath.lineTo(endH.x, endH.y)

                val startV = gpsToCanvas(mapCenterLat - 0.02, lonLine, cx, cy)
                val endV = gpsToCanvas(mapCenterLat + 0.02, lonLine, cx, cy)
                streetGridPath.moveTo(startV.x, startV.y)
                streetGridPath.lineTo(endV.x, endV.y)
            }
            drawPath(
                path = streetGridPath,
                color = Color(0xFFCBD5E1).copy(alpha = 0.5f),
                style = Stroke(width = 1.5f)
            )

            // 2. Draw Main Configured SF Route Streets
            val sfRoutePoints = listOf(
                Pair(37.77490, -122.41940),
                Pair(37.77612, -122.41710),
                Pair(37.77740, -122.41480),
                Pair(37.77885, -122.41240),
                Pair(37.78010, -122.41010),
                Pair(37.78125, -122.40780),
                Pair(37.78248, -122.40530),
                Pair(37.78380, -122.40300),
                Pair(37.78510, -122.40050),
                Pair(37.78650, -122.39810),
                Pair(37.78780, -122.39560),
                Pair(37.78910, -122.39310)
            )

            val primaryRoutePath = Path()
            sfRoutePoints.forEachIndexed { index, p ->
                val canvasPt = gpsToCanvas(p.first, p.second, cx, cy)
                if (index == 0) {
                    primaryRoutePath.moveTo(canvasPt.x, canvasPt.y)
                } else {
                    primaryRoutePath.lineTo(canvasPt.x, canvasPt.y)
                }
            }
            drawPath(
                path = primaryRoutePath,
                color = Color(0xFFCBD5E1),
                style = Stroke(width = 18f, miter = 4f)
            )
            drawPath(
                path = primaryRoutePath,
                color = Color.White,
                style = Stroke(width = 10f)
            )

            // 3. Draw Vehicle Travel History Trace
            if (historyPath.size > 1) {
                val tracePath = Path()
                historyPath.forEachIndexed { index, p ->
                    val canvasPt = gpsToCanvas(p.first, p.second, cx, cy)
                    if (index == 0) {
                        tracePath.moveTo(canvasPt.x, canvasPt.y)
                    } else {
                        tracePath.lineTo(canvasPt.x, canvasPt.y)
                    }
                }
                drawPath(
                    path = tracePath,
                    color = PurpleMain.copy(alpha = 0.65f), // Lavender tracking line
                    style = Stroke(width = 4f)
                )
            }

            // 4. Draw Detected Pothole Markers
            potholes.forEach { p ->
                val pt = gpsToCanvas(p.latitude, p.longitude, cx, cy)

                // Select color based on severity
                val markerColor = when (p.severity) {
                    "Critical" -> AlertRed
                    "High" -> Color(0xFFD97706)
                    "Medium" -> Color(0xFFB45309)
                    else -> Color(0xFF15803D)
                }

                // Pulsing highlight outer ring if selected
                if (selectedPothole?.id == p.id) {
                    drawCircle(
                        color = markerColor.copy(alpha = 0.3f),
                        radius = 28f,
                        center = pt
                    )
                    drawCircle(
                        color = markerColor.copy(alpha = 0.6f),
                        radius = 18f,
                        center = pt,
                        style = Stroke(width = 3f)
                    )
                } else {
                    // Slight halo ring
                    drawCircle(
                        color = markerColor.copy(alpha = 0.15f),
                        radius = 16f,
                        center = pt
                    )
                }

                // Inner core marker
                drawCircle(
                    color = markerColor,
                    radius = 8f,
                    center = pt
                )
                drawCircle(
                    color = Color.White,
                    radius = 3.5f,
                    center = pt
                )
            }

            // 5. Draw Active Vehicle Pointer Location
            val vOffset = gpsToCanvas(vehicleLat, vehicleLon, cx, cy)
            rotate(degrees = vehicleBearing, pivot = vOffset) {
                // Outer glow ring
                drawCircle(
                    color = PurpleMain.copy(alpha = 0.2f),
                    radius = 36f,
                    center = vOffset
                )

                // Drawing navigation arrowhead
                val arrowPath = Path().apply {
                    moveTo(vOffset.x, vOffset.y - 18f)
                    lineTo(vOffset.x - 12f, vOffset.y + 14f)
                    lineTo(vOffset.x, vOffset.y + 7f)
                    lineTo(vOffset.x + 12f, vOffset.y + 14f)
                    close()
                }
                drawPath(
                    path = arrowPath,
                    color = PurpleMain
                )
                drawPath(
                    path = arrowPath,
                    color = Color.White,
                    style = Stroke(width = 2.5f)
                )
            }
        }
    }
}
