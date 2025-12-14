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
import androidx.compose.ui.geometry.Size
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.graphics.asAndroidBitmap
import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Paint
import androidx.compose.foundation.background
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.sqrt

fun Offset.normalize(): Offset {
    val length = sqrt(this.x * this.x + this.y * this.y)
    return if (length != 0f) Offset(this.x / length, this.y / length) else Offset.Zero
}

fun extractFrame(
    spriteSheet: ImageBitmap,
    frameIndex: Int,
    frameWidth: Int,
    frameHeight: Int
): ImageBitmap {
    if (frameWidth <= 0 || frameHeight <= 0) return spriteSheet

    val cols = spriteSheet.width / frameWidth
    val rows = spriteSheet.height / frameHeight
    val totalFrames = cols * rows

    if (totalFrames <= 0) return spriteSheet

    val safeFrameIndex = frameIndex % totalFrames
    val x = (safeFrameIndex % cols) * frameWidth
    val y = (safeFrameIndex / cols) * frameHeight

    val target = ImageBitmap(frameWidth, frameHeight)
    val canvas = androidx.compose.ui.graphics.Canvas(target)
    canvas.drawImageRect(
        image = spriteSheet,
        srcOffset = IntOffset(x, y),
        srcSize = IntSize(frameWidth, frameHeight),
        dstSize = IntSize(frameWidth, frameHeight),
        paint = Paint()
    )
    return target
}


fun ImageBitmap.crop(x: Int, y: Int, width: Int, height: Int): ImageBitmap {
    val target = ImageBitmap(width, height)
    val canvas = Canvas(target)
    canvas.drawImageRect(
        this,
        srcOffset = IntOffset(x, y),
        srcSize = IntSize(width, height),
        dstOffset = IntOffset(0, 0),
        dstSize = IntSize(width, height),
        paint = Paint()
    )
    return target
}
fun getFrameOffset(frameIndex: Int, frameWidth: Int, frameHeight: Int, sheetWidth: Int): IntOffset {
    val x = (frameIndex % (sheetWidth / frameWidth)) * frameWidth
    val y = (frameIndex / (sheetWidth / frameWidth)) * frameHeight
    return IntOffset(x, y)
}










fun loadCharacterSprite(context: Context): ImageBitmap {
    val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.mc_stay)
    return bitmap.asImageBitmap()
}

fun getFrameFromSpriteSheet(
    spriteSheet: ImageBitmap,
    frameWidth: Int,
    frameHeight: Int,
    frameIndex: Int
): ImageBitmap {
    val columns = spriteSheet.width / frameWidth
    val srcX = (frameIndex % columns) * frameWidth
    val srcY = (frameIndex / columns) * frameHeight
    val androidBitmap = spriteSheet.asAndroidBitmap()
    val cropped = Bitmap.createBitmap(androidBitmap, srcX, srcY, frameWidth, frameHeight)
    return cropped.asImageBitmap()
}


data class Enemy(
    var position: Offset,
    var velocity: Offset = Offset.Zero,
    var lastAttackTime: Long = 0L,
    var angle: Float = 0f
)

data class GameStateData(
    val eggHP: Int = 10,
    val score: Int = 0,
    val playerPos: Offset = Offset.Zero,
    val enemies: List<Enemy> = emptyList(),
    var joystickOffset: Offset = Offset.Zero,
    val joystickCenter: Offset = Offset.Zero,
    val screenWidth: Float = 0f,
    val screenHeight: Float = 0f
)


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        setContent {
            GameApp()
        }
    }
}

enum class GameScreenState {
    MENU, PLAYING, PAUSED, GAME_OVER
}

