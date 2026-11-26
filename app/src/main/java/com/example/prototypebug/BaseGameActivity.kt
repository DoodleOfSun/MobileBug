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
import android.view.Choreographer
import android.view.Surface
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.SeekBar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlin.math.atan2

abstract class BaseGameActivity : AudioActivity(), SensorEventListener {

    override val battleMusicResource: Int = R.raw.battle_bgm

    protected lateinit var sensorManager: SensorManager
    protected var accelerometer: Sensor? = null
    protected lateinit var container: ConstraintLayout
    protected lateinit var playerInfo: PlayerInfo
    lateinit var weaponManager: WeaponManager

    protected var velX = 0f
    protected var velY = 0f
    protected val squareSizePx by lazy { dpToPx(80f) }
    protected var sensitivity = 10f
    protected val damping = 0.9f

    protected lateinit var playerHPBar: ProgressBar
    protected lateinit var playerNormalAttack: PlayerNormalAttack

    protected var isBlockMurdererStarted = false
    protected lateinit var enemySpawner: EnemySpawnAndManagement
    protected lateinit var rewardJewels: RewardManagement

    protected var lastFrameTimeNanos = 0L
    protected var deltaTime = 0f
    protected var secTime = 0f
    protected var monsterSpawnTimeDelay = 3f
    protected var isPaused = false

    protected var gameTime = 0f
    protected var waveCount = 0
    protected val winTime = 300f

    protected lateinit var gameLoop: Choreographer.FrameCallback

    protected var score = 0f
    protected var enemyKillCount = 0
    protected var bossKillCount = 0

    // 맵마다 레이아웃만 다르게
    abstract fun getLayoutId(): Int

    protected open fun createEnemySpawner() = EnemySpawnAndManagement()

    protected open fun createRewardManagement() = RewardManagement()

    protected open fun onNormalAttackKill(enemy: Enemy) = Unit

    protected open fun constrainPlayerMovement(previousX: Float, previousY: Float) = Unit

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(getLayoutId())

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        playerHPBar = findViewById(R.id.progressBar_playerHP)
        playerHPBar.visibility = View.INVISIBLE
        playerHPBar.progress = 100

        playerNormalAttack = PlayerNormalAttack()
        playerNormalAttack.sprite = findViewById(R.id.imageView_playerNormalAttack)
        playerNormalAttack.sprite.visibility = View.INVISIBLE

        val prefs = getSharedPreferences("game_settings", MODE_PRIVATE)
        sensitivity = prefs.getFloat("sensitivity", 10f)

        container = findViewById(R.id.mainContainer)

        playerInfo = PlayerInfo(::playSound)
        playerInfo.playerSprite = findViewById(R.id.square)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        weaponManager = WeaponManager(container, this)

        initEntireSystem()

        container.post {
            playerInfo.x = (container.width - squareSizePx) / 2f
            playerInfo.y = (container.height - squareSizePx) / 2f
            playerInfo.playerSprite.x = playerInfo.x
            playerInfo.playerSprite.y = playerInfo.y
        }

