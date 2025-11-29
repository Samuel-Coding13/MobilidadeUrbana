package com.example.mobilidadeurbana

import android.app.Application
import com.google.firebase.FirebaseApp
import org.osmdroid.config.Configuration
import java.io.File

/**
 * Classe de aplicação principal
 * Inicializa Firebase e configurações do OSMDroid
 */
class MyApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Inicializa o Firebase
        FirebaseApp.initializeApp(this)

        // Configuração do OSMDroid (mapas)
        Configuration.getInstance().apply {
            // Define o diretório base para cache do OSMDroid
            osmdroidBasePath = File(cacheDir, "osmdroid")

            // Define o diretório para cache de tiles (mapas)
            osmdroidTileCache = File(osmdroidBasePath, "tiles")

            // Define o User Agent (identificação do app)
            userAgentValue = packageName

            // Configura o nível de log (opcional, para debug)
            // Descomente a linha abaixo para ver logs do OSMDroid
            // isDebugMode = true

            // Define o tamanho máximo do cache (200 MB)
            tileFileSystemCacheMaxBytes = 200L * 1024L * 1024L

            // Define o tempo de expiração do cache (7 dias)
            expirationOverrideDuration = 7L * 24L * 60L * 60L * 1000L
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        // Limpeza adicional se necessário
    }
}