@Composable
fun GameApp() {
    var currentScreen by remember { mutableStateOf(GameScreenState.MENU) }
    var gameState by remember { mutableStateOf(GameStateData()) }

    when (currentScreen) {
        GameScreenState.MENU -> MainMenuScreen {
            // Reset state when starting new game from menu
            gameState = GameStateData()
            currentScreen = GameScreenState.PLAYING
        }
        GameScreenState.PLAYING -> GameScreen(
            gameState = gameState,
            onGameOver = { newState ->
                gameState = newState
                currentScreen = GameScreenState.GAME_OVER
            },
            onPause = { newState ->
                gameState = newState
                currentScreen = GameScreenState.PAUSED
            }
        )
        GameScreenState.PAUSED -> PauseScreen(
            currentScore = gameState.score,
            onResume = {
                gameState = gameState.copy(joystickOffset = Offset.Zero)
                currentScreen = GameScreenState.PLAYING
            },
            onRestart = {
                // Reset state when restarting from pause
                gameState = GameStateData()
                currentScreen = GameScreenState.PLAYING
            },
            onQuit = {
                // Reset state when quitting to menu
                gameState = GameStateData()
                currentScreen = GameScreenState.MENU
            }
        )
        GameScreenState.GAME_OVER -> GameOverScreen(
            score = gameState.score,
            onRestart = {
                // Reset state when restarting from game over
                gameState = GameStateData()
                currentScreen = GameScreenState.PLAYING
            },
            onQuit = {
                // Reset state when quitting to menu
                gameState = GameStateData()
                currentScreen = GameScreenState.MENU
            }
        )
    }
}

@Composable
fun loadCharacterSprite(): ImageBitmap {
    val context = LocalContext.current
    val drawable = context.resources.getIdentifier("mc_stay", "drawable", context.packageName)
    return ImageBitmap.imageResource(id = drawable)
}


