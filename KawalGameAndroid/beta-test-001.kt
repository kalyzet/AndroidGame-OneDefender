package com.example.jagabakso

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import kotlinx.coroutines.delay
import kotlin.math.*
import kotlin.random.Random
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.Alignment


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GameScreen()
        }
    }
}

data class Enemy(var position: Offset, var lastAttackTime: Long = 0)

@Composable
fun GameScreen() {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val widthPx = with(LocalDensity.current) { maxWidth.toPx() }
        val heightPx = with(LocalDensity.current) { maxHeight.toPx() }
        val eggCenter = Offset(widthPx / 2, heightPx / 2)

        var playerPos by remember { mutableStateOf(Offset(widthPx / 2, heightPx - 100)) }
        var enemies by remember { mutableStateOf(listOf<Enemy>()) }
        var joystickOffset by remember { mutableStateOf(Offset.Zero) }
        val maxJoystickRadius = 150f

        var eggHP by remember { mutableStateOf(10) }
        val enemySpeed = 2f
        val playerSize = 60f
        val enemyRadius = 20f

        // Spawn musuh tiap 2 detik
        LaunchedEffect(Unit) {
            while (true) {
                val side = Random.nextInt(4)
                val pos = when (side) {
                    0 -> Offset(0f, Random.nextFloat() * heightPx)
                    1 -> Offset(widthPx, Random.nextFloat() * heightPx)
                    2 -> Offset(Random.nextFloat() * widthPx, 0f)
                    else -> Offset(Random.nextFloat() * widthPx, heightPx)
                }
                enemies = enemies + Enemy(pos)
                delay(2000L)
            }
        }

        // Gerak musuh ke arah telur dan serang
        LaunchedEffect(Unit) {
            while (true) {
                val now = System.currentTimeMillis()
                enemies.forEach { enemy ->
                    val direction = eggCenter - enemy.position
                    val distance = direction.getDistance()
                    if (distance > 5f) {
                        val norm = direction / distance
                        enemy.position += norm * enemySpeed
                    } else {
                        if (now - enemy.lastAttackTime >= 2000L) {
                            eggHP--
                            enemy.lastAttackTime = now
                        }
                    }
                }
                enemies = enemies.filter { it.position.getDistanceTo(playerPos) > (playerSize / 2 + enemyRadius) }
                delay(16L)
            }
        }

        // Gerakkan player dari analog
        LaunchedEffect(joystickOffset) {
            while (true) {
                val magnitude = joystickOffset.getDistance()
                if (magnitude > 0.1f) {
                    val angle = atan2(joystickOffset.y, joystickOffset.x)
                    val speed = 5f
                    playerPos += Offset(cos(angle), sin(angle)) * speed
                    playerPos = Offset(
                        playerPos.x.coerceIn(0f, widthPx),
                        playerPos.y.coerceIn(0f, heightPx)
                    )
                }
                delay(16L)
            }
        }

        Box(Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = { joystickOffset = Offset.Zero },
                        onDrag = { _, dragAmount ->
                            joystickOffset += dragAmount
                            if (joystickOffset.getDistance() > maxJoystickRadius) {
                                val angle = atan2(joystickOffset.y, joystickOffset.x)
                                joystickOffset = Offset(
                                    x = cos(angle) * maxJoystickRadius,
                                    y = sin(angle) * maxJoystickRadius
                                )
                            }
                        }
                    )
                }
            ) {
                // Telur emas di tengah
                drawCircle(
                    color = Color.Yellow,
                    radius = 40f,
                    center = eggCenter
                )

                // Musuh
                enemies.forEach { enemy ->
                    drawCircle(
                        color = Color.Red,
                        radius = enemyRadius,
                        center = enemy.position
                    )
                }

                // Player (kotak biru)
                drawRect(
                    color = Color.Cyan,
                    topLeft = Offset(playerPos.x - playerSize / 2, playerPos.y - playerSize / 2),
                    size = androidx.compose.ui.geometry.Size(playerSize, playerSize)
                )

                // Analog
                val analogCenter = Offset(200f, heightPx - 200f)
                drawCircle(
                    color = Color.Gray,
                    radius = maxJoystickRadius,
                    center = analogCenter
                )
                drawCircle(
                    color = Color.White,
                    radius = 40f,
                    center = analogCenter + joystickOffset
                )
            }

            // Tampilkan HP telur
            Text(
                text = "HP Telur: $eggHP",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            )
        }
    }
}

// Utility
private fun Offset.getDistance(): Float = sqrt(x * x + y * y)

private operator fun Offset.minus(other: Offset): Offset =
    Offset(x - other.x, y - other.y)

private operator fun Offset.div(scalar: Float): Offset =
    Offset(x / scalar, y / scalar)

private fun Offset.getDistanceTo(other: Offset): Float =
    sqrt((x - other.x).pow(2) + (y - other.y).pow(2))
