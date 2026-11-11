package com.example.prototypebug

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.Choreographer
import android.view.MotionEvent
import android.view.Surface
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.transition.Visibility
import kotlin.math.atan2


// TODO : 5분 타이머, 보스가 주기적으로 나왔으면 좋겠다. 보스의 이동속도는 매우 느리다.

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    private lateinit var container: ConstraintLayout

    // 플레이어
    private lateinit var playerInfo: PlayerInfo

    // 무기
    public lateinit var weaponManager : WeaponManager

    // 속도 (관성 느낌을 주기 위함)
    private var velX = 0f
    private var velY = 0f

    private val squareSizePx by lazy { dpToPx(80f) }
    private var sensitivity = 10f      // 기울기 민감도
    private val damping = 0.9f        // 감속 (관성 자연스럽게)

    private lateinit var playerHPBar : ProgressBar

    private lateinit var playerNormalAttack : PlayerNormalAttack

    private var isBlockMurdererStarted = false;

    // 적
    private lateinit var enemySpawner : EnemySpawnAndManagement
    private lateinit var enemy : Enemy

    // 경험치 보석
    private lateinit var rewardJewels : RewardManagement

    // 시간 흐름
    private var lastFrameTimeNanos = 0L
    private var deltaTime = 0f   // 초 단위
    private var secTime = 0f;
    private var monsterSpawnTimeDelay = 3f
    private var isPaused : Boolean = false

    private var gameTime = 0f
    private var waveCount = 0
    private val winTime = 300f          // 5분

    // 텍스트 요소 멤버변수
    lateinit var titleText : TextView
    lateinit var tapToStartText : TextView
    lateinit var diedText : TextView

    // 프레임당 호출 내장 콜백
    private lateinit var gameLoop: Choreographer.FrameCallback

    private var totalEnemySpawn : Int = 0

    // 점수
    private var score : Float = 0f
    private var enemyKillCount : Int = 0
    private var bossKillCount : Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        titleText = findViewById<TextView>(R.id.titleText)
        tapToStartText = findViewById<TextView>(R.id.tapToStartText)
        diedText = findViewById<TextView>(R.id.playerDeadText)
        diedText.visibility = View.INVISIBLE

        playerHPBar = findViewById<ProgressBar>(R.id.progressBar_playerHP)
        playerHPBar.visibility = View.INVISIBLE
        playerHPBar.progress = 100

        playerNormalAttack = PlayerNormalAttack()
        playerNormalAttack.sprite = findViewById<ImageView>(R.id.imageView_playerNormalAttack)
        playerNormalAttack.sprite.visibility = View.INVISIBLE

        val prefs = getSharedPreferences("game_settings", MODE_PRIVATE)
        sensitivity = prefs.getFloat("sensitivity", 10f)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        container = findViewById(R.id.mainContainer)

        // 플레이어 초기화
        playerInfo = PlayerInfo()
        playerInfo.playerSprite = findViewById(R.id.square)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // 무기 매니저 초기화
        weaponManager = WeaponManager()

        initEntireSystem()

        // 뷰가 그려진 후 초기 위치를 화면 중앙으로 설정
        container.post {
            playerInfo.x = (container.width - squareSizePx) / 2f
            playerInfo.y = (container.height - squareSizePx) / 2f
            playerInfo.playerSprite.x = playerInfo.x
            playerInfo.playerSprite.y = playerInfo.y
        }

        val settingsButton = findViewById<ImageButton>(R.id.btn_settings)

        settingsButton.setOnClickListener {
            isPaused = true
            val settingsXML = layoutInflater.inflate(R.layout.settings, null)
            val btnClose = settingsXML.findViewById<ImageButton>(R.id.btnSettingClose)
            val seekBar = settingsXML.findViewById<SeekBar>(R.id.sensitivityBar)

            val dialog = Dialog(this)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(settingsXML)

            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.window?.setLayout(
                dpToPx(550f),
                dpToPx(330f)
            )

            seekBar.progress = sensitivity.toInt()

            btnClose.setOnClickListener {
                sensitivity = seekBar.progress.toFloat()
                Log.d("GameDebug", "현재 민감도: $sensitivity")

                val prefs = getSharedPreferences("game_settings", MODE_PRIVATE)
                prefs.edit().putFloat("sensitivity", sensitivity).apply()

                isPaused = false
                dialog.dismiss()
            }

            dialog.show()

            // 작아졌다가 쑥 커지는 연출
            val content = dialog.window?.decorView ?: return@setOnClickListener
            content.scaleX = 0.3f
            content.scaleY = 0.3f
            content.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(250)
                .setInterpolator(android.view.animation.OvershootInterpolator())
                .start()
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

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
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        @Suppress("DEPRECATION")
        val rotation = windowManager.defaultDisplay.rotation

        val rawX = event.values[0]
        val rawY = event.values[1]

        val mappedX: Float
        val mappedY: Float
        when (rotation) {
            Surface.ROTATION_90 -> {
                mappedX = rawY
                mappedY = -rawX
            }
            Surface.ROTATION_270 -> {
                mappedX = -rawY
                mappedY = rawX
            }
            else -> {
                mappedX = rawX
                mappedY = rawY
            }
        }

        val tiltX = +mappedX
        val tiltY = -mappedY

        velX += tiltX * sensitivity * 0.05f
        velY += tiltY * sensitivity * 0.05f

        velX *= damping
        velY *= damping

        playerInfo.x += velX
        playerInfo.y += velY

        val maxX = (container.width - squareSizePx).coerceAtLeast(0)
        val maxY = (container.height - squareSizePx).coerceAtLeast(0)

        if (playerInfo.x < 0f) { playerInfo.x = 0f; velX = 0f }
        if (playerInfo.x > maxX) { playerInfo.x = maxX.toFloat(); velX = 0f }
        if (playerInfo.y < 0f) { playerInfo.y = 0f; velY = 0f }
        if (playerInfo.y > maxY) { playerInfo.y = maxY.toFloat(); velY = 0f }

        playerInfo.playerSprite.x = playerInfo.x
        playerInfo.playerSprite.y = playerInfo.y

        val speedSq = velX * velX + velY * velY
        if (speedSq > 0.01f) {
            val angleDeg = Math.toDegrees(atan2(velY.toDouble(), velX.toDouble())).toFloat()
            playerInfo.playerSprite.rotation = angleDeg
        }

        updateHpBarPosition()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun initEntireSystem(){
        val container = findViewById<View>(R.id.mainContainer)

        enemySpawner = EnemySpawnAndManagement()
        enemySpawner.container = findViewById<ViewGroup>(R.id.mainContainer)
        enemySpawner.context = this

        rewardJewels = RewardManagement()
        rewardJewels.container = findViewById<ViewGroup>(R.id.mainContainer)
        rewardJewels.context = this

        @SuppressLint("ClickableViewAccessibility")
        container.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {


                isBlockMurdererStarted = startGame(isBlockMurdererStarted)
            }
            true
        }



        gameUpdate()
    }

    private fun startGame(startBoolData: Boolean): Boolean {
        diedText.visibility = View.INVISIBLE

        if (!startBoolData) {
            gameLaunch()
        }

        // 리셋 끝난 뒤에 VISIBLE로 바꾸기
        playerHPBar.visibility = View.VISIBLE
        playerNormalAttack.sprite.visibility = View.VISIBLE   // 또는 playerNormalAttack.show()

        return true
    }

    private fun endGame(){
        diedText.visibility = View.VISIBLE
        playerHPBar.visibility = View.INVISIBLE
    }

    private fun gameLaunch() {
        titleText.visibility = View.INVISIBLE
        tapToStartText.visibility = View.INVISIBLE
        resetAllObjects()
    }

    private fun gameUpdate(){
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

                // 이 밑에 Update 필요한 구문을 작성


                // 기존 몬스터 스폰 로직 수정
                if (secTime >= monsterSpawnTimeDelay) {
                    Log.d("GameDebug", "3초가 지났어요")

                    waveCount++

                    repeat(9) {
                        enemySpawner.spawnFromEdge()
                        totalEnemySpawn++
                    }

                    // 10웨이브마다 보스 함께 스폰
                    if (waveCount % 10 == 0) {
                        enemySpawner.spawnBoss()
                    }

                    secTime -= monsterSpawnTimeDelay
                }

                val targetX = playerInfo.x
                val targetY = playerInfo.y
                enemySpawner.updateAll(playerInfo, deltaTime)

                // HP 바 업데이트
                updateHPBarValue()

                // 플레이어 승리 체크
                gameTime += deltaTime

                // 5분 경과 → 승리
                if (gameTime >= winTime) {
                    winGame()
                }


                // gameUpdate() 안, 적 업데이트 근처
                weaponManager.updateAll(deltaTime, playerInfo, enemySpawner.enemies)

                // 플레이어 사망 체크
                if(playerInfo.checkPlayerDied()){
                    isBlockMurdererStarted = false
                    endGame()
                }

                playerNormalAttack.update(
                    playerInfo.playerSprite.x,
                    playerInfo.playerSprite.y,
                    playerInfo.playerSprite.width.toFloat(),  // squareSizePx 대신 실제 크기
                    deltaTime
                )


                val toRemove = mutableListOf<Enemy>()

                enemySpawner.enemies.forEach { enemy ->
                    if (playerNormalAttack.checkCollision(enemy.getCenterX(), enemy.getCenterY(), enemy.radius)) {
                        enemy.takeDamage(playerNormalAttack.attackPower)

                        // 적 사망 체크
                        if (enemy.health <= 0f) {

                            rewardJewels.spawn(enemy.getCenterX(), enemy.getCenterY())
                            toRemove.add(enemy)

                        }
                    }
                }


                // 루프가 끝난 뒤에 한꺼번에 제거
                toRemove.forEach { enemy ->
                    enemySpawner.removeEnemy(enemy)
                }


                rewardJewels.update(playerInfo, deltaTime)

            }

            Choreographer.getInstance().postFrameCallback(gameLoop)
        }
        Choreographer.getInstance().postFrameCallback(gameLoop)
    }

    private fun returnToMain(){
        val titleText = findViewById<TextView>(R.id.titleText)
        val tapToStartText = findViewById<TextView>(R.id.tapToStartText)

        titleText.visibility = View.VISIBLE
        tapToStartText.visibility = View.VISIBLE

    }

    public fun dpToPx(dp: Float): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }

    private fun resetAllObjects(){
        playerInfo.playerReset()

        enemySpawner.resetAllEnemies()

        playerHPBar.progress = 100
        playerHPBar.visibility = View.INVISIBLE

        gameTime = 0f
        waveCount = 0

        score = 0f
        enemyKillCount = 0
        bossKillCount = 0

        container.post {
            playerInfo.x = (container.width - squareSizePx) / 2f
            playerInfo.y = (container.height - squareSizePx) / 2f
            playerInfo.playerSprite.x = playerInfo.x
            playerInfo.playerSprite.y = playerInfo.y
        }
    }

    private fun updateHpBarPosition() {
        if (!isBlockMurdererStarted) return

        val sprite = playerInfo.playerSprite
        val centerX = sprite.x + sprite.width / 2f
        val centerY = sprite.y + sprite.height / 2f

        playerHPBar.x = centerX - playerHPBar.width / 2f
        playerHPBar.y = centerY - dpToPx(80f)   // 머리 위 거리만 조절
    }

    private fun updateHPBarValue() {
        if (!isBlockMurdererStarted) return

        val hpPercent = (playerInfo.health.toFloat() / playerInfo.maxHealth * 100).toInt()
        playerHPBar.progress = hpPercent.coerceIn(0, 100)
    }

    public fun setPause(data : Boolean){
        isPaused = data
    }

    private fun winGame() {
        isBlockMurdererStarted = false

        // 점수 계산
        score = winTime * 30 + enemyKillCount * 100 + bossKillCount * 3500

        diedText.text = "승리!\n점수: $score"
        diedText.visibility = View.VISIBLE

        playerHPBar.visibility = View.INVISIBLE
        playerNormalAttack.sprite.visibility = View.INVISIBLE

        Log.d("GameDebug", "5분 생존 승리! 점수: $score")
    }

    // MainActivity 안에 (레벨업 발생 시 호출할 함수)
    private fun showLevelUpDialog() {
        isPaused = true

        val levelUpXML = layoutInflater.inflate(R.layout.level_up_settings, null)  // 레벨업 레이아웃 이름에 맞게
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(levelUpXML)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(false)

        // 3개 랜덤 뽑기
        val choices = weaponManager.getRandomChoices(3)

        // UI 요소 찾기 (레이아웃 ID에 맞게 수정)
        val text1 = levelUpXML.findViewById<TextView>(R.id.text_choose1)
        val btn1  = levelUpXML.findViewById<ImageButton>(R.id.btn_choose1)
        val text2 = levelUpXML.findViewById<TextView>(R.id.text_choose2)
        val btn2  = levelUpXML.findViewById<ImageButton>(R.id.btn_choose2)
        val text3 = levelUpXML.findViewById<TextView>(R.id.text_choose3)
        val btn3  = levelUpXML.findViewById<ImageButton>(R.id.btn_choose3)

        // 데이터 바인딩
        text1.text = choices[0].name
        btn1.setImageResource(choices[0].iconRes)

        text2.text = choices[1].name
        btn2.setImageResource(choices[1].iconRes)

        text3.text = choices[2].name
        btn3.setImageResource(choices[2].iconRes)

        // 클릭 처리
        btn1.setOnClickListener {
            weaponManager.addOrLevelUp(choices[0])
            isPaused = false
            dialog.dismiss()
        }
        btn2.setOnClickListener {
            weaponManager.addOrLevelUp(choices[1])
            isPaused = false
            dialog.dismiss()
        }
        btn3.setOnClickListener {
            weaponManager.addOrLevelUp(choices[2])
            isPaused = false
            dialog.dismiss()
        }

        dialog.show()
    }
}