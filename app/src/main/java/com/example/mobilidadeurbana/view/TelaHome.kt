package com.example.mobilidadeurbana.view

import android.Manifest
import android.annotation.SuppressLint
import android.os.Looper
import androidx.activity.compose.BackHandler
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
import com.google.firebase.firestore.FirebaseFirestore
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
    val firestore = FirebaseFirestore.getInstance()
    val user = FirebaseAuth.getInstance().currentUser

    // CORES AZUIS
    val azulPrincipal = Color(0xFF0066FF)
    val azulClaro = Color(0xFF00D4FF)
    val azulEscuro = Color(0xFF003366)
    val fundoDrawer = Color(0xFFF8FBFF)

    // Estados para os botões
    var statusOnibus by remember { mutableStateOf("Parado") }
    var rotaSelecionada by remember { mutableStateOf("") }
    var showStatusDialog by remember { mutableStateOf(false) }
    var showRotaDialog by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }

    val statusOptions = listOf("Em operação", "Parado", "Manutenção", "Fora de serviço")
    val rotaOptions = listOf("T1", "T2", "T3")

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

    val nome = user?.displayName ?: user?.email ?: "usuário"
    val fusedClient = LocationServices.getFusedLocationProviderClient(context)
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var currentLocation by remember { mutableStateOf<android.location.Location?>(null) }
    var userMarker by remember { mutableStateOf<Marker?>(null) }
    var isTracking by remember { mutableStateOf(false) }

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
                map.controller.animateTo(geo)
                map.invalidate()

                // Atualiza localização no Firebase CORRETAMENTE
                if (isTracking && rotaSelecionada.isNotEmpty() && user != null) {
                    firestore.collection("onibus")
                        .document(rotaSelecionada)
                        .collection("veiculos")
                        .document(user.uid)
                        .set(
                            hashMapOf(
                                "uid" to user.uid,
                                "lat" to loc.latitude,
                                "lng" to loc.longitude,
                                "status" to statusOnibus,
                                "timestamp" to com.google.firebase.Timestamp.now()
                            )
                        )
                }
            }
        }
    }

    fun startTracking() {
        if (!hasLocationPermission) return
        if (rotaSelecionada.isEmpty()) return
        try {
            fusedClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
            isTracking = true
        } catch (_: SecurityException) {}
    }

    fun stopTracking() {
        fusedClient.removeLocationUpdates(locationCallback)

        // Remove do Firebase
        if (user != null && rotaSelecionada.isNotEmpty()) {
            firestore.collection("onibus")
                .document(rotaSelecionada)
                .collection("veiculos")
                .document(user.uid)
                .delete()
        }

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

    // Intercepta o botão voltar
    BackHandler {
        showExitDialog = true
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
                    },
                    update = { map ->
                        if (hasLocationPermission && currentLocation == null) fetchLastLocationAndCenter()
                    }
                )

                // Botões na parte inferior
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
                            modifier = Modifier.size(64.dp)
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = "Status",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        // Botão Rastreamento (central, maior)
                        FloatingActionButton(
                            onClick = {
                                if (isTracking) {
                                    stopTracking()
                                } else {
                                    if (rotaSelecionada.isEmpty()) {
                                        // Mostrar mensagem
                                    } else {
                                        showConfirmDialog = true
                                    }
                                }
                            },
                            shape = CircleShape,
                            containerColor = if (isTracking) Color(0xFFD50000) else Color(0xFF00C853),
                            modifier = Modifier.size(72.dp)
                        ) {
                            Icon(
                                if (isTracking) Icons.Default.Close else Icons.Default.PlayArrow,
                                contentDescription = if (isTracking) "Parar" else "Iniciar",
                                tint = Color.White,
                                modifier = Modifier.size(38.dp)
                            )
                        }

                        // Botão Rota
                        FloatingActionButton(
                            onClick = { showRotaDialog = true },
                            shape = CircleShape,
                            containerColor = azulPrincipal,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = "Rota",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                // Chip mostrando rota selecionada (mantém rota persistente)
                if (rotaSelecionada.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = azulPrincipal)
                    ) {
                        Text(
                            text = "Rota: $rotaSelecionada",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // Dialog de Status
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
                                onClick = { statusOnibus = status }
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

    // Dialog de Rota
    if (showRotaDialog) {
        AlertDialog(
            onDismissRequest = { showRotaDialog = false },
            title = { Text("Selecionar Rota", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Escolha a rota que será rastreada:")
                    Spacer(Modifier.height(16.dp))
                    rotaOptions.forEach { rota ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = rotaSelecionada == rota,
                                onClick = { rotaSelecionada = rota },
                                enabled = !isTracking
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(rota)
                        }
                    }
                    if (isTracking) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Pare o rastreamento para mudar de rota",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Red
                        )
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

    // Dialog de Confirmação
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Confirmar Rastreamento", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Deseja realmente iniciar o rastreamento?")
                    Spacer(Modifier.height(12.dp))
                    Text("Rota: $rotaSelecionada", fontWeight = FontWeight.Bold, color = azulPrincipal)
                    Text("Status: $statusOnibus", fontWeight = FontWeight.Bold, color = azulPrincipal)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showConfirmDialog = false
                    startTracking()
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

    // Dialog de Saída
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