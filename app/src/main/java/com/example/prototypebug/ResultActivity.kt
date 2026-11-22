package com.example.prototypebug

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.widget.Button
import android.widget.TextView

class ResultActivity : AudioActivity() {

    private var playedResultSound = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        val resultText = findViewById<TextView>(R.id.text_clear)
        val scoreText = findViewById<TextView>(R.id.text_score)
        val btnRetry = findViewById<Button>(R.id.btn_restart)
        val btnMapSelect = findViewById<Button>(R.id.btn_mapselect)


        // Intent로 데이터 받기
        val isWin = intent.getBooleanExtra("isWin", false)
        val score = intent.getFloatExtra("score", 0f)

        scoreText.text = "score : ${score.toInt()}"

        resultText.text = if (isWin) "nailed it" else "died"
        val mapClassName = intent.getStringExtra("mapClass")

        btnRetry.setGameClickListener(::playSound) {
            if (mapClassName != null) {
                val clazz = Class.forName(mapClassName)
                startActivity(Intent(this, clazz))
                finish()
            }
        }

        // 맵 선택으로
        btnMapSelect.setGameClickListener(::playSound) {
            startActivity(Intent(this, MapSelectActivity::class.java))
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!playedResultSound) {
            playedResultSound = true
            val isWin = intent.getBooleanExtra("isWin", false)
            playSound(if (isWin) GameSound.VICTORY else GameSound.DEFEAT)
        }
    }
}
