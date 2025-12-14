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
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.atan2

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        setContent {
            GameScreen()
        }
    }
}

data class Enemy(
    var position: Offset,
    var velocity: Offset = Offset.Zero,
    var lastAttackTime: Long = 0L,
    var angle: Float = 0f // Untuk animasi rotasi
)

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
    val enemySpeed = 80f // Sekarang dalam piksel/detik
    val enemyRotateSpeed = 2f // Kecepatan rotasi musuh

    var playerPos by remember { mutableStateOf(Offset.Zero) }
    var joystickCenter by remember { mutableStateOf(Offset.Zero) }
    var joystickOffset by remember { mutableStateOf(Offset.Zero) }
    var enemies by remember { mutableStateOf(listOf<Enemy>()) }

    var widthPx by remember { mutableStateOf(0f) }
    var heightPx by remember { mutableStateOf(0f) }
    var lastFrameTime by remember { mutableStateOf(0L) }

    // Player movement effect dijalankan dalam coroutine terpisah
    val scope = rememberCoroutineScope()

    // Game loop yang terpisah untuk update posisi
    LaunchedEffect(Unit) {
        while (true) {
            val currentTime = System.currentTimeMillis()
            val deltaTime = if (lastFrameTime == 0L) 16L else (currentTime - lastFrameTime).coerceAtMost(50L)
            val deltaTimeSec = deltaTime / 1000f

            if (!isGameOver) {
                // Update player position based on joystick
                playerPos = playerPos + (joystickOffset * 0.2f * deltaTimeSec * 60f)

                // Screen boundaries
                playerPos = Offset(
                    x = playerPos.x.coerceIn(0f, widthPx),
                    y = playerPos.y.coerceIn(0f, heightPx)
                )

                // Update enemies
                val eggCenter = Offset(widthPx / 2, heightPx / 2)
                enemies = enemies.map { enemy ->
                    // Hitung arah ke telur
                    val direction = eggCenter - enemy.position
                    val distance = direction.getDistance()

                    // Update velocity dengan lerp untuk transisi mulus
                    val targetVelocity = if (distance > 5f) {
                        (direction / distance) * enemySpeed * deltaTimeSec
                    } else {
                        Offset.Zero
                    }

                    // Terapkan lerp untuk menghaluskan perubahan arah
                    enemy.velocity = lerp(
                        enemy.velocity,
                        targetVelocity,
                        5f * deltaTimeSec
                    )

                    // Update posisi
                    val newPosition = enemy.position + enemy.velocity

                    // Update sudut rotasi untuk animasi
                    val newAngle = enemy.angle + enemyRotateSpeed * deltaTimeSec

                    // Update enemy
                    enemy.copy(
                        position = newPosition,
                        velocity = enemy.velocity,
                        angle = newAngle
                    )
                }.toList()

                // Cek collision dan kondisi game lainnya
                scope.launch {
                    // Logika collision dan spawn musuh
                    val now = System.currentTimeMillis()
                    val newEnemies = enemies.toMutableList()

                    // Hapus musuh yang kena player
                    val hitEnemies = newEnemies.filter {
                        it.position.getDistanceTo(playerPos) <= (playerSize / 2 + enemyRadius)
                    }
                    if (hitEnemies.isNotEmpty()) {
                        newEnemies.removeAll(hitEnemies.toSet())
                        score += hitEnemies.size * 10
                    }

                    // Musuh menyerang telur
                    newEnemies.forEach { enemy ->
                        if (enemy.position.getDistanceTo(eggCenter) <= 5f) {
                            if (now - enemy.lastAttackTime >= 1000L) {
                                eggHP--
                                enemy.lastAttackTime = now
                            }
                        }
                    }

                    // Spawn musuh baru jika perlu
                    val expectedCount = min(20, 3 + (score / 100) * 2)
                    if (newEnemies.size < expectedCount) {
                        newEnemies.add(Enemy(
                            position = when ((0..3).random()) {
                                0 -> Offset(0f, (0..heightPx.toInt()).random().toFloat())
                                1 -> Offset(widthPx, (0..heightPx.toInt()).random().toFloat())
                                2 -> Offset((0..widthPx.toInt()).random().toFloat(), 0f)
                                else -> Offset((0..widthPx.toInt()).random().toFloat(), heightPx)
                            },
                            velocity = Offset.Zero
                        ))
                    }

                    enemies = newEnemies

                    // Cek game over
                    if (eggHP <= 0) {
                        isGameOver = true
                    }
                }
            }

            lastFrameTime = currentTime
            delay(8) // Lebih tinggi FPS untuk animasi lebih mulus
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Canvas(modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned {
                widthPx = it.size.width.toFloat()
                heightPx = it.size.height.toFloat()
                if (playerPos == Offset.Zero) {
                    playerPos = Offset(widthPx / 2, heightPx - 100f)
                    joystickCenter = Offset(200f, heightPx - 200f)
                }
            }
        ) {
            // Draw game elements
            drawCircle(Color.Yellow, radius = eggRadius, center = Offset(widthPx / 2, heightPx / 2))

            // Player
            drawRect(
                Color.Cyan,
                topLeft = playerPos - Offset(playerSize / 2, playerSize / 2),
                size = androidx.compose.ui.geometry.Size(playerSize, playerSize)
            )

            // Enemies dengan animasi rotasi
            enemies.forEach { enemy ->
                // Gambar musuh dengan animasi rotasi kecil
                val rotationCenter = enemy.position
                val spikeLength = enemyRadius * 1.2f

                // Gambar lingkaran dasar
                drawCircle(Color.Red, radius = enemyRadius, center = enemy.position)

                // Gambar "spikes" untuk efek animasi
                for (i in 0 until 6) {
                    val spikeAngle = enemy.angle + i * (2 * PI / 6).toFloat()
                    val spikeEnd = rotationCenter + Offset(
                        cos(spikeAngle) * spikeLength,
                        sin(spikeAngle) * spikeLength
                    )
                    drawLine(
                        Color.Red.copy(alpha = 0.7f),
                        rotationCenter,
                        spikeEnd,
                        strokeWidth = 3f
                    )
                }
            }

            // Joystick
            drawCircle(Color.Gray.copy(alpha = 0.6f), radius = joystickRadius, center = joystickCenter)
            drawCircle(Color.DarkGray.copy(alpha = 0.8f), radius = knobRadius, center = joystickCenter + joystickOffset)
        }

        // Joystick control
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

        // Game UI
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "HP Telur: $eggHP",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Score: $score",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Game Over Screen
        if (isGameOver) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xAA000000))
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text("GAME OVER", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                    Text("Score: $score", fontSize = 28.sp, color = Color.White, modifier = Modifier.padding(top = 16.dp))
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            eggHP = 10
                            score = 0
                            enemies = emptyList()
                            isGameOver = false
                            playerPos = Offset(widthPx / 2, heightPx - 100f)
                        },
                        modifier = Modifier
                            .padding(horizontal = 32.dp)
                            .height(50.dp)
                            .width(200.dp)
                    ) {
                        Text("PLAY AGAIN", fontSize = 20.sp)
                    }
                }
            }
        }
    }
}

// Helper functions
fun Offset.getDistance(): Float = sqrt(x * x + y * y)
fun Offset.getDistanceTo(other: Offset): Float = (this - other).getDistance()
fun Offset.coerceInCircle(radius: Float): Offset {
    val dist = getDistance()
    return if (dist <= radius) this else this * (radius / dist)
}

// Linear interpolation untuk smoothing pergerakan
fun lerp(start: Offset, end: Offset, alpha: Float): Offset {
    return start + (end - start) * alpha.coerceIn(0f, 1f)
}
