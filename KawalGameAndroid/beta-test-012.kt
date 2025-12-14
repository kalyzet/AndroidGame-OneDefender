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
import androidx.compose.ui.graphics.drawscope.withTransform
import android.media.MediaPlayer
import android.media.SoundPool
import android.media.AudioAttributes
import android.util.Log
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp





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
lateinit var bgMusic: MediaPlayer
lateinit var soundPool: SoundPool
var soundButtonClick = 0
var soundEnemyAttack = 0
var soundGameOver = 0
var soundHitEnemy = 0
var soundHeal = 0

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
    val joystickOffset: Offset = Offset.Zero,
    val joystickCenter: Offset = Offset.Zero,
    val screenWidthPx: Float = 1920f,
    val screenHeightPx: Float = 1080f,
    val healItems: List<Offset> = emptyList(),
    val lastHealSpawnTime: Long = 0L
)





class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        setContent {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            soundPool = SoundPool.Builder()
                .setMaxStreams(6)
                .setAudioAttributes(audioAttributes)
                .build()
            soundButtonClick = soundPool.load(this, R.raw.button_click, 1)
            soundEnemyAttack = soundPool.load(this, R.raw.enemy_attack, 1)
            soundGameOver = soundPool.load(this, R.raw.game_over, 1)
            soundHitEnemy = soundPool.load(this, R.raw.hit_enemy, 1)
            soundHeal = soundPool.load(this, R.raw.heal_sound, 1)

            // 🔊 BGM
            bgMusic = MediaPlayer.create(this, R.raw.bgmusic)
            bgMusic.isLooping = true
            bgMusic.start()

            GameApp()
        }
    }

    override fun onPause() {
        super.onPause()
        if (::bgMusic.isInitialized && bgMusic.isPlaying) {
            bgMusic.pause()
        }
        GameAppStateHolder.pauseGame()   // sama kayak tombol pause
    }

    override fun onResume() {
        super.onResume()
        if (::bgMusic.isInitialized && !bgMusic.isPlaying) {
            bgMusic.start()
        }
        GameAppStateHolder.resumeGame()  // sama kayak tombol resume
    }


} // ⬅️ ini penutup MainActivity



// 🔹 Simpan state global supaya bisa restore saat minimize
object GameAppStateHolder {
    var onPauseGame: (() -> Unit)? = null
    var onResumeGame: (() -> Unit)? = null

    // 🔹 Simpan state game biar ga hilang
    var currentScreen: GameScreenState = GameScreenState.MENU
    var gameState: GameStateData = GameStateData()

    fun pauseGame() {
        if (currentScreen == GameScreenState.PLAYING) {
            currentScreen = GameScreenState.PAUSED
        }
        onPauseGame?.invoke()
    }

    fun resumeGame() {
        if (currentScreen == GameScreenState.PAUSED) {
            currentScreen = GameScreenState.PLAYING
        }
        onResumeGame?.invoke()
    }
}





enum class GameScreenState {
    MENU, PLAYING, PAUSED, GAME_OVER
}


@Composable
fun GameBackground() {
    // Ambil context dari Compose
    val context = LocalContext.current

    // Load image background dari drawable
    val backgroundImage = remember {
        ImageBitmap.imageResource(res = context.resources, id = R.drawable.background)
    }



    // Gambar full screen
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawImage(
                image = backgroundImage,
                dstSize = IntSize(size.width.toInt(), size.height.toInt())
            )
        }
    }
}

