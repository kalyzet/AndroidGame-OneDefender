package com.example.jagabakso

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.math.sqrt




class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var playerX by remember { mutableStateOf(300f) }
            var playerY by remember { mutableStateOf(500f) }

            // Tambahan: variabel arah gerak
            var direction by remember { mutableStateOf(Offset.Zero) }

            // Loop gerakan terus-menerus
            LaunchedEffect(direction) {
                while (true) {
                    playerX += direction.x * 5
                    playerY += direction.y * 5
                    delay(16) // sekitar 60 FPS
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                // Karakter utama
                Box(
                    modifier = Modifier
                        .offset { IntOffset(playerX.toInt(), playerY.toInt()) }
                        .size(50.dp)
                        .background(Color.Blue)
                )

                // Joystick
                JoystickControl(
                    onMove = { dx, dy ->
                        direction = Offset(dx, dy)
                    },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                )
            }
        }

    }
}

@Composable
fun GameScreen() {
    var characterX by remember { mutableStateOf(0.dp) }

    // Posisi musuh
    var enemyX by remember { mutableStateOf(300.dp) } // Mulai dari kanan
    var enemyY by remember { mutableStateOf(0.dp) }

    // Update musuh terus setiap 50ms
    LaunchedEffect(Unit) {
        while (true) {
            enemyX -= 5.dp
            delay(50)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.DarkGray)
    ) {
        // Telur emas
        Box(
            modifier = Modifier
                .size(60.dp)
                .align(Alignment.Center)
                .background(Color.Yellow, shape = CircleShape)
        )

        // Karakter biru (pelindung)
        Box(
            modifier = Modifier
                .size(50.dp)
                .align(Alignment.BottomCenter)
                .offset(x = characterX, y = (-60).dp)
                .background(Color.Blue)
        )

        // Musuh merah
        Box(
            modifier = Modifier
                .size(40.dp)
                .align(Alignment.Center)
                .offset(x = enemyX, y = enemyY)
                .background(Color.Red)
        )

        // Tombol gerak
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = { characterX -= 20.dp }) {
                Text("◀", fontSize = 24.sp)
            }
            Button(onClick = { characterX += 20.dp }) {
                Text("▶", fontSize = 24.sp)
            }
        }
    }
}

@Composable
fun JoystickControl(
    onMove: (dx: Float, dy: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var knobPosition by remember { mutableStateOf(Offset.Zero) }

    val joystickSize = 150f
    val knobSize = 40f
    val radius = (joystickSize - knobSize) / 1

    Box(
        modifier = modifier
            .size(joystickSize.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val newOffset = (knobPosition + dragAmount).coerceInCircle(radius)
                        knobPosition = newOffset
                        onMove(newOffset.x / radius, newOffset.y / radius)
                    },
                    onDragEnd = {
                        knobPosition = Offset.Zero
                        onMove(0f, 0f)
                    }
                )
            }
            .background(Color.LightGray, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        knobPosition.x.roundToInt(),
                        knobPosition.y.roundToInt()
                    )
                }
                .size(knobSize.dp)
                .background(Color.DarkGray, CircleShape)
        )
    }
}

// Fungsi bantu supaya knob-nya nggak keluar dari lingkaran joystick
fun Offset.getDistance(): Float {
    return sqrt(x * x + y * y)
}

fun Offset.coerceInCircle(maxRadius: Float): Offset {
    val distance = getDistance()
    return if (distance <= maxRadius) this
    else this * (maxRadius / distance)
}

