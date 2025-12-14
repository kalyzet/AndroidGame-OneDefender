package com.example.jagabakso

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.foundation.gestures.detectDragGestures



class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        setContent {
            GameScreen()
        }
    }
}

data class Enemy(var position: Offset, var lastAttackTime: Long = 0L)

@Composable
fun GameScreen() {
    var eggHP by remember { mutableStateOf(10) }
    var score by remember { mutableStateOf(0) }
    var isGameOver by remember { mutableStateOf(false) }

    val playerSize = 60f
    val eggRadius = 40f
    val enemyRadius = 30f
    val joystickRadius = 120f
    val knobRadius = 40f
    val enemySpeed = 2f

    var playerPos by remember { mutableStateOf(Offset.Zero) }
    var joystickCenter by remember { mutableStateOf(Offset.Zero) }
    var joystickOffset by remember { mutableStateOf(Offset.Zero) }
    var enemies by remember { mutableStateOf(listOf<Enemy>()) }

    var widthPx by remember { mutableStateOf(0f) }
    var heightPx by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            if (!isGameOver) {
                val velocity = joystickOffset * 0.2f
                playerPos += velocity

                // Batas layar
                playerPos = Offset(
                    x = playerPos.x.coerceIn(0f, widthPx),
                    y = playerPos.y.coerceIn(0f, heightPx)
                )
            }
            delay(16L)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            if (!isGameOver) {
                val now = System.currentTimeMillis()
                val eggCenter = Offset(widthPx / 2, heightPx / 2)
                val newEnemies = enemies.toMutableList()

                for (enemy in enemies) {
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

                // Cek serangan player
                newEnemies.removeAll {
                    it.position.getDistanceTo(playerPos) <= (playerSize / 2 + enemyRadius)
                }
                score += (enemies.size - newEnemies.size) * 10
                enemies = newEnemies

                // Tambah musuh setiap skor kelipatan 100
                val expectedCount = 3 + (score / 100) * 2
                if (enemies.size < expectedCount) {
                    enemies = enemies + Enemy(
                        position = Offset(
                            x = (0..widthPx.toInt()).random().toFloat(),
                            y = listOf(0, heightPx.toInt()).random().toFloat()
                        )
                    )
                }

                if (eggHP <= 0) {
                    isGameOver = true
                }
            }

            delay(50)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Canvas(modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
            }
            .onGloballyPositioned {
                widthPx = it.size.width.toFloat()
                heightPx = it.size.height.toFloat()
                if (playerPos == Offset.Zero) {
                    playerPos = Offset(widthPx / 2, heightPx - 100f)
                    joystickCenter = Offset(200f, heightPx - 200f)
                }
            }
        ) {
            // Telur emas
            drawCircle(Color.Yellow, radius = eggRadius, center = Offset(widthPx / 2, heightPx / 2))

            // Player
            drawRect(Color.Cyan, topLeft = playerPos - Offset(playerSize / 2, playerSize / 2), size = androidx.compose.ui.geometry.Size(playerSize, playerSize))

            // Musuh
            enemies.forEach {
                drawCircle(Color.Red, radius = enemyRadius, center = it.position)
            }

            // Joystick analog
            drawCircle(Color.Gray, radius = joystickRadius, center = joystickCenter)
            drawCircle(Color.DarkGray, radius = knobRadius, center = joystickCenter + joystickOffset)
        }

        // Joystick gesture
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            val dx = it.x - joystickCenter.x
                            val dy = it.y - joystickCenter.y
                            if (sqrt(dx * dx + dy * dy) <= joystickRadius) {
                                joystickOffset = Offset(dx, dy).coerceInCircle(joystickRadius - knobRadius)
                            }
                        },
                        onDrag = { change, _ ->
                            val dx = change.position.x - joystickCenter.x
                            val dy = change.position.y - joystickCenter.y
                            joystickOffset = Offset(dx, dy).coerceInCircle(joystickRadius - knobRadius)
                        },
                        onDragEnd = {
                            joystickOffset = Offset.Zero
                        }
                    )
                }
        )

        // HP dan skor
        Text(
            text = "HP Telur: $eggHP",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        )
        Text(
            text = "Score: $score",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 40.dp)
        )

        // Game Over Screen
        if (isGameOver) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xAA000000))
                    .align(Alignment.Center)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text("Game Over", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                    Text("Score: $score", fontSize = 24.sp, color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        eggHP = 10
                        score = 0
                        enemies = listOf()
                        isGameOver = false
                        playerPos = Offset(widthPx / 2, heightPx - 100)
                        joystickOffset = Offset.Zero
                    }) {
                        Text("Restart")
                    }
                }
            }
        }
    }
}

// Extension function
fun Offset.getDistance(): Float = sqrt(x * x + y * y)
fun Offset.getDistanceTo(other: Offset): Float = (this - other).getDistance()
fun Offset.coerceInCircle(radius: Float): Offset {
    val dist = getDistance()
    return if (dist <= radius) this else this * (radius / dist)
}
