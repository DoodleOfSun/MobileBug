package com.example.prototypebug

import android.app.Dialog
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.Surface
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.SeekBar
import kotlin.math.atan2

class TitleActivity : AudioActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private lateinit var playerSprite: View
    private var velX = 0f
    private var velY = 0f
    private var sensitivity = 10f
    private val damping = 0.9f

    private var isPaused = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        playerSprite = findViewById(R.id.square)

        val prefs = getSharedPreferences("game_settings", MODE_PRIVATE)
        sensitivity = prefs.getFloat("sensitivity", 10f)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // 화면 터치 → 맵 선택으로
        findViewById<View>(R.id.mainContainer).setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                playSound(GameSound.BUTTON_CLICK)
                startActivity(Intent(this, MapSelectActivity::class.java))
            }
            true
        }

        // 설정 버튼
        val settingsButton = findViewById<ImageButton>(R.id.btn_settings)
        settingsButton.setGameClickListener(::playSound) {
            isPaused = true
            val settingsXML = layoutInflater.inflate(R.layout.settings, null)
            val btnClose = settingsXML.findViewById<ImageButton>(R.id.btnSettingClose)
            val seekBar = settingsXML.findViewById<SeekBar>(R.id.sensitivityBar)

            val dialog = Dialog(this)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(settingsXML)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.window?.setLayout(dpToPx(550f), dpToPx(330f))
            // 카메라 구멍 영역까지 화면 전체 기준으로 가운데 정렬 (시작화면과 같은 기준)
            dialog.window?.let { window ->
                window.attributes = window.attributes.apply {
                    layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                }
            }

            seekBar.progress = sensitivity.toInt()
            setupVolumeBars(settingsXML)

            btnClose.setGameClickListener(::playSound) {
                isPaused = false
                sensitivity = seekBar.progress.toFloat()
                prefs.edit().putFloat("sensitivity", sensitivity).apply()
                Log.d("GameDebug", "민감도 저장: $sensitivity")
                dialog.dismiss()
            }

            dialog.setOnDismissListener {
                isPaused = false
            }

            dialog.show()
        }
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (isPaused) return
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        @Suppress("DEPRECATION")
        val rotation = windowManager.defaultDisplay.rotation
        val rawX = event.values[0]
        val rawY = event.values[1]

        val (mappedX, mappedY) = when (rotation) {
            Surface.ROTATION_90 -> rawY to -rawX
            Surface.ROTATION_270 -> -rawY to rawX
            else -> rawX to rawY
        }

        velX += mappedX * sensitivity * 0.05f
        velY += -mappedY * sensitivity * 0.05f
        velX *= damping
        velY *= damping

        playerSprite.x += velX
        playerSprite.y += velY

        val container = findViewById<View>(R.id.mainContainer)
        val maxX = (container.width - playerSprite.width).coerceAtLeast(0).toFloat()
        val maxY = (container.height - playerSprite.height).coerceAtLeast(0).toFloat()

        playerSprite.x = playerSprite.x.coerceIn(0f, maxX)
        playerSprite.y = playerSprite.y.coerceIn(0f, maxY)

        val speedSq = velX * velX + velY * velY
        if (speedSq > 0.01f) {
            playerSprite.rotation = Math.toDegrees(atan2(velY.toDouble(), velX.toDouble())).toFloat()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun dpToPx(dp: Float): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
