package com.example.acelerometroparcial1

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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    private var isFlashOn by mutableStateOf(false) // Estado de la linterna
    private var isVibrating = false
    private val vibrationHandler = Handler(Looper.getMainLooper())

    private val imageSoundMap = listOf(
        Pair(R.drawable.nox, R.raw.lumos_sound_effect),  // Nox
        Pair(R.drawable.leviosa, R.raw.leviosa),         // Leviosa
        Pair(R.drawable.expeliarmus, R.raw.expelliarmus_wand), // Expelliarmus
        Pair(R.drawable.lumus, R.raw.lumos_sound_effect) // Lumos
    )

    private var currentIndex by mutableIntStateOf(0) // Nox por defecto
    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f
    private var lastTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        cameraId = cameraManager?.cameraIdList?.get(0)

        setContent {
            MainScreen(
                imageRes = imageSoundMap[currentIndex].first,
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
        mediaPlayer = null
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
            0, 3 -> toggleNoxLumos() // Alternar entre Nox y Lumos
            1 -> {
                playCurrentSound()
                vibrateWhileElevated()
            }
            2 -> playCurrentSound() // Expelliarmus
        }
    }

    private fun toggleNoxLumos() {
        if (isFlashOn) {
            turnOffFlash()
            currentIndex = 0 // Cambiar a Nox
        } else {
            turnOnFlash()
            currentIndex = 3 // Cambiar a Lumos
        }
        playCurrentSound()
        updateImage()
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
        mediaPlayer?.setOnCompletionListener {
            mediaPlayer?.release()
            mediaPlayer = null
        }
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

    private fun updateImage() {
        setContent {
            MainScreen(
                imageRes = imageSoundMap[currentIndex].first,
                onImageClick = { changeImage() }
            )
        }
    }

    private fun changeImage() {
        if (!isFlashOn && (mediaPlayer == null || !mediaPlayer!!.isPlaying)) { // Permitir cambiar de hechizo solo si la linterna está apagada y no se está reproduciendo media
            currentIndex = (currentIndex + 1) % 3 // Solo permitir cambiar entre Expelliarmus, Leviosa y Nox
            updateImage()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        stopVibration()
        turnOffFlash()
        mediaPlayer = null
    }
}

@Composable
fun MainScreen(imageRes: Int, onImageClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { onImageClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = "Imagen de hechizo",
                modifier = Modifier
                    .size(300.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Agita el celular para activar:\n" +
                        "• Nox/Lumos: Alternar linterna\n" +
                        "• Leviosa: Sonido y vibración\n" +
                        "• Expelliarmus: Sonido",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewMainScreen() {
    MainScreen(
        imageRes = R.drawable.nox,
        onImageClick = {}
    )
}