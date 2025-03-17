package com.example.acelerometroparcial1

import android.content.Context
import android.hardware.Camera
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.os.Bundle
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

class MainActivity : ComponentActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var mediaPlayer: MediaPlayer? = null
    private var camera: Camera? = null
    private var isFlashOn = false

    // Lista de imágenes y sonidos asociados
    private val imageSoundMap: List<Pair<Int, Int>> = listOf(
        Pair(R.drawable.harry, R.raw.expelliarmus),
        Pair(R.drawable.hermione, R.raw.leviosa),
        Pair(R.drawable.ron, R.raw.lumos) // Ron usa Lumos
    )

    private var currentIndex by mutableStateOf(0)

    // Variables para detectar el movimiento
    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f
    private var lastTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

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
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                val x = it.values[0]
                val y = it.values[1]
                val z = it.values[2]

                val deltaX = Math.abs(x - lastX)
                val deltaY = Math.abs(y - lastY)
                val deltaZ = Math.abs(z - lastZ)

                val currentTime = System.currentTimeMillis()
                if ((deltaX > 10 || deltaY > 10 || deltaZ > 10) && (currentTime - lastTime > 1000)) {
                    playCurrentSound()
                    if (currentIndex == 2) { // Si la imagen actual es Ron (Lumos)
                        toggleFlashlight()
                    }
                    lastTime = currentTime
                }

                lastX = x
                lastY = y
                lastZ = z
            }
        }
    }

    private fun changeImage() {
        // Cambiar al siguiente índice en la lista, sin reproducir sonido
        currentIndex = (currentIndex + 1) % imageSoundMap.size
        if (currentIndex != 2) {
            turnOffFlash() // Apagar la linterna si se cambia a otro personaje
        }
    }

    private fun playCurrentSound() {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer.create(this, imageSoundMap[currentIndex].second)
        mediaPlayer?.start()
    }

    private fun toggleFlashlight() {
        if (isFlashOn) {
            turnOffFlash()
        } else {
            turnOnFlash()
        }
    }

    private fun turnOnFlash() {
        try {
            camera = Camera.open()
            val params = camera?.parameters
            params?.flashMode = Camera.Parameters.FLASH_MODE_TORCH
            camera?.parameters = params
            camera?.startPreview()
            isFlashOn = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun turnOffFlash() {
        try {
            camera?.stopPreview()
            camera?.release()
            camera = null
            isFlashOn = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        turnOffFlash() // Apagar linterna al cerrar la app
    }
}

@Composable
fun ImageScreen(imageRes: Int, onImageClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = "Imagen de personaje",
            modifier = Modifier
                .size(250.dp)
                .clickable { onImageClick() } // Solo cambia la imagen, sin sonido ni linterna
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("Toca la imagen para cambiar\nAgita el celular para activar sonido\nSi es 'Lumos', prenderá la linterna")
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewImageScreen() {
    ImageScreen(imageRes = R.drawable.harry) {}
}
