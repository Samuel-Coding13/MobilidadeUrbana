package com.example.mobilidadeurbana.viewmodel

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobilidadeurbana.service.LocationTrackingService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.osmdroid.views.overlay.Polyline

data class Parada(
    val id: String = "",
    val nome: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0
)

data class PontoRota(
    val lat: Double = 0.0,
    val lng: Double = 0.0
)

data class Rota(
    val codigo: String = "",
    val nome: String = "",
    val cor: String = "#FF0000",
    val pontos: List<PontoRota> = emptyList(),
    val paradas: List<Parada> = emptyList()
)

class HomeViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    var statusOnibus = mutableStateOf("Em operação")
        private set

    var rotaSelecionada = mutableStateOf<Rota?>(null)
        private set

    var isTracking = mutableStateOf(false)
        private set

    var rotas = mutableStateOf<List<Rota>>(emptyList())
        private set

    private val _polylines = MutableStateFlow<List<Polyline>>(emptyList())
    val polylines: StateFlow<List<Polyline>> = _polylines.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun setStatus(novoStatus: String) {
        statusOnibus.value = novoStatus
    }

    fun selecionarRota(rota: Rota) {
        rotaSelecionada.value = rota
        Log.d("HOME_VM", "Rota selecionada: ${rota.nome} (${rota.codigo})")
    }

    fun carregarRotas() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val rotasCarregadas = pegarRotas()
                rotas.value = rotasCarregadas

                val novasPolylines = gerarPolylines(rotasCarregadas)
                _polylines.value = novasPolylines

                Log.d("HOME_VM", "Rotas carregadas: ${rotasCarregadas.size}")
            } catch (e: Exception) {
                Log.e("HOME_VM", "Erro ao carregar rotas", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun pegarRotas(): List<Rota> {
        return try {
            val snapshot = firestore
                .collection("rotas")
                .orderBy("nome", Query.Direction.ASCENDING)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                try {
                    val codigo = doc.id
                    val nome = doc.getString("nome") ?: "Rota $codigo"
                    val cor = doc.getString("cor") ?: "#FF0000"

                    val pontosArray = doc.get("codigo") as? List<Map<String, Any>>
                    val pontos = pontosArray?.mapNotNull { ponto ->
                        val lng = (ponto["lng"] as? Number)?.toDouble()
                        val lat = (ponto["lat"] as? Number)?.toDouble()
                        if (lat != null && lng != null) {
                            PontoRota(lat, lng)
                        } else null
                    }?.takeIf { it.size >= 2 } ?: emptyList()

                    val paradasArray = doc.get("paradas") as? List<Map<String, Any>>
                    val paradas = paradasArray?.mapNotNull { parada ->
                        try {
                            val id = (parada["id"] as? Number)?.toString() ?: ""
                            val nomeParada = parada["nome"] as? String ?: "Parada $id"
                            val lat = (parada["lat"] as? Number)?.toDouble() ?: 0.0
                            val lng = (parada["lng"] as? Number)?.toDouble() ?: 0.0

                            if (lat != 0.0 && lng != 0.0) {
                                Parada(id, nomeParada, lat, lng)
                            } else null
                        } catch (e: Exception) {
                            Log.e("HOME_VM", "Erro ao processar parada", e)
                            null
                        }
                    } ?: emptyList()

                    if (pontos.isEmpty()) {
                        Log.w("HOME_VM", "Rota $codigo sem pontos suficientes")
                        null
                    } else {
                        Rota(
                            codigo = codigo,
                            nome = nome,
                            cor = cor,
                            pontos = pontos,
                            paradas = paradas
                        )
                    }
                } catch (e: Exception) {
                    Log.e("HOME_VM", "Erro ao processar rota ${doc.id}", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("HOME_VM", "Erro ao pegar rotas", e)
            emptyList()
        }
    }

    private fun gerarPolylines(rotas: List<Rota>): List<Polyline> {
        val novasLinhas = mutableListOf<Polyline>()

        for (rota in rotas) {
            if (rota.pontos.size < 2) continue

            val polyline = Polyline().apply {
                title = rota.nome

                color = try {
                    android.graphics.Color.parseColor(rota.cor.uppercase())
                } catch (e: Exception) {
                    android.graphics.Color.parseColor("#FF0000")
                }

                width = 8f
                isGeodesic = true
            }

            var pontosValidos = 0
            for (ponto in rota.pontos) {
                polyline.addPoint(org.osmdroid.util.GeoPoint(ponto.lat, ponto.lng))
                pontosValidos++
            }

            if (pontosValidos >= 2) {
                novasLinhas.add(polyline)
                Log.d("HOME_VM", "Polyline criada para ${rota.nome}: $pontosValidos pontos")
            }
        }

        return novasLinhas
    }

    fun startTracking(context: Context, lat: Double, lng: Double, velocidade: Float) {
        val rota = rotaSelecionada.value ?: return

        val serviceIntent = Intent(context, LocationTrackingService::class.java).apply {
            action = LocationTrackingService.ACTION_START_TRACKING
            putExtra(LocationTrackingService.EXTRA_ROTA_CODIGO, rota.codigo)
            putExtra(LocationTrackingService.EXTRA_STATUS, statusOnibus.value)
            putExtra(LocationTrackingService.EXTRA_LAT, lat)
            putExtra(LocationTrackingService.EXTRA_LNG, lng)
            putExtra(LocationTrackingService.EXTRA_VELOCIDADE, velocidade)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }

        isTracking.value = true
        Log.d("HOME_VM", "Rastreamento iniciado via serviço na rota ${rota.codigo}")
    }

    fun stopTracking(context: Context) {
        val serviceIntent = Intent(context, LocationTrackingService::class.java).apply {
            action = LocationTrackingService.ACTION_STOP_TRACKING
        }
        context.startService(serviceIntent)

        isTracking.value = false
        Log.d("HOME_VM", "Rastreamento parado")
    }

    override fun onCleared() {
        super.onCleared()
        _polylines.value = emptyList()
    }
}