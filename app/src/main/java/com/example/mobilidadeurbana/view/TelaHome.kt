package com.example.mobilidadeurbana.view

import android.Manifest
import android.annotation.SuppressLint
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.example.mobilidadeurbana.R
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun TelaHome(onLogout: () -> Unit, navController: NavController) {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    // CORES AZUIS
    val azulPrincipal = Color(0xFF0066FF)
    val azulClaro = Color(0xFF00D4FF)
    val azulEscuro = Color(0xFF003366)
    val fundoDrawer = Color(0xFFF8FBFF)

    LaunchedEffect(Unit) {
        Configuration.getInstance().load(
            context,
            androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
        )
    }

    var hasLocationPermission by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        hasLocationPermission =
            (perms[Manifest.permission.ACCESS_FINE_LOCATION] == true) ||
                    (perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true)
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    val user = FirebaseAuth.getInstance().currentUser
    val nome = user?.displayName ?: user?.email ?: "usuário"
    val fusedClient = LocationServices.getFusedLocationProviderClient(context)
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var currentLocation by remember { mutableStateOf<android.location.Location?>(null) }
    var userMarker by remember { mutableStateOf<Marker?>(null) }
    var isTracking by remember { mutableStateOf(false) }

    val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L)
        .setMinUpdateDistanceMeters(2f)
        .setMaxUpdateDelayMillis(5000L)
        .build()

    val locationCallback = remember {
        object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                currentLocation = loc
                val map = mapViewRef ?: return
                val geo = GeoPoint(loc.latitude, loc.longitude)
                if (userMarker == null) {
                    val marker = Marker(map)
                    marker.title = nome
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    marker.position = geo
                    map.overlays.add(marker)
                    userMarker = marker
                } else {
                    userMarker?.position = geo
                }
                map.controller.animateTo(geo)
                map.invalidate()
            }
        }
    }

    fun startTracking() {
        if (!hasLocationPermission) return
        try {
            fusedClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
            isTracking = true
        } catch (_: SecurityException) {}
    }

    fun stopTracking() {
        fusedClient.removeLocationUpdates(locationCallback)
        val map = mapViewRef
        userMarker?.let { marker ->
            map?.overlays?.remove(marker)
            map?.invalidate()
        }
        userMarker = null
        isTracking = false
    }

    fun fetchLastLocationAndCenter() {
        if (!hasLocationPermission) return
        fusedClient.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) {
                currentLocation = loc
                val map = mapViewRef ?: return@addOnSuccessListener
                val geo = GeoPoint(loc.latitude, loc.longitude)
                if (userMarker == null) {
                    val marker = Marker(map)
                    marker.title = nome
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    marker.position = geo
                    map.overlays.add(marker)
                    userMarker = marker
                } else {
                    userMarker?.position = geo
                }
                map.controller.setZoom(17.0)
                map.controller.setCenter(geo)
                map.invalidate()
            }
        }
    }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) fetchLastLocationAndCenter()
    }

    DisposableEffect(Unit) {
        onDispose {
            stopTracking()
            mapViewRef = null
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = fundoDrawer,
                drawerTonalElevation = 12.dp
            ) {
                Spacer(Modifier.height(32.dp))
                Column(modifier = Modifier.padding(24.dp)) {
                    Image(
                        painter = painterResource(id = R.drawable.outline_bus_alert_24),
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        colorFilter = ColorFilter.tint(azulPrincipal)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("Mobilidade Urbana", style = MaterialTheme.typography.headlineSmall, color = azulEscuro, fontWeight = FontWeight.Bold)
                }

                HorizontalDivider(color = azulClaro.copy(alpha = 0.3f))

                Spacer(Modifier.height(16.dp))

                NavigationDrawerItem(
                    label = { Text("Perfil", color = azulEscuro) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("perfil")
                    },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Perfil", tint = azulPrincipal) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    label = { Text("Ouvidoria", color = azulEscuro) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("ouvidoria")
                    },
                    icon = { Icon(Icons.Default.Call, contentDescription = "Ouvidoria", tint = azulPrincipal) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                Spacer(Modifier.weight(1f))

                TextButton(
                    onClick = {
                        scope.launch { drawerState.close() }
                        stopTracking()
                        FirebaseAuth.getInstance().signOut()
                        onLogout()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Spacer(Modifier.width(12.dp))
                    Text("Sair da conta", color = Color.Red, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "Mobilidade Urbana",
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, tint = Color.White, contentDescription = "Menu")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = azulPrincipal)
                )
            },
            containerColor = Color(0xFFF0F7FF)
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        val map = MapView(ctx).apply {
                            setMultiTouchControls(true)
                            controller.setZoom(16.0)
                        }
                        mapViewRef = map
                        map
                    },
                    update = { map ->
                        if (hasLocationPermission && currentLocation == null) fetchLastLocationAndCenter()
                    }
                )

                // Botão de rastreamento flutuante
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    FloatingActionButton(
                        onClick = { if (isTracking) stopTracking() else startTracking() },
                        shape = CircleShape,
                        containerColor = if (isTracking) Color(0xFFD50000) else Color(0xFF00C853),
                        modifier = Modifier
                            .size(72.dp)
                            .align(Alignment.Center)
                    ) {
                        Icon(
                            if (isTracking) Icons.Default.Close else Icons.Default.PlayArrow,
                            contentDescription = if (isTracking) "Parar rastreamento" else "Iniciar rastreamento",
                            tint = Color.White,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }
            }
        }
    }
}