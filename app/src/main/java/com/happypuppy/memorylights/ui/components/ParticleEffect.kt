package com.happypuppy.memorylights.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.*
import kotlin.random.Random

data class Particle(
    val id: Int,
    val startX: Float,
    val startY: Float,
    val velocityX: Float,
    val velocityY: Float,
    val color: Color,
    val size: Float,
    val life: Float = 1f,
    val gravity: Float = 0.5f
)

@Composable
fun ParticleEffect(
    isActive: Boolean,
    modifier: Modifier = Modifier,
    particleCount: Int = 50,
    colors: List<Color> = listOf(
        Color(0xFFFFD700), // Gold
        Color(0xFFFFA500), // Orange
        Color(0xFFFF6B6B), // Red
        Color(0xFF4ECDC4), // Teal
        Color(0xFF45B7D1), // Blue
        Color(0xFF96CEB4), // Green
        Color(0xFFFECA57)  // Yellow
    ),
    onComplete: (() -> Unit)? = null
) {
    val density = LocalDensity.current
    var particles by remember { mutableStateOf(emptyList<Particle>()) }
    
    var animationProgress by remember { mutableFloatStateOf(0f) }
    
    LaunchedEffect(isActive) {
        if (isActive) {
            // Generate particles
            particles = (0 until particleCount).map { i ->
                val angle = Random.nextFloat() * 2 * PI
                val speed = Random.nextFloat() * 150 + 50
                Particle(
                    id = i,
                    startX = 0f,
                    startY = 0f,
                    velocityX = cos(angle).toFloat() * speed,
                    velocityY = sin(angle).toFloat() * speed - Random.nextFloat() * 100,
                    color = colors.random(),
                    size = Random.nextFloat() * 12 + 8,
                    gravity = Random.nextFloat() * 0.4f + 0.1f
                )
            }
            
            // Drive the progress off the Compose frame clock so we tick once
            // per actual frame instead of a fixed 16 ms timer.
            animationProgress = 0f
            val animationDuration = 2000f
            val startMillis = withFrameMillis { it }
            while (animationProgress < 1f) {
                val nowMillis = withFrameMillis { it }
                animationProgress = ((nowMillis - startMillis) / animationDuration).coerceAtMost(1f)
            }
            
            particles = emptyList()
            onComplete?.invoke()
        } else {
            animationProgress = 0f
            particles = emptyList()
        }
    }
    
    Canvas(modifier = modifier.fillMaxSize()) {
        if (isActive && particles.isNotEmpty()) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            
            particles.forEach { particle ->
                val time = animationProgress * 2000 // Convert to milliseconds
                
                // Calculate current position with gravity
                val currentX = centerX + particle.velocityX * time / 1000
                val currentY = centerY + particle.velocityY * time / 1000 + 
                              0.5f * particle.gravity * (time / 1000).pow(2) * 500
                
                // Calculate alpha based on life and time
                val alpha = maxOf(0f, 1f - animationProgress)
                val particleColor = particle.color.copy(alpha = alpha)
                
                // Only draw if particle is visible
                if (alpha > 0f) {
                    drawCircle(
                        color = particleColor,
                        radius = particle.size * (1f - animationProgress * 0.5f),
                        center = Offset(currentX, currentY)
                    )
                }
            }
        }
    }
}