@Composable
fun GameScreen(
    gameState: GameStateData,
    onGameOver: (GameStateData) -> Unit,
    onPause: (GameStateData) -> Unit
) {
    val context = LocalContext.current
    var eggHP by remember { mutableStateOf(gameState.eggHP) }
    var score by remember { mutableStateOf(gameState.score) }
    var playerPos by remember { mutableStateOf(gameState.playerPos) }
    var joystickCenter by remember { mutableStateOf(gameState.joystickCenter) }
    var joystickOffset by remember { mutableStateOf(gameState.joystickOffset) }
    var enemies by remember { mutableStateOf(gameState.enemies) }
    var isPaused by remember { mutableStateOf(false) }
    var screenWidthPx by remember { mutableStateOf(gameState.screenWidth) }
    var screenHeightPx by remember { mutableStateOf(gameState.screenHeight) }
    val playerSprite = ImageBitmap.imageResource(context.resources, R.drawable.mc_stay)

    // 🔹 Context & SpriteSheet dari drawable
    val spriteSheet = remember { loadCharacterSprite(context) }

    // 🔹 Ukuran-ukuran game
    val playerSize = 128f
    val eggRadius = 70f
    val eggHitbox = 50f
    val enemyRadius = 30f
    val joystickRadius = 120f
    val knobRadius = 40f
    val enemySpeed = 80f
    val enemyRotateSpeed = 2f

    val scope = rememberCoroutineScope()
    var lastFrameTime by remember { mutableStateOf(0L) }

    var frameIndex by remember { mutableStateOf(0) }
    val playerIdle = ImageBitmap.imageResource(context.resources, R.drawable.mc_stay)
    val playerMove = ImageBitmap.imageResource(context.resources, R.drawable.mc_move)


    val idleSprite = ImageBitmap.imageResource(context.resources, R.drawable.mc_stay) // 96x32
    val walkSprite = ImageBitmap.imageResource(context.resources, R.drawable.mc_move) // 64x32

    val idleFrames = 3
    val walkFrames = 2
    val frameWidth = 32
    val frameHeight = 32

    var currentFrame by remember { mutableStateOf(0) }

    LaunchedEffect(joystickOffset) {
        while (true) {
            delay(if (joystickOffset != Offset.Zero) 120L else 200L)
            val maxFrames = if (joystickOffset != Offset.Zero) walkFrames else idleFrames
            currentFrame = (currentFrame + 1) % maxFrames
        }
    }


    LaunchedEffect(Unit) {
        while (true) {
            delay(150) // ganti frame tiap 150 ms
            frameIndex = (frameIndex + 1) % 3 // misal sprite sheet punya 3 frame
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            val currentTime = System.currentTimeMillis()
            val deltaTime = if (lastFrameTime == 0L) 16L else (currentTime - lastFrameTime).coerceAtMost(50L)
            val deltaTimeSec = deltaTime / 1000f

            if (!isPaused && eggHP > 0) {
                // Update player position
                playerPos += joystickOffset * 0.2f * deltaTimeSec * 60f
                playerPos = playerPos.coerceIn(0f, screenWidthPx, 0f, screenHeightPx)

                // Update enemies
                val eggCenter = Offset(screenWidthPx / 2, screenHeightPx / 2)
                enemies = enemies.map { enemy ->
                    val toEgg = eggCenter - enemy.position
                    val distanceToEgg = toEgg.getDistance()
                    val shouldMove = distanceToEgg > (eggHitbox + enemyRadius)
                    val direction = if (shouldMove) toEgg.normalize() else Offset.Zero

                    val targetVelocity = direction * enemySpeed
                    val newVelocity = lerp(enemy.velocity, targetVelocity, 5f * deltaTimeSec)
                    val newPosition = enemy.position + newVelocity * deltaTimeSec
                    val newAngle = enemy.angle + enemyRotateSpeed * deltaTimeSec

                    enemy.copy(position = newPosition, velocity = newVelocity, angle = newAngle)
                }

                // Game logic
                scope.launch {
                    val newEnemies = enemies.toMutableList()
                    val now = System.currentTimeMillis()

                    // Player collision
                    newEnemies.removeAll { enemy ->
                        if (enemy.position.getDistanceTo(playerPos) <= (playerSize / 2 + enemyRadius)) {
                            score += 10
                            true
                        } else false
                    }

                    // Egg attack
                    newEnemies.forEach { enemy ->
                        if (enemy.position.getDistanceTo(eggCenter) <= (eggHitbox + enemyRadius) &&
                            now - enemy.lastAttackTime >= 1000L
                        ) {
                            eggHP--
                            enemy.lastAttackTime = now
                        }
                    }

                    // Spawn enemies
                    val expectedCount = min(20, 3 + (score / 100) * 2)
                    if (newEnemies.size < expectedCount) {
                        val spawnPos = when (Random.nextInt(4)) {
                            0 -> Offset(0f, Random.nextFloat() * screenHeightPx)
                            1 -> Offset(screenWidthPx, Random.nextFloat() * screenHeightPx)
                            2 -> Offset(Random.nextFloat() * screenWidthPx, 0f)
                            else -> Offset(Random.nextFloat() * screenWidthPx, screenHeightPx)
                        }
                        newEnemies.add(Enemy(position = spawnPos))
                    }

                    enemies = newEnemies

                    if (eggHP <= 0) {
                        onGameOver(
                            GameStateData(
                                eggHP,
                                score,
                                playerPos,
                                enemies,
                                joystickOffset,
                                joystickCenter,
                                screenWidthPx,
                                screenHeightPx
                            )
                        )
                    }
                }
            }
            lastFrameTime = currentTime
            delay(8)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF006400))) {
        Canvas(modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned {
                val w = it.size.width.toFloat()
                val h = it.size.height.toFloat()
                if (screenWidthPx != w || screenHeightPx != h) {
                    screenWidthPx = w
                    screenHeightPx = h
                    if (playerPos == Offset.Zero) {
                        playerPos = Offset(w / 2, h - 100f)
                        joystickCenter = Offset(200f, h - 200f)
                    }
                }
            }
        ) {
            // Gambar Telur
            drawCircle(Color.Yellow, eggRadius, Offset(screenWidthPx / 2, screenHeightPx / 2))

            // 🔹 Ambil frame dari spritesheet
            val frameWidth = 32
            val frameHeight = 32

// Tentukan animasi berdasarkan gerakan
            val isMoving = joystickOffset.getDistance() > 0.5f
            val spriteSheet = if (isMoving) playerMove else playerIdle

// Jumlah frame tergantung sprite yang dipakai
            val totalFrames = spriteSheet.width / frameWidth
            val frameIndex = ((System.currentTimeMillis() / 200) % totalFrames).toInt()

            val playerFrame = extractFrame(spriteSheet, frameIndex, frameWidth, frameHeight)

            drawImage(
                image = playerFrame,
                dstSize = IntSize(playerSize.toInt(), playerSize.toInt()),
                dstOffset = IntOffset(
                    (playerPos.x - playerSize / 2).toInt(),
                    (playerPos.y - playerSize / 2).toInt()
                )
            )


            // Gambar musuh
            enemies.forEach { enemy ->
                drawCircle(Color.Red, enemyRadius, enemy.position)
            }

            // Joystick
            drawCircle(Color.Gray.copy(0.6f), joystickRadius, joystickCenter)
            drawCircle(Color.DarkGray.copy(0.8f), knobRadius, joystickCenter + joystickOffset)
        }

        // Input Joystick
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val vec = offset - joystickCenter
                            if (vec.getDistance() <= joystickRadius) {
                                joystickOffset = vec.coerceInCircle(joystickRadius - knobRadius)
                            }
                        },
                        onDrag = { change, _ ->
                            val vec = change.position - joystickCenter
                            joystickOffset = vec.coerceInCircle(joystickRadius - knobRadius)
                        },
                        onDragEnd = {
                            joystickOffset = Offset.Zero
                        }
                    )
                }
        )

        // UI atas
        Column(Modifier.padding(16.dp)) {
            Text("HP Telur: $eggHP", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Score: $score", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp))
        }

        // Tombol Pause
        Button(
            onClick = {
                isPaused = true
                onPause(
                    GameStateData(
                        eggHP,
                        score,
                        playerPos,
                        enemies,
                        joystickOffset,
                        joystickCenter,
                        screenWidthPx,
                        screenHeightPx
                    )
                )
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
                "PENJAGA BAKSO",
                color = Color.Yellow,
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 40.dp)
            )
            Button(
                onClick = onStartGame,
                modifier = Modifier.width(200.dp).height(60.dp)
            ) {
                Text("PLAY GAME", fontSize = 20.sp)
            }
        }
    }
}

