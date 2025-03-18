@file:Suppress("DEPRECATION")

package com.example.acelerometroparcial1

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.hardware.camera2.CameraManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

class MainActivity : ComponentActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var mediaPlayer: MediaPlayer? = null
    private var cameraManager: CameraManager? = null
    private var vibrator: Vibrator? = null
    private var cameraId: String? = null
    private var isFlashOn by mutableStateOf(false)
    private var isVibrating = false
    private val vibrationHandler = Handler(Looper.getMainLooper())

    private val imageSoundMap = listOf(
        Pair(R.drawable.nox, R.raw.lumos_sound_effect), // Nox/Lumos (imagen por defecto)
        Pair(R.drawable.leviosa, R.raw.leviosa),        // Leviosa
        Pair(R.drawable.expeliarmus, R.raw.expelliarmus_wand) // Expelliarmus
    )

    private var currentIndex by mutableIntStateOf(0) // Nox/Lumos por defecto
    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f
    private var lastTime: Long = 0

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        cameraId = cameraManager?.cameraIdList?.get(0)

        setContent {
            val imageRes = when {
                currentIndex == 0 && isFlashOn -> R.drawable.lumus
                else -> imageSoundMap[currentIndex].first
            }
            ImageScreen(
                imageRes = imageRes,
                onImageClick = { changeImage() }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
        stopVibration()
        turnOffFlash()
        mediaPlayer?.release()
        mediaPlayer = null // Liberar el recurso de mediaPlayer
        clearSensorData() // Borrar información de los sensores
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                val x = it.values[0]
                val y = it.values[1]
                val z = it.values[2]

                val deltaX = abs(x - lastX)
                val deltaY = abs(y - lastY)
                val deltaZ = abs(z - lastZ)

                val currentTime = System.currentTimeMillis()
                if ((deltaX > 10 || deltaY > 10 || deltaZ > 10) && (currentTime - lastTime > 1000)) {
                    activateAction()
                    lastTime = currentTime
                }

                lastX = x
                lastY = y
                lastZ = z

                if (currentIndex == 1 && y > 5) { // Detectar si el teléfono está a un nivel elevado
                    if (!isVibrating) {
                        isVibrating = true
                        vibrateWhileElevated()
                    }
                } else {
                    stopVibration()
                }
            }
        }
    }

    private fun activateAction() {
        when (currentIndex) {
            0 -> toggleFlashAndImage() // Nox/Lumos
            1 -> playCurrentSound() // Leviosa
            2 -> playCurrentSound() // Expelliarmus
        }
    }

    private fun changeImage() {
        if (isFlashOn) {
            return // Bloquear cambio si la linterna está encendida
        }
        currentIndex = (currentIndex + 1) % imageSoundMap.size
    }

    private fun toggleFlashAndImage() {
        if (isFlashOn) {
            turnOffFlash()
        } else {
            turnOnFlash()
        }
        playCurrentSound()
    }

    private fun turnOnFlash() {
        cameraId?.let {
            cameraManager?.setTorchMode(it, true)
            isFlashOn = true
        }
    }

    private fun turnOffFlash() {
        cameraId?.let {
            cameraManager?.setTorchMode(it, false)
            isFlashOn = false
        }
    }

    private fun playCurrentSound() {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer.create(this, imageSoundMap[currentIndex].second)
        mediaPlayer?.start()
    }

    private fun vibrateWhileElevated() {
        if (isVibrating) {
            vibrator?.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
            vibrationHandler.postDelayed({ vibrateWhileElevated() }, 500)
        }
    }

    private fun stopVibration() {
        isVibrating = false
        vibrationHandler.removeCallbacksAndMessages(null)
        vibrator?.cancel()
    }

    private fun clearSensorData() {
        lastX = 0f
        lastY = 0f
        lastZ = 0f
        lastTime = 0
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        stopVibration()
        turnOffFlash()
        clearSensorData() // Borrar información de los sensores
    }
}

@Composable
fun ImageScreen(imageRes: Int, onImageClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onImageClick() }
    ) {
        // Imagen de fondo
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = "Imagen de hechizo",
            modifier = Modifier.fillMaxSize(),
        )

        // Texto superpuesto
        Text(
            text = "Toca la imagen para cambiar\n" +
                    "Agita el celular para activar:\n" +
                    "• Expelliarmus: Sonido\n" +
                    "• Leviosa: Sonido y vibración mientras está elevado\n" +
                    "• Lumos: Alternar linterna",
            modifier = Modifier
                .align(Alignment.BottomCenter) // Alinea el texto en la parte inferior
                .padding(16.dp), // Añade un poco de espacio alrededor del texto
            color = MaterialTheme.colorScheme.onSurface, // Color del texto para que sea visible
            fontSize = 18.sp, // Tamaño del texto
            fontWeight = FontWeight.Bold // Texto en negrita
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewImageScreen() {
    ImageScreen(imageRes = R.drawable.nox) {}
}