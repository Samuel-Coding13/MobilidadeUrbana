package com.example.mobilidadeurbana

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.rememberNavController
import com.example.mobilidadeurbana.viewmodel.AuthViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MaterialTheme {
                val navController = rememberNavController()
                var initialRoute by rememberSaveable { mutableStateOf<String?>(null) }

                if (initialRoute == null) {
                    SplashWithPersistentAuth(
                        authViewModel = authViewModel,
                        onResult = { route -> initialRoute = route }
                    )
                } else {
                    AppNavigation(
                        navController = navController,
                        authViewModel = authViewModel,
                        startDestination = initialRoute!!
                    )
                }
            }
        }
    }
}

@Composable
fun SplashWithPersistentAuth(
    authViewModel: AuthViewModel,
    onResult: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isChecking by remember { mutableStateOf(true) }
    var hasInternet by remember { mutableStateOf<Boolean?>(null) }

    // CORES AZUIS
    val azulPrincipal = Color(0xFF0066FF)
    val azulEscuro = Color(0xFF003366)
    val azulClaro = Color(0xFF00D4FF)
    val vermelhoErro = Color(0xFFDD2C00)

    fun checkInternetAndProceed() {
        scope.launch {
            isChecking = true
            delay(800)
            val connected = context.isInternetAvailable()
            hasInternet = connected

            if (connected) {
                delay(600)
                val user = FirebaseAuth.getInstance().currentUser

                if (user != null && user.isEmailVerified) {
                    // Verifica nível de acesso
                    try {
                        val doc = FirebaseFirestore.getInstance()
                            .collection("usuarios")
                            .document(user.uid)
                            .get()
                            .await()

                        val acesso = doc.getLong("acesso")?.toInt()
                        when (acesso) {
                            2 -> onResult("home")     // Motorista
                            3 -> onResult("admin")    // Administrador
                            else -> onResult("login")
                        }
                    } catch (e: Exception) {
                        onResult("login")
                    }
                } else {
                    onResult("login")
                }
            }
            isChecking = false
        }
    }

    LaunchedEffect(Unit) {
        checkInternetAndProceed()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F7FF))
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        when {
            isChecking || hasInternet == null -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(id = R.drawable.outline_bus_alert_24),
                        contentDescription = "Ônibus",
                        modifier = Modifier.size(140.dp),
                        colorFilter = ColorFilter.tint(azulPrincipal)
                    )
                    Spacer(modifier = Modifier.height(40.dp))
                    Text(
                        "Mobilidade",
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Bold,
                        color = azulPrincipal
                    )
                    Text(
                        "Urbana",
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Bold,
                        color = azulClaro
                    )
                    Spacer(modifier = Modifier.height(48.dp))
                    CircularProgressIndicator(
                        color = azulPrincipal,
                        strokeWidth = 6.dp,
                        modifier = Modifier.size(60.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Verificando conexão...", color = azulEscuro)
                }
            }

            hasInternet == false -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = null,
                        tint = vermelhoErro,
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Sem conexão com a internet",
                        style = MaterialTheme.typography.headlineMedium,
                        color = azulEscuro,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Verifique sua conexão Wi-Fi ou dados móveis e tente novamente.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = { checkInternetAndProceed() },
                        colors = ButtonDefaults.buttonColors(containerColor = azulPrincipal),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Tentar Novamente", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}