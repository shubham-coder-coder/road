package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.PotholeEntity
import com.example.ui.PotholeViewModel
import com.example.ui.camera.CameraScreen
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.map.MapScreen
import com.example.ui.potholes.PotholeListScreen
import com.example.ui.theme.*
import com.example.ui.theme.MyApplicationTheme

enum class TabItem {
    INSPECTION, MAP, PORTAL, REGISTRY
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainContainer()
            }
        }
    }
}

@Composable
fun MainContainer() {
    val context = LocalContext.current
    val viewModel: PotholeViewModel = viewModel()
    
    var currentTab by remember { mutableStateOf(TabItem.INSPECTION) }
    var selectedPotholeForMap by remember { mutableStateOf<PotholeEntity?>(null) }

    // Runtime permissions launcher
    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] == true
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (cameraGranted && locationGranted) {
            Toast.makeText(context, "All hardware sensors synchronized successfully.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Inspection limited without Camera and Location permissions.", Toast.LENGTH_LONG).show()
        }
    }

    // Check permissions on start
    LaunchedEffect(Unit) {
        val hasCamera = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val hasLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (!hasCamera || !hasLocation) {
            permissionsLauncher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().background(CreamBg),
        bottomBar = {
            NavigationBar(
                containerColor = PurpleNavBg,
                tonalElevation = 8.dp,
                modifier = Modifier.testTag("bottom_nav")
            ) {
                NavigationBarItem(
                    selected = currentTab == TabItem.INSPECTION,
                    onClick = { currentTab = TabItem.INSPECTION },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == TabItem.INSPECTION) Icons.Filled.Videocam else Icons.Outlined.Videocam,
                            contentDescription = "Inspection"
                        )
                    },
                    label = { Text("Scanner", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CharcoalDark,
                        selectedTextColor = CharcoalDark,
                        indicatorColor = PurpleNavIndicator,
                        unselectedIconColor = BodyText,
                        unselectedTextColor = BodyText
                    ),
                    modifier = Modifier.testTag("nav_inspection")
                )

                NavigationBarItem(
                    selected = currentTab == TabItem.MAP,
                    onClick = { currentTab = TabItem.MAP },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == TabItem.MAP) Icons.Filled.Map else Icons.Outlined.Map,
                            contentDescription = "Map"
                        )
                    },
                    label = { Text("Interactive Map", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CharcoalDark,
                        selectedTextColor = CharcoalDark,
                        indicatorColor = PurpleNavIndicator,
                        unselectedIconColor = BodyText,
                        unselectedTextColor = BodyText
                    ),
                    modifier = Modifier.testTag("nav_map")
                )

                NavigationBarItem(
                    selected = currentTab == TabItem.PORTAL,
                    onClick = { currentTab = TabItem.PORTAL },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == TabItem.PORTAL) Icons.Filled.Dashboard else Icons.Outlined.Dashboard,
                            contentDescription = "Portal"
                        )
                    },
                    label = { Text("Portal Desk", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CharcoalDark,
                        selectedTextColor = CharcoalDark,
                        indicatorColor = PurpleNavIndicator,
                        unselectedIconColor = BodyText,
                        unselectedTextColor = BodyText
                    ),
                    modifier = Modifier.testTag("nav_portal")
                )

                NavigationBarItem(
                    selected = currentTab == TabItem.REGISTRY,
                    onClick = { currentTab = TabItem.REGISTRY },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == TabItem.REGISTRY) Icons.Filled.GridView else Icons.Outlined.GridView,
                            contentDescription = "Registry"
                        )
                    },
                    label = { Text("Registry", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CharcoalDark,
                        selectedTextColor = CharcoalDark,
                        indicatorColor = PurpleNavIndicator,
                        unselectedIconColor = BodyText,
                        unselectedTextColor = BodyText
                    ),
                    modifier = Modifier.testTag("nav_registry")
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding).background(CreamBg)) {
            when (currentTab) {
                TabItem.INSPECTION -> {
                    CameraScreen(viewModel = viewModel)
                }
                TabItem.MAP -> {
                    // Pass selected pothole from registry if any
                    MapScreen(viewModel = viewModel)
                }
                TabItem.PORTAL -> {
                    DashboardScreen(viewModel = viewModel)
                }
                TabItem.REGISTRY -> {
                    PotholeListScreen(
                        viewModel = viewModel,
                        onNavigateToMapWithSelection = { pothole ->
                            selectedPotholeForMap = pothole
                            currentTab = TabItem.MAP
                        }
                    )
                }
            }
        }
    }
}
