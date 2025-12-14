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
import kotlin.random.Random


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        setContent {
            GameApp()
        }
    }
}

@Composable
fun GameApp() {
    var gameState by remember { mutableStateOf(GameState.MENU) } // MENU, PLAYING, PAUSED, GAME_OVER

    when (gameState) {
        GameState.MENU -> MainMenuScreen { gameState = GameState.PLAYING }
        GameState.PLAYING -> GameScreen(
            onGameOver = { gameState = GameState.GAME_OVER },
            onPause = { gameState = GameState.PAUSED }
        )
        GameState.PAUSED -> PauseScreen(
            onResume = { gameState = GameState.PLAYING },
            onRestart = { gameState = GameState.PLAYING },
            onQuit = { gameState = GameState.MENU }
        )
        GameState.GAME_OVER -> GameOverScreen(
            onRestart = { gameState = GameState.PLAYING },
            onQuit = { gameState = GameState.MENU }
        )
    }
}

enum class GameState {
    MENU, PLAYING, PAUSED, GAME_OVER
}

@Composable
fun MainMenuScreen(onStartGame: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "SELAMAT DATANG",
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = "PENJAGA BAKSO",
                color = Color.Yellow,
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 40.dp)
            )

            Button(
                onClick = onStartGame,
                modifier = Modifier
                    .width(200.dp)
                    .height(60.dp)
            ) {
                Text("PLAY GAME", fontSize = 20.sp)
            }
        }
    }
}

@Composable
fun PauseScreen(
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onQuit: () -> Unit
) {
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
            Text("PAUSED", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onResume,
                modifier = Modifier
                    .width(200.dp)
                    .height(50.dp)
            ) {
                Text("RESUME", fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onRestart,
                modifier = Modifier
                    .width(200.dp)
                    .height(50.dp)
            ) {
                Text("RESTART", fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onQuit,
                modifier = Modifier
                    .width(200.dp)
                    .height(50.dp)
            ) {
                Text("QUIT", fontSize = 18.sp)
            }
        }
    }
}

