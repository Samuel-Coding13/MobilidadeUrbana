package com.example.mobilidadeurbana.util

import androidx.compose.animation.core.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.material3.Text

/**
 * Utilitários leves. A função de carregar bitmap/uri foi removida por completo.
 * Mantemos apenas a animação de texto piscante usada para mensagens.
 */

@Composable
fun BlinkText(text: String) {
    val transition = rememberInfiniteTransition()
    val alpha by transition.animateFloat(
        initialValue = 0.0f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600),
            repeatMode = RepeatMode.Reverse
        )
    )
    Text(text = text, modifier = Modifier.alpha(alpha))
}
