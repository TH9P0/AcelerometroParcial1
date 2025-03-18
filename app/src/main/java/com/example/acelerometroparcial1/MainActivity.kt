package com.example.acelerometroparcial1

import android.content.Context
import android.content.pm.ActivityInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import android.hardware.camera2.CameraManager

class MainActivity : ComponentActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var mediaPlayer: MediaPlayer? = null
    private var cameraManager: CameraManager? = null
    private var vibrator: Vibrator? = null
    private var cameraId: String? = null
    private var isFlashOn by mutableStateOf(false) // Estado para controlar la linterna

    // Lista de imágenes y sonidos asociados
    private val imageSoundMap: List<Pair<Int, Int>> = listOf(
        Pair(R.drawable.expeliarmus, R.raw.expelliarmus_wand), // Expelliarmus
        Pair(R.drawable.leviosa, R.raw.leviosa),               // Leviosa
        Pair(R.drawable.nox, R.raw.lumos_sound_effect),                 // Nox (imagen por defecto)
        Pair(R.drawable.lumus, R.raw.lumos_sound_effect)       // Lumos (linterna encendida)
    )

    private var currentIndex by mutableIntStateOf(2) // Índice actual de la imagen/sonido (inicia en Nox)
    private var isVibrating = false // Estado para controlar la vibración

    // Variables para detectar el movimiento
    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f
    private var lastTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Bloquear la rotación de pantalla
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        cameraId = cameraManager?.cameraIdList?.get(0)

        setContent {
            ImageScreen(
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
        turnOffFlash() // Asegurarse de apagar la linterna si la app se pausa
        stopVibration() // Asegurarse de detener la vibración si la app se pausa
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
                    activateAction() // Activar acción según el hechizo actual
                    lastTime = currentTime
                }

                lastX = x
                lastY = y
                lastZ = z
            } else {
                stopVibration() // Detener la vibración cuando el dispositivo no se está moviendo
            }
        }
    }

    private fun activateAction() {
        when (currentIndex) {
            0 -> playCurrentSound() // Expelliarmus: solo reproduce sonido
            1 -> { // Leviosa: reproduce sonido y vibra
                playCurrentSound()
                isVibrating = true
                vibratePhone()
            }
            2 -> toggleFlashAndImage() // Nox/Lumos: alterna entre linterna encendida/apagada
            3 -> toggleFlashAndImage() // Nox/Lumos: alterna entre linterna encendida/apagada
        }
    }

    private fun toggleFlashAndImage() {
        if (isFlashOn) {
            // Apagar linterna y cambiar a Nox
            turnOffFlash()
            currentIndex = 2 // Nox
        } else {
            // Encender linterna y cambiar a Lumos
            turnOnFlash()
            currentIndex = 3 // Lumos
        }
        playCurrentSound() // Reproducir sonido correspondiente
    }

    private fun changeImage() {
        // Cambiar al siguiente índice en la lista
        currentIndex = (currentIndex + 1) % imageSoundMap.size
        if (currentIndex != 3) {
            turnOffFlash() // Apagar la linterna si no es Lumos
        }
    }

    private fun playCurrentSound() {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer.create(this, imageSoundMap[currentIndex].second)
        mediaPlayer?.start()
    }

    private fun vibratePhone() {
        if (isVibrating) {
            vibrator?.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    private fun stopVibration() {
        isVibrating = false
        vibrator?.cancel()
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

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        turnOffFlash() // Apagar linterna al cerrar la app
        stopVibration() // Detener vibración al cerrar la app
    }
}

@Composable
fun ImageScreen(imageRes: Int, onImageClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = "Imagen de hechizo",
            modifier = Modifier.size(250.dp).clickable { onImageClick() }
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Toca la imagen para cambiar\n" +
                    "Agita el celular para activar:\n" +
                    "• Expelliarmus: Sonido\n" +
                    "• Leviosa: Sonido y vibración\n" +
                    "• Nox/Lumos: Alternar linterna"
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewImageScreen() {
    ImageScreen(imageRes = R.drawable.nox) {}
}