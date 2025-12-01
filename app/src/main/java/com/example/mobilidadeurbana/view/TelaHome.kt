package com.example.mobilidadeurbana.view

import android.Manifest
import android.annotation.SuppressLint
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.mobilidadeurbana.R
import com.example.mobilidadeurbana.viewmodel.HomeViewModel
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun TelaHome(
    onLogout: () -> Unit,
    navController: NavController,
    viewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val user = FirebaseAuth.getInstance().currentUser

    val azulPrincipal = Color(0xFF0066FF)
    val azulClaro = Color(0xFF00D4FF)
    val azulEscuro = Color(0xFF003366)
    val fundoDrawer = Color(0xFFF8FBFF)

    val statusOnibus by viewModel.statusOnibus
    val rotaSelecionada by viewModel.rotaSelecionada
    val isTracking by viewModel.isTracking
    val rotas by viewModel.rotas

    var showStatusDialog by remember { mutableStateOf(false) }
    var showRotaDialog by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }

    val statusOptions = listOf("Em operação", "Parado", "Manutenção", "Fora de serviço")

    LaunchedEffect(Unit) {
        Configuration.getInstance().load(
            context,
            androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
        )
        viewModel.carregarRotas()
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

    val nome = user?.displayName ?: user?.email ?: "usuário"
    val fusedClient = LocationServices.getFusedLocationProviderClient(context)
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var currentLocation by remember { mutableStateOf<android.location.Location?>(null) }
    var userMarker by remember { mutableStateOf<Marker?>(null) }
    var routePolyline by remember { mutableStateOf<Polyline?>(null) }
    var paradaMarkers by remember { mutableStateOf<List<Marker>>(emptyList()) }

    val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
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

                if (!isTracking) {
                    map.controller.animateTo(geo)
                }
                map.invalidate()

                if (isTracking) {
                    viewModel.updateLocationInFirebase(loc.latitude, loc.longitude)
                }
            }
        }
    }

    // BASEADO NO CÓDIGO DO EDUARDO - Polyline correta
    fun desenharRota(rota: com.example.mobilidadeurbana.viewmodel.Rota) {
        val map = mapViewRef ?: return

        // Remove desenhos anteriores
        routePolyline?.let { map.overlays.remove(it) }
        paradaMarkers.forEach { map.overlays.remove(it) }

        // Cria a polyline
        if (rota.pontos.isNotEmpty()) {
            val polyline = Polyline(map)

            // Converte pontos para GeoPoint
            val geoPoints = rota.pontos.map { ponto ->
                GeoPoint(ponto.lat, ponto.lng)
            }

            polyline.setPoints(geoPoints)

            // Cor da linha
            val corInt = try {
                android.graphics.Color.parseColor(rota.cor)
            } catch (e: Exception) {
                azulPrincipal.toArgb()
            }

            // Configura estilo (BASEADO NO CÓDIGO DO EDUARDO)
            polyline.outlinePaint.apply {
                color = corInt
                strokeWidth = 10f
                isAntiAlias = true
            }

            map.overlays.add(polyline)
            routePolyline = polyline

            // Centraliza
            if (geoPoints.isNotEmpty()) {
                map.controller.setZoom(14.0)
                map.controller.setCenter(geoPoints[0])
            }
        }

        // Paradas
        val novosMarkers = rota.paradas.map { parada ->
            val marker = Marker(map)
            marker.position = GeoPoint(parada.lat, parada.lng)
            marker.title = parada.nome
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            map.overlays.add(marker)
            marker
        }
        paradaMarkers = novosMarkers

        map.invalidate()
    }



    LaunchedEffect(rotaSelecionada) {
        rotaSelecionada?.let { rota ->
            desenharRota(rota)
        }
    }

    // CORRIGIDO: Função startTracking simplificada
    fun startTracking() {
        if (!hasLocationPermission) return
        if (rotaSelecionada == null) return
        if (isTracking) return // Evita duplo clique

        try {
            fusedClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
            currentLocation?.let { loc ->
                viewModel.startTracking(loc.latitude, loc.longitude)
            }
        } catch (_: SecurityException) {}
    }

    fun stopTracking() {
        fusedClient.removeLocationUpdates(locationCallback)
        viewModel.stopTracking()
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
                }

                if (rotaSelecionada == null) {
                    map.controller.setZoom(17.0)
                    map.controller.setCenter(geo)
                }
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
                    modifier = Modifier.padding(horizontal = 12.dp),
                    shape = MaterialTheme.shapes.small,
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.Transparent
                    )
                )

                NavigationDrawerItem(
                    label = { Text("Ouvidoria", color = azulEscuro) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("ouvidoria")
                    },
                    icon = { Icon(Icons.Default.Call, contentDescription = "Ouvidoria", tint = azulPrincipal) },
                    modifier = Modifier.padding(horizontal = 12.dp),
                    shape = MaterialTheme.shapes.small,
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.Transparent
                    )
                )

                Spacer(Modifier.weight(1f))

                TextButton(
                    onClick = {
                        scope.launch { drawerState.close() }
                        showExitDialog = true
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
                    }
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Botão Status
                        FloatingActionButton(
                            onClick = { showStatusDialog = true },
                            shape = CircleShape,
                            containerColor = Color(0xFFFF9800),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = "Status",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        // Botão Play/Stop - CORRIGIDO
                        FloatingActionButton(
                            onClick = {
                                if (isTracking) {
                                    stopTracking()
                                } else {
                                    if (rotaSelecionada == null) {
                                        // Mostrar mensagem
                                    } else {
                                        showConfirmDialog = true
                                    }
                                }
                            },
                            shape = CircleShape,
                            containerColor = if (isTracking) Color(0xFFD50000) else Color(0xFF00C853),
                            modifier = Modifier.size(70.dp)
                        ) {
                            Icon(
                                if (isTracking) Icons.Default.Close else Icons.Default.PlayArrow,
                                contentDescription = if (isTracking) "Parar" else "Iniciar",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        // Botão Rota
                        FloatingActionButton(
                            onClick = { showRotaDialog = true },
                            shape = CircleShape,
                            containerColor = azulPrincipal,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = "Rota",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                rotaSelecionada?.let { rota ->
                    Card(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = try {
                                Color(android.graphics.Color.parseColor(rota.cor))
                            } catch (e: Exception) {
                                azulPrincipal
                            }
                        )
                    ) {
                        Text(
                            text = "Rota: ${rota.nome}",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // Dialog Status
    if (showStatusDialog) {
        AlertDialog(
            onDismissRequest = { showStatusDialog = false },
            title = { Text("Status do Ônibus", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Status atual: $statusOnibus", fontWeight = FontWeight.Bold, color = azulPrincipal)
                    Spacer(Modifier.height(16.dp))
                    Text("Selecione um novo status:")
                    Spacer(Modifier.height(8.dp))
                    statusOptions.forEach { status ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = statusOnibus == status,
                                onClick = { viewModel.setStatus(status) }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(status)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showStatusDialog = false }) {
                    Text("OK", color = azulPrincipal)
                }
            }
        )
    }

    // Dialog Rota
    if (showRotaDialog) {
        AlertDialog(
            onDismissRequest = { showRotaDialog = false },
            title = { Text("Selecionar Rota", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn {
                    items(rotas) { rota ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !isTracking) {
                                    viewModel.selecionarRota(rota)
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = rotaSelecionada?.codigo == rota.codigo,
                                onClick = { viewModel.selecionarRota(rota) },
                                enabled = !isTracking
                            )
                            Spacer(Modifier.width(12.dp))

                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(
                                        try {
                                            Color(android.graphics.Color.parseColor(rota.cor))
                                        } catch (e: Exception) {
                                            azulPrincipal
                                        },
                                        shape = MaterialTheme.shapes.small
                                    )
                            )

                            Spacer(Modifier.width(12.dp))
                            Text("${rota.nome} (${rota.codigo})")
                        }
                    }

                    if (isTracking) {
                        item {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Pare o rastreamento para mudar de rota",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Red
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRotaDialog = false }) {
                    Text("OK", color = azulPrincipal)
                }
            }
        )
    }

    // Dialog Confirmação
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Confirmar Rastreamento", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Deseja realmente iniciar o rastreamento?")
                    Spacer(Modifier.height(12.dp))
                    Text("Rota: ${rotaSelecionada?.nome}", fontWeight = FontWeight.Bold, color = azulPrincipal)
                    Text("Status: $statusOnibus", fontWeight = FontWeight.Bold, color = azulPrincipal)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showConfirmDialog = false
                    startTracking() // Chama apenas UMA vez
                }) {
                    Text("SIM", color = Color(0xFF00C853), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("NÃO", color = Color.Gray)
                }
            }
        )
    }

    // Dialog Saída
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Sair do Aplicativo", fontWeight = FontWeight.Bold) },
            text = {
                Text("Deseja realmente sair do aplicativo?${if (isTracking) "\n\nO rastreamento será interrompido." else ""}")
            },
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    stopTracking()
                    FirebaseAuth.getInstance().signOut()
                    onLogout()
                }) {
                    Text("SIM", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("NÃO", color = Color.Gray)
                }
            }
        )
    }
}