@Composable
fun PauseScreen(
    currentScore: Int,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onQuit: () -> Unit
) {
    Box(Modifier.fillMaxSize().background(Color(0xAA000000))) {
        Column(
            Modifier.fillMaxSize(),
            Arrangement.Center,
            Alignment.CenterHorizontally
        ) {
            Text("PAUSED", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("Score: $currentScore", fontSize = 24.sp, color = Color.White,
                modifier = Modifier.padding(vertical = 16.dp))

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onResume,
                modifier = Modifier.width(200.dp).height(50.dp)
            ) {
                Text("RESUME", fontSize = 18.sp)
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onRestart,
                modifier = Modifier.width(200.dp).height(50.dp)
            ) {
                Text("RESTART", fontSize = 18.sp)
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onQuit,
                modifier = Modifier.width(200.dp).height(50.dp)
            ) {
                Text("QUIT", fontSize = 18.sp)
            }
        }
    }
}

@Composable
fun GameOverScreen(
    score: Int,
    onRestart: () -> Unit,
    onQuit: () -> Unit
) {
    Box(Modifier.fillMaxSize().background(Color(0xAA000000))) {
        Column(
            Modifier.fillMaxSize(),
            Arrangement.Center,
            Alignment.CenterHorizontally
        ) {
            Text("GAME OVER", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color.Red)
            Text("Score: $score", fontSize = 28.sp, color = Color.White,
                modifier = Modifier.padding(top = 16.dp))

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onRestart,
                modifier = Modifier.width(200.dp).height(50.dp)
            ) {
                Text("PLAY AGAIN", fontSize = 18.sp)
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onQuit,
                modifier = Modifier.width(200.dp).height(50.dp)
            ) {
                Text("MAIN MENU", fontSize = 18.sp)
            }
        }
    }
}

// Helper extensions
fun Offset.getDistance(): Float = sqrt(x * x + y * y)
fun Offset.getDistanceTo(other: Offset): Float = (this - other).getDistance()
fun Offset.coerceInCircle(radius: Float): Offset {
    val dist = getDistance()
    return if (dist <= radius) this else this * (radius / dist)
}

fun Offset.coerceIn(minX: Float, maxX: Float, minY: Float, maxY: Float): Offset {
    return Offset(x.coerceIn(minX, maxX), y.coerceIn(minY, maxY))
}

fun lerp(start: Offset, end: Offset, alpha: Float): Offset {
    return start + (end - start) * alpha.coerceIn(0f, 1f)
}