@Composable
fun GameScreen(
    onGameOver: () -> Unit,
    onPause: () -> Unit
) {
    var eggHP by remember { mutableStateOf(10) }
    var score by remember { mutableStateOf(0) }
    var isPaused by remember { mutableStateOf(false) }

    val playerSize = 60f
    val eggRadius = 40f
    val enemyRadius = 30f
    val joystickRadius = 120f
    val knobRadius = 40f
    val enemySpeed = 80f
    val enemyRotateSpeed = 2f

    var playerPos by remember { mutableStateOf(Offset.Zero) }
    var joystickCenter by remember { mutableStateOf(Offset.Zero) }
    var joystickOffset by remember { mutableStateOf(Offset.Zero) }
    var enemies by remember { mutableStateOf(listOf<Enemy>()) }

    var widthPx by remember { mutableStateOf(0f) }
    var heightPx by remember { mutableStateOf(0f) }
    var lastFrameTime by remember { mutableStateOf(0L) }

    val scope = rememberCoroutineScope()

    // Game loop
    LaunchedEffect(Unit) {
        while (true) {
            val currentTime = System.currentTimeMillis()
            val deltaTime = if (lastFrameTime == 0L) 16L else (currentTime - lastFrameTime).coerceAtMost(50L)
            val deltaTimeSec = deltaTime / 1000f

            if (!isPaused && eggHP > 0) {
                // Player movement
                playerPos += joystickOffset * 0.2f * deltaTimeSec * 60f

                // Screen boundaries
                playerPos = Offset(
                    x = playerPos.x.coerceIn(0f, widthPx),
                    y = playerPos.y.coerceIn(0f, heightPx)
                )

                // Update enemies
                val eggCenter = Offset(widthPx / 2, heightPx / 2)
                enemies = enemies.map { enemy ->
                    val direction = eggCenter - enemy.position
                    val distance = direction.getDistance()

                    val targetVelocity = if (distance > 5f) {
                        (direction / distance) * enemySpeed * deltaTimeSec
                    } else {
                        Offset.Zero
                    }

                    enemy.velocity = lerp(
                        enemy.velocity,
                        targetVelocity,
                        5f * deltaTimeSec
                    )

                    val newPosition = enemy.position + enemy.velocity
                    val newAngle = enemy.angle + enemyRotateSpeed * deltaTimeSec

                    enemy.copy(
                        position = newPosition,
                        velocity = enemy.velocity,
                        angle = newAngle
                    )
                }.toList()

                // Game logic
                scope.launch {
                    val now = System.currentTimeMillis()
                    val newEnemies = enemies.toMutableList()

                    // Collision with player
                    newEnemies.removeAll { enemy ->
                        if (enemy.position.getDistanceTo(playerPos) <= (playerSize / 2 + enemyRadius)) {
                            score += 10
                            true
                        } else {
                            false
                        }
                    }

                    // Attack egg
                    newEnemies.forEach { enemy ->
                        if (enemy.position.getDistanceTo(eggCenter) <= eggRadius + enemyRadius) {
                            if (now - enemy.lastAttackTime >= 1000L) {
                                eggHP--
                                enemy.lastAttackTime = now
                            }
                        }
                    }

                    // Spawn enemies
                    val expectedCount = min(20, 3 + (score / 100) * 2)
                    if (newEnemies.size < expectedCount) {
                        val spawnPosition = when (Random.nextInt(4)) {
                            0 -> Offset(0f, Random.nextFloat() * heightPx)
                            1 -> Offset(widthPx, Random.nextFloat() * heightPx)
                            2 -> Offset(Random.nextFloat() * widthPx, 0f)
                            else -> Offset(Random.nextFloat() * widthPx, heightPx)
                        }
                        newEnemies.add(Enemy(position = spawnPosition))
                    }
                    enemies = newEnemies

                    if (eggHP <= 0) {
                        onGameOver()
                    }
                }
            }

            lastFrameTime = currentTime
            delay(8)
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

            // Enemies
            enemies.forEach { enemy ->
                drawCircle(Color.Red, radius = enemyRadius, center = enemy.position)

                for (i in 0 until 6) {
                    val spikeAngle = enemy.angle + i * (2 * PI / 6).toFloat()
                    val spikeEnd = enemy.position + Offset(
                        cos(spikeAngle) * enemyRadius * 1.2f,
                        sin(spikeAngle) * enemyRadius * 1.2f
                    )
                    drawLine(
                        Color.Red.copy(alpha = 0.7f),
                        enemy.position,
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
                        onDragStart = { offset ->
                            val dx = offset.x - joystickCenter.x
                            val dy = offset.y - joystickCenter.y
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

        // Pause button
        Button(
            onClick = {
                isPaused = true
                onPause()
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Text("PAUSE", fontSize = 16.sp)
        }
    }
}

@Composable
fun GameOverScreen(
    score: Int = 0,
    onRestart: () -> Unit,
    onQuit: () -> Unit
) {
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
                onClick = onRestart,
                modifier = Modifier
                    .width(200.dp)
                    .height(50.dp)
            ) {
                Text("PLAY AGAIN", fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onQuit,
                modifier = Modifier
                    .width(200.dp)
                    .height(50.dp)
            ) {
                Text("MAIN MENU", fontSize = 18.sp)
            }
        }
    }
}

fun Offset.getDistance(): Float = sqrt(x * x + y * y)
fun Offset.getDistanceTo(other: Offset): Float = (this - other).getDistance()
fun Offset.coerceInCircle(radius: Float): Offset {
    val dist = getDistance()
    return if (dist <= radius) this else this * (radius / dist)
}

fun lerp(start: Offset, end: Offset, alpha: Float): Offset {
    return start + (end - start) * alpha.coerceIn(0f, 1f)
}

data class Enemy(
    var position: Offset,
    var velocity: Offset = Offset.Zero,
    var lastAttackTime: Long = 0L,
    var angle: Float = 0f
)
