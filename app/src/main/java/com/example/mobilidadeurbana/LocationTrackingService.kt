package com.example.mobilidadeurbana.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.mobilidadeurbana.MainActivity
import com.example.mobilidadeurbana.R
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint

class LocationTrackingService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var rotaCodigo: String? = null
    private var statusOnibus: String = "Em operação"
    private var isTracking = false

    companion object {
        const val CHANNEL_ID = "location_tracking_channel"
        const val NOTIFICATION_ID = 1
        const val ACTION_START_TRACKING = "START_TRACKING"
        const val ACTION_STOP_TRACKING = "STOP_TRACKING"
        const val EXTRA_ROTA_CODIGO = "rota_codigo"
        const val EXTRA_STATUS = "status"
        const val EXTRA_LAT = "lat"
        const val EXTRA_LNG = "lng"
        const val EXTRA_VELOCIDADE = "velocidade"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("LocationService", "Service onCreate")

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        createNotificationChannel()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    updateLocationInFirebase(location)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("LocationService", "onStartCommand: ${intent?.action}")

        when (intent?.action) {
            ACTION_START_TRACKING -> {
                rotaCodigo = intent.getStringExtra(EXTRA_ROTA_CODIGO)
                statusOnibus = intent.getStringExtra(EXTRA_STATUS) ?: "Em operação"
                val lat = intent.getDoubleExtra(EXTRA_LAT, 0.0)
                val lng = intent.getDoubleExtra(EXTRA_LNG, 0.0)
                val velocidade = intent.getFloatExtra(EXTRA_VELOCIDADE, 0f)

                startTracking(lat, lng, velocidade)
            }
            ACTION_STOP_TRACKING -> {
                stopTracking()
            }
        }

        return START_STICKY
    }

    private fun startTracking(lat: Double, lng: Double, velocidade: Float) {
        if (isTracking) {
            Log.d("LocationService", "Já está rastreando")
            return
        }

        Log.d("LocationService", "Iniciando rastreamento")
        isTracking = true

        startForeground(NOTIFICATION_ID, createNotification())

        val location = Location("initial").apply {
            latitude = lat
            longitude = lng
            speed = velocidade
        }
        updateLocationInFirebase(location)

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            2000L
        ).apply {
            setMinUpdateDistanceMeters(1f)
            setMaxUpdateDelayMillis(2000L)
            setWaitForAccurateLocation(false)
        }.build()

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("LocationService", "Sem permissão de localização")
            stopSelf()
            return
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        Log.d("LocationService", "Rastreamento iniciado com sucesso")
    }

    private fun stopTracking() {
        if (!isTracking) {
            Log.d("LocationService", "Não está rastreando")
            return
        }

        Log.d("LocationService", "Parando rastreamento")
        isTracking = false

        fusedLocationClient.removeLocationUpdates(locationCallback)

        val user = auth.currentUser
        if (user != null) {
            firestore.collection("onibus")
                .document(user.uid)
                .delete()
                .addOnSuccessListener {
                    Log.d("LocationService", "Documento removido do Firestore")
                }
                .addOnFailureListener { e ->
                    Log.e("LocationService", "Erro ao remover documento", e)
                }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun updateLocationInFirebase(location: Location) {
        val user = auth.currentUser
        if (user == null || rotaCodigo == null || !isTracking) {
            Log.w("LocationService", "Não pode atualizar: user=$user, rota=$rotaCodigo, tracking=$isTracking")
            return
        }

        val dados = hashMapOf(
            "uid" to user.uid,
            "localizacao" to GeoPoint(location.latitude, location.longitude),
            "lat" to location.latitude,
            "lng" to location.longitude,
            "status" to statusOnibus,
            "rotaCodigo" to rotaCodigo!!,
            "velocidade" to location.speed,
            "timestamp" to com.google.firebase.Timestamp.now()
        )

        firestore.collection("onibus")
            .document(user.uid)
            .set(dados)
            .addOnSuccessListener {
                Log.d("LocationService", "Localização atualizada: ${location.latitude}, ${location.longitude}")
            }
            .addOnFailureListener { e ->
                Log.e("LocationService", "Erro ao atualizar localização", e)
            }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Rastreamento de Localização",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Rastreamento contínuo da localização do ônibus"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Rastreamento Ativo")
            .setContentText("Rota: $rotaCodigo - Status: $statusOnibus")
            .setSmallIcon(R.drawable.outline_bus_alert_24)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.d("LocationService", "Service onDestroy")
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}