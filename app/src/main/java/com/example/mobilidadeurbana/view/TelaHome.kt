package com.example.mobilidadeurbana.view

import android.Manifest
import android.annotation.SuppressLint
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun TelaHome(onLogout: () -> Unit, navController: NavController) {
    val context = LocalContext.current.applicationContext

    // Configuração inicial do osmdroid
    LaunchedEffect(Unit) {
        Configuration.getInstance().load(context, androidx.preference.PreferenceManager.getDefaultSharedPreferences(context))
    }

    // Permissão de localização
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

    // Estados
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
        // 🔥 Remove o marcador do mapa
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

    // Drawer controlado via CoroutineScope
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val gesturesEnabled = drawerState.isOpen

    // Interface principal
    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = gesturesEnabled, // ✅ só fecha por gesto quando aberto
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFFEEEEEE),
                drawerTonalElevation = 8.dp
            ) {
                Spacer(Modifier.height(24.dp))
                Text(
                    "Menu",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 24.dp, bottom = 16.dp)
                )

                // Cartão de informações
                val lat = currentLocation?.latitude?.roundToInt()?.toString() ?: "—"
                val lon = currentLocation?.longitude?.roundToInt()?.toString() ?: "—"
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF2196F3),
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Olá, $nome!", color = Color.White, style = MaterialTheme.typography.titleMedium)
                        Text("Latitude: $lat", color = Color.White)
                        Text("Longitude: $lon", color = Color.White)
                    }
                }

                Spacer(Modifier.height(24.dp))

                NavigationDrawerItem(
                    label = { Text("Perfil") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("perfil")
                    },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Perfil") },
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
                    Icon(Icons.Default.ExitToApp, contentDescription = "Sair")
                    Spacer(Modifier.width(8.dp))
                    Text("Sair")
                }
            }
        },
        content = {
            Surface(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Mapa
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

                    // TopAppBar com clique para abrir menu
                    TopAppBar(
                        title = {
                            Text(
                                "Mobilidade Urbana",
                                modifier = Modifier.clickable {
                                    scope.launch { drawerState.open() }
                                }
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            titleContentColor = Color.White,
                            navigationIconContentColor = Color.White
                        )
                    )

                    // 🔵 Barra inferior com botão perfeitamente centralizado
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Color(0x66000000))
                            .padding(16.dp)
                    ) {
                        // Linha base: botão de rastreamento centralizado
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            FloatingActionButton(
                                onClick = {
                                    if (isTracking) stopTracking() else startTracking()
                                },
                                shape = CircleShape,
                                containerColor = if (isTracking) Color(0xFFD50000) else Color(0xFF00C853),
                                modifier = Modifier.size(72.dp)
                            ) {
                                Icon(
                                    if (isTracking) Icons.Default.Close else Icons.Default.PlayArrow,
                                    contentDescription = if (isTracking) "Parar rastreamento" else "Iniciar rastreamento",
                                    tint = Color.White,
                                    modifier = Modifier.size(38.dp)
                                )
                            }
                        }

                        // Botão Sair fixo no canto direito
                        Button(
                            onClick = {
                                scope.launch { drawerState.close() }
                                stopTracking()
                                FirebaseAuth.getInstance().signOut()
                                onLogout()
                            },
                            shape = RoundedCornerShape(40.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0277BD)),
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .height(56.dp)
                                .width(120.dp)
                        ) {
                            Icon(Icons.Default.ExitToApp, contentDescription = "Sair", tint = Color.White)
                            Spacer(Modifier.width(6.dp))
                            Text("Sair", color = Color.White)
                        }
                    }
                }
            }
        }
    )

    // Atualiza posição inicial
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) fetchLastLocationAndCenter()
    }

    DisposableEffect(Unit) {
        onDispose {
            stopTracking()
            mapViewRef = null
        }
    }
}