@Composable
fun GameApp() {
    var currentScreen by remember { mutableStateOf(GameAppStateHolder.currentScreen) }
    var gameState by remember { mutableStateOf(GameAppStateHolder.gameState) }

    // sinkronkan
    DisposableEffect(currentScreen, gameState) {
        GameAppStateHolder.currentScreen = currentScreen
        GameAppStateHolder.gameState = gameState
        onDispose { }
    }



    when (currentScreen) {
        GameScreenState.MENU -> MainMenuScreen {
            bgMusic.setVolume(1f, 1f) // 100% volume saat game over

            // Reset state when starting new game from menu
            gameState = GameStateData()
            currentScreen = GameScreenState.PLAYING
        }
        GameScreenState.PLAYING -> {
            bgMusic.setVolume(0.2f, 0.2f) // 50% volume saat in-game

            GameScreen(
                gameState = gameState,
                onGameOver = { newState ->
                    gameState = newState
                    currentScreen = GameScreenState.GAME_OVER
                },
                onPause = { newState ->
                    gameState = newState
                    currentScreen = GameScreenState.PAUSED
                },
                isGamePlaying = currentScreen == GameScreenState.PLAYING,
            )
        }

        GameScreenState.PAUSED -> {
            bgMusic.setVolume(1f, 1f) // 100% volume saat pause

            PauseScreen(
                currentScore = gameState.score,
                onResume = {
                    gameState = gameState.copy(joystickOffset = Offset.Zero)
                    currentScreen = GameScreenState.PLAYING
                },
                onRestart = {
                    gameState = GameStateData()
                    currentScreen = GameScreenState.PLAYING
                },
                onQuit = {
                    gameState = GameStateData()
                    currentScreen = GameScreenState.MENU
                }
            )
        }

        GameScreenState.GAME_OVER -> {
            bgMusic.setVolume(1f, 1f) // 100% volume saat game over

            GameOverScreen(
                score = gameState.score,
                onRestart = {
                    gameState = GameStateData()
                    currentScreen = GameScreenState.PLAYING
                },
                onQuit = {
                    gameState = GameStateData()
                    currentScreen = GameScreenState.MENU
                }
            )
        }

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
    onPause: (GameStateData) -> Unit,
    isGamePlaying: Boolean
) {
    val context = LocalContext.current
    var eggHP by remember { mutableStateOf(gameState.eggHP) }
    var score by remember { mutableStateOf(gameState.score) }
    var playerPos by remember { mutableStateOf(gameState.playerPos) }
    var joystickCenter by remember { mutableStateOf(gameState.joystickCenter) }
    var joystickOffset by remember { mutableStateOf(gameState.joystickOffset) }
    var enemies by remember { mutableStateOf(gameState.enemies) }
    var isPaused by remember { mutableStateOf(false) }
    var screenWidthPx by remember { mutableStateOf(gameState.screenWidthPx) }
    var screenHeightPx by remember { mutableStateOf(gameState.screenHeightPx) }
    val playerSprite = ImageBitmap.imageResource(context.resources, R.drawable.mc_stay)


    var healItems by remember { mutableStateOf(gameState.healItems) }
    var lastHealSpawnTime by remember { mutableStateOf(gameState.lastHealSpawnTime) }



    // Context & SpriteShee drawable
    val spriteSheet = remember { loadCharacterSprite(context) }
// Posisi item heal "null = tidak ada di layar"
    val baseSize = min(screenWidthPx, screenHeightPx)

    // Ukuran-ukuran base game
    val playerSize = baseSize * 0.10f      // 10% layar
    val enemyRadius = baseSize * 0.065f     // musuh sedikit lebih besar biar jelas
    val eggRadius = baseSize * 0.18f        // telur lebih besar
    val eggHitbox = eggRadius * 1.00f       // sinkron dgn gambar telur
    val healRadius = baseSize * 0.035f      // heal lebih kecil, biar pas

// Kecepatan musuh skala layar (px/s)
    val enemySpeed = baseSize * 0.22f       // ~0.22 * min(w,h) per detik

    val joystickRadius = (screenWidthPx * 0.10f)
    val knobRadius = joystickRadius * 0.25f

    val enemyRotateSpeed = 2f

    val healSprite = ImageBitmap.imageResource(context.resources, R.drawable.heal_item1)
    val healFrames = 2
    val healFrameWidth = 32
    val healFrameHeight = 32
    var healFrameIndex by remember { mutableStateOf(0) }


    val scope = rememberCoroutineScope()

    var frameIndex by remember { mutableStateOf(0) }
    val playerIdle = ImageBitmap.imageResource(context.resources, R.drawable.mc_stay)
    val playerMove = ImageBitmap.imageResource(context.resources, R.drawable.mc_move)


    val idleSprite = ImageBitmap.imageResource(context.resources, R.drawable.mc_stay) // 96x32
    val walkSprite = ImageBitmap.imageResource(context.resources, R.drawable.mc_move) // 64x32
    var playerFacingRight by remember { mutableStateOf(true) }

    val idleFrames = 3
    val walkFrames = 2
    val frameWidth = 32
    val frameHeight = 32
    val idleFrameCount = 3
    val moveFrameCount = 3
    val eggImage = ImageBitmap.imageResource(context.resources, R.drawable.telur3)
    val idleSpriteSheet = ImageBitmap.imageResource(context.resources, R.drawable.mc_stay)
    val moveSpriteSheet = ImageBitmap.imageResource(context.resources, R.drawable.mc_move)

    val enemySpriteSheet = ImageBitmap.imageResource(context.resources, R.drawable.enemy_move)
    val enemyFrameCount = 4

    val enemyMoveFrames = 4
    val enemyStayFrames = 2
    val enemyFrameWidth = 32
    val enemyFrameHeight = 32

    var enemyFrameIndex by remember { mutableStateOf(0) }
    // Pasang joystick di pojok kiri bawah dengan margin 32.dp
    if (joystickCenter == Offset.Zero) {
        val margin: Float = with(LocalDensity.current) { 32.dp.toPx() }
        joystickCenter = Offset(
            joystickRadius + margin,
            screenHeightPx - joystickRadius - margin
        )
    }


    val enemyMoveSprite = ImageBitmap.imageResource(context.resources, R.drawable.enemy_move)
    val enemyStaySprite = ImageBitmap.imageResource(context.resources, R.drawable.enemy_stay)

// Pastikan soundPool sudah dibuat
    val soundWalk = remember {
        soundPool.load(context, R.raw.mc_walk, 1)
    }


// Animasi musuh
    LaunchedEffect(Unit) {
        while (true) {
            delay(250)
            enemyFrameIndex = (enemyFrameIndex + 1) % enemyMoveFrames
        }
    }




    var startTime by remember { mutableStateOf(System.currentTimeMillis()) }
    var gameElapsedTime by remember { mutableStateOf(0L) }
    var lastFrameTime by remember { mutableStateOf(System.currentTimeMillis()) }


// --- Animasi frame heal ---
    LaunchedEffect(Unit) {
        while (true) {
            delay(300) // ganti frame tiap 300 ms
            healFrameIndex = (healFrameIndex + 1) % healFrames
        }
    }

// --- Game loop ---
    val now = System.currentTimeMillis()


// Spawn heal tiap 30 detik setelah awal game
    // Spawn heal tiap 30 detik setelah awal game






    var currentFrame by remember { mutableStateOf(0) }
    if (joystickOffset.x > 0.5f) playerFacingRight = true
    else if (joystickOffset.x < -0.5f) playerFacingRight = false


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
    // Di awal GameScreen()
    // Variabel global/remember di GameScreen
    var isWalkingSoundPlaying by remember { mutableStateOf(false) }
    var walkingSoundId by remember { mutableStateOf(0) }

// Saat update joystickOffset:
    if (joystickOffset.getDistance() > 0.5f) {
        if (!isWalkingSoundPlaying) {
            walkingSoundId = soundPool.play(soundWalk, 1f, 1f, 0, -1, 1f) // -1 = loop terus
            isWalkingSoundPlaying = true
        }
    } else {
        if (isWalkingSoundPlaying) {
            soundPool.stop(walkingSoundId)
            isWalkingSoundPlaying = false
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
                    // beri "buffer" agar musuh berhenti sedikit di luar telur
                    val shouldMove = distanceToEgg > (eggHitbox + enemyRadius * 0.6f)
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
                    // Player collision (hapus musuh saat kena player)
                    val playerHitbox = playerSize * 0.60f
                    val enemyHitbox = enemyRadius * 0.85f

                    val removed = newEnemies.removeAll { enemy ->
                        val collided = enemy.position.getDistanceTo(playerPos) <= (playerHitbox + enemyHitbox)
                        if (collided) {
                            soundPool.play(soundHitEnemy, 1f, 1f, 0, 0, 1f)
                            score += 10
                        }
                        collided
                    }



                    // Egg attack
                    val eggCenter = Offset(screenWidthPx / 2, screenHeightPx / 2)
                    newEnemies.replaceAll { enemy ->
                        val dist = enemy.position.getDistanceTo(eggCenter)
                        if (dist <= (eggHitbox + enemyHitbox)) {
                            if (now - enemy.lastAttackTime >= 1_000L) {
                                eggHP = (eggHP - 1).coerceAtLeast(0)
                                soundPool.play(soundEnemyAttack, 1f, 1f, 0, 0, 1f)
                                enemy.copy(lastAttackTime = now) // pastikan Enemy adalah data class
                            } else {
                                enemy
                            }
                        } else {
                            enemy
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
// === HEAL SPAWN & PICKUP (di dalam loop utama) ===
                    if (isGamePlaying) {
                        // Spawn heal baru tiap 30 detik TANPA menghapus yang lama
                        if (now - lastHealSpawnTime >= 30_000) {
                            val newHealPos = Offset(
                                Random.nextFloat() * (screenWidthPx - healRadius * 2) + healRadius,
                                Random.nextFloat() * (screenHeightPx - healRadius * 2) + healRadius
                            )
                            healItems = healItems + newHealPos
                            lastHealSpawnTime = now
                        }

                        // Cek collision player dengan item heal
                        // Corrected line with explicit type declaration
                        val healHitbox = healRadius * 1.2f
                        healItems = healItems.filter { healPos: Offset ->
                            val collided = playerPos.getDistanceTo(healPos) <= (playerHitbox + healHitbox)
                            if (collided) {
                                eggHP = (eggHP + 1).coerceAtMost(10)
                                soundPool.play(soundHeal, 1f, 1f, 0, 0, 1f)
                            }
                            !collided
                        }
                    }


// Render heal items







                    enemies = newEnemies

                    if (eggHP <= 0) {
                        soundPool.play(soundGameOver, 1f, 1f, 0, 0, 1f) // mainkan suara game over dulu

                        onGameOver(
                            GameStateData(
                                eggHP,
                                score,
                                playerPos,
                                enemies,
                                joystickOffset,
                                joystickCenter,
                                screenWidthPx,
                                screenHeightPx,
                                healItems = healItems,                 // 🔹 simpan posisi heal
                                lastHealSpawnTime = lastHealSpawnTime
                            )
                        )
                    }

                }
            }
            lastFrameTime = currentTime
            delay(8)
        }
    }


    Box(modifier = Modifier.fillMaxSize()) {
        GameBackground()

        Canvas(
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
                        onDragEnd = { joystickOffset = Offset.Zero }
                    )
                }
                .onGloballyPositioned {
                    val w = it.size.width.toFloat()
                    val h = it.size.height.toFloat()

                    screenWidthPx = w
                    screenHeightPx = h

                    if (playerPos == Offset.Zero) {
                        playerPos = Offset(w / 2, h - 100f)
                    }
                    if (joystickCenter == Offset.Zero) {
                        joystickCenter = Offset(180f, h - 180f)
                    }
                }
        ) {
            // Gambar Telur
            drawImage(
                image = eggImage,
                dstSize = IntSize((eggRadius * 2).toInt(), (eggRadius * 2).toInt()),
                dstOffset = IntOffset(
                    (screenWidthPx / 2 - eggRadius).toInt(),
                    (screenHeightPx / 2 - eggRadius).toInt()
                )
            )

            // ... sisanya tetap sama
// Gambar item heal kalau ada
            healItems.forEach { healPos: Offset ->
                // Calculate the source offset of the current frame on the sprite sheet
                val srcX = (healFrameIndex % (healSprite.width / healFrameWidth)) * healFrameWidth
                val srcY = (healFrameIndex / (healSprite.width / healFrameWidth)) * healFrameHeight

                // Draw the specific frame from the sprite sheet
                drawImage(
                    image = healSprite,
                    srcOffset = IntOffset(srcX, srcY),
                    srcSize = IntSize(healFrameWidth, healFrameHeight),
                    dstOffset = IntOffset(
                        (healPos.x - healRadius).toInt(),
                        (healPos.y - healRadius).toInt()
                    ),
                    dstSize = IntSize((healRadius * 2).toInt(), (healRadius * 2).toInt())
                )
            }

            // Player Frame
            // 🔹 Ambil frame dari spritesheet
            val frameWidth = 32
            val frameHeight = 32

// Tentukan animasi berdasarkan gerakan
            val isMoving = joystickOffset.getDistance() > 0.5f
            val spriteSheet = if (isMoving) playerMove else playerIdle

// Jumlah frame tergantung sprite yang dipakai
            // Tentukan frame index sesuai animasi
            val frameIndex = if (isMoving) {
                ((System.currentTimeMillis() / 80) % moveFrameCount).toInt() // animasi jalan
            } else {
                ((System.currentTimeMillis() / 300) % idleFrameCount).toInt() // animasi diam
            }

// Ambil frame dari sprite sheet
            val playerFrame = if (isMoving) {
                extractFrame(moveSpriteSheet, frameIndex, frameWidth, frameHeight)
            } else {
                extractFrame(idleSpriteSheet, frameIndex, frameWidth, frameHeight)
            }

// Flip kalau hadap kiri
            val finalFrame = if (!playerFacingRight) {
                val flipped = ImageBitmap(playerFrame.width, playerFrame.height)
                val flipCanvas = androidx.compose.ui.graphics.Canvas(flipped)
                flipCanvas.scale(-1f, 1f)
                flipCanvas.translate(-playerFrame.width.toFloat(), 0f) // supaya pivotnya benar
                flipCanvas.drawImage(playerFrame, Offset.Zero, Paint())
                flipped
            } else {
                playerFrame
            }

// Gambar di posisi player
            drawImage(
                image = finalFrame,
                dstSize = IntSize(playerSize.toInt(), playerSize.toInt()),
                dstOffset = IntOffset(
                    (playerPos.x - playerSize / 2).toInt(),
                    (playerPos.y - playerSize / 2).toInt()
                )
            )

            // Musuh
            enemies.forEach { enemy ->
                val isMoving = enemy.velocity.getDistance() > 0.1f
                val spriteSheet = if (isMoving) enemyMoveSprite else enemyStaySprite
                val frameCount = if (isMoving) enemyMoveFrames else enemyStayFrames

                val frameIndex = enemyFrameIndex % frameCount
                val frame = extractFrame(spriteSheet, frameIndex, enemyFrameWidth, enemyFrameHeight)

                // Tentukan arah hadap (default kanan)
                val facingRight = enemy.velocity.x >= 0
                val finalFrame = if (!facingRight) {
                    val flipped = ImageBitmap(frame.width, frame.height)
                    val flipCanvas = androidx.compose.ui.graphics.Canvas(flipped)
                    flipCanvas.scale(-1f, 1f)
                    flipCanvas.translate(-frame.width.toFloat(), 0f)
                    flipCanvas.drawImage(frame, Offset.Zero, Paint())
                    flipped
                } else {
                    frame
                }

                drawImage(
                    image = finalFrame,
                    dstSize = IntSize((enemyRadius * 2).toInt(), (enemyRadius * 2).toInt()),
                    dstOffset = IntOffset(
                        (enemy.position.x - enemyRadius).toInt(),
                        (enemy.position.y - enemyRadius).toInt()
                    )
                )
            }


            // Joystick
            drawCircle(
                color = Color.White.copy(alpha = 0.4f),
                radius = joystickRadius * 0.8f,
                center = joystickCenter
            )
            drawCircle(
                color = Color.DarkGray.copy(alpha = 0.9f),
                radius = knobRadius,
                center = joystickCenter + joystickOffset
            )
        }

        // UI Atas
        Column(Modifier.padding(16.dp)) {
            Text("Base HP: $eggHP", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(
                "Score: $score",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Tombol Pause (harus di dalam BoxScope → ini oke)
        Button(
            onClick = {
                isPaused = true
                soundPool.play(soundButtonClick, 1f, 1f, 0, 0, 1f)

                onPause(
                    GameStateData(
                        eggHP,
                        score,
                        playerPos,
                        enemies,
                        joystickOffset,
                        joystickCenter,
                        screenWidthPx,
                        screenHeightPx,
                        healItems = healItems,
                        lastHealSpawnTime = lastHealSpawnTime
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

    // UI atas
        Column(Modifier.padding(16.dp)) {
            Text("Base HP: $eggHP", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Score: $score", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp))
        }

        // Tombol Pause

}

@Composable
fun MainMenuScreen(onStartGame: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        GameBackground() // ✅ taruh dulu background-nya

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "ONE DEFENDER",
                color = Color.Yellow,
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 40.dp)
            )
            Button(
                onClick = {
                    soundPool.play(soundButtonClick, 1f, 1f, 0, 0, 1f)
                    onStartGame()
                },
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
    Box(Modifier.fillMaxSize()) {
        // Background game
        GameBackground()

        // Overlay hitam transparan
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xAA000000))
        )

        // Konten menu pause
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("PAUSED", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(
                "Score: $currentScore",
                fontSize = 24.sp,
                color = Color.White,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    soundPool.play(soundButtonClick, 1f, 1f, 0, 0, 1f)
                    onResume()},
                modifier = Modifier.width(200.dp).height(50.dp),

                ) {
                Text("RESUME", fontSize = 18.sp)
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { soundPool.play(soundButtonClick, 1f, 1f, 0, 0, 1f)
                    onRestart()},
                modifier = Modifier.width(200.dp).height(50.dp),

                ) {
                Text("RESTART", fontSize = 18.sp)
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { soundPool.play(soundButtonClick, 1f, 1f, 0, 0, 1f)
                    onQuit()},
                modifier = Modifier.width(200.dp).height(50.dp),

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
    Box(Modifier.fillMaxSize()) {
        // Background game
        GameBackground()

        // Overlay hitam transparan
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xAA000000))
        )

        // Konten game over
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("GAME OVER", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color.Red)
            Text(
                "Score: $score",
                fontSize = 28.sp,
                color = Color.White,
                modifier = Modifier.padding(top = 16.dp)
            )

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { soundPool.play(soundButtonClick, 1f, 1f, 0, 0, 1f)
                    onRestart()},
                modifier = Modifier.width(200.dp).height(50.dp),

                ) {
                Text("PLAY AGAIN", fontSize = 18.sp)
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { soundPool.play(soundButtonClick, 1f, 1f, 0, 0, 1f)
                    onQuit()},
                modifier = Modifier.width(200.dp).height(50.dp),

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