        setupSettingsButton()
        hideSystemBars()
    }

    private fun setupSettingsButton() {
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
            // 카메라 구멍 영역까지 화면 전체 기준으로 가운데 정렬 (게임 화면과 같은 기준)
            dialog.window?.let { window ->
                window.attributes = window.attributes.apply {
                    layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                }
            }

            seekBar.progress = sensitivity.toInt()

            btnClose.setGameClickListener(::playSound) {
                sensitivity = seekBar.progress.toFloat()
                getSharedPreferences("game_settings", MODE_PRIVATE)
                    .edit().putFloat("sensitivity", sensitivity).apply()
                isPaused = false
                dialog.dismiss()
            }

            dialog.setOnDismissListener {
                isPaused = false
            }

            dialog.show()
        }
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (isPaused || !isBlockMurdererStarted) return
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

        val previousX = playerInfo.x
        val previousY = playerInfo.y
        playerInfo.x += velX
        playerInfo.y += velY

        val maxX = (container.width - playerInfo.playerSprite.width).coerceAtLeast(0).toFloat()
        val maxY = (container.height - playerInfo.playerSprite.height).coerceAtLeast(0).toFloat()
        playerInfo.x = playerInfo.x.coerceIn(0f, maxX)
        playerInfo.y = playerInfo.y.coerceIn(0f, maxY)
        constrainPlayerMovement(previousX, previousY)

        playerInfo.playerSprite.x = playerInfo.x
        playerInfo.playerSprite.y = playerInfo.y

        val speedSq = velX * velX + velY * velY
        if (speedSq > 0.01f) {
            playerInfo.playerSprite.rotation = Math.toDegrees(atan2(velY.toDouble(), velX.toDouble())).toFloat()
        }
        updateHpBarPosition()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    protected open fun initEntireSystem() {
        isBlockMurdererStarted = true
        playerHPBar.visibility = View.VISIBLE
        playerNormalAttack.sprite.visibility = View.VISIBLE

        enemySpawner = createEnemySpawner()
        enemySpawner.container = findViewById(R.id.mainContainer)
        enemySpawner.context = this

        rewardJewels = createRewardManagement()
        rewardJewels.container = findViewById(R.id.mainContainer)
        rewardJewels.context = this

        gameUpdate()
    }

    protected fun gameUpdate() {
        gameLoop = Choreographer.FrameCallback { frameTimeNanos ->
            if (lastFrameTimeNanos == 0L) {
                lastFrameTimeNanos = frameTimeNanos
            } else {
                deltaTime = (frameTimeNanos - lastFrameTimeNanos) / 1_000_000_000f
                lastFrameTimeNanos = frameTimeNanos
            }

            if (!isBlockMurdererStarted || isPaused) {
                secTime = 0f
            } else {
                secTime += deltaTime
                gameTime += deltaTime

                if (secTime >= monsterSpawnTimeDelay) {
                    waveCount++
                    if (this is Map2Activity) {
                        repeat(9) { enemySpawner.spawnRangedFromEdge() }               // Map2: 원거리
                        if (waveCount % 10 == 0) enemySpawner.spawnRangedBoss()        // Map2: 원거리 보스
                    } else {
                        repeat(9) { enemySpawner.spawnFromEdge() }                      // Map1: 일반
                        if (waveCount % 10 == 0) enemySpawner.spawnBoss()              // Map1: 근접 보스
                    }
                    secTime -= monsterSpawnTimeDelay
                }

                enemySpawner.updateAll(playerInfo, deltaTime)
                weaponManager.updateAll(deltaTime, playerInfo, enemySpawner.enemies)
                rewardJewels.update(playerInfo, deltaTime)
                updateHPBarValue()

                if (gameTime >= winTime) winGame()
                if (playerInfo.checkPlayerDied()) {
                    isBlockMurdererStarted = false
                    endGame()
                }

                // 노말 어택 업데이트 + 데미지
                playerNormalAttack.update(
                    playerInfo.playerSprite.x,
                    playerInfo.playerSprite.y,
                    playerInfo.playerSprite.width.toFloat(),
                    deltaTime
                )

                val toRemove = mutableListOf<Enemy>()

                enemySpawner.enemies.forEach { enemy ->
                    if (playerNormalAttack.checkCollision(enemy.getCenterX(), enemy.getCenterY(), enemy.radius)) {
                        enemy.takeDamage(playerNormalAttack.attackPower)
                        if (enemy.health <= 0f) {
                            playSound(GameSound.ENEMY_DEFEATED)
                            rewardJewels.spawn(enemy.getCenterX(), enemy.getCenterY())
                            onNormalAttackKill(enemy)
                            toRemove.add(enemy)
                        }
                    }
                }

                // 모든 무기로 죽은 적 제거 (노말어택 + 새 무기 전부)
                val deadEnemies = mutableListOf<Enemy>()

                enemySpawner.enemies.forEach { enemy ->
                    if (enemy.health <= 0f) {
                        rewardJewels.spawn(enemy.getCenterX(), enemy.getCenterY())
                        deadEnemies.add(enemy)
                    }
                }

                deadEnemies.forEach { enemy ->
                    enemySpawner.removeEnemy(enemy)
                }
            }

            Choreographer.getInstance().postFrameCallback(gameLoop)
        }
        Choreographer.getInstance().postFrameCallback(gameLoop)
    }

    protected fun endGame() {
        score = gameTime * 10 + enemyKillCount * 100 + bossKillCount * 3500
        goToResult(false)
    }

    protected fun winGame() {
        isBlockMurdererStarted = false
        score = winTime * 30 + enemyKillCount * 100 + bossKillCount * 3500
        goToResult(true)
    }

    private fun goToResult(isWin: Boolean) {
        val intent = Intent(this, ResultActivity::class.java)
        intent.putExtra("isWin", isWin)
        intent.putExtra("score", score)
        intent.putExtra("mapClass", this::class.java.name)
        startActivity(intent)
        finish()
    }

    protected fun updateHpBarPosition() {
        if (!isBlockMurdererStarted) return
        val sprite = playerInfo.playerSprite
        playerHPBar.x = sprite.x + sprite.width / 2f - playerHPBar.width / 2f
        playerHPBar.y = sprite.y + sprite.height / 2f - dpToPx(80f)
    }

    protected fun updateHPBarValue() {
        if (!isBlockMurdererStarted) return

        val current = playerInfo.health.toFloat()
        val max = playerInfo.maxHealth.toFloat()
        val percent = ((current / max) * 100f).toInt().coerceIn(0, 100)

        playerHPBar.progress = percent
    }
    fun setPause(data: Boolean) { isPaused = data }

    fun dpToPx(dp: Float): Int = (dp * resources.displayMetrics.density).toInt()
}
