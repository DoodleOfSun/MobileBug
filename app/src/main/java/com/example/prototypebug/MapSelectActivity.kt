package com.example.prototypebug

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.widget.ImageButton
import kotlin.jvm.java

class MapSelectActivity : AudioActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mapselect)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        findViewById<ImageButton>(R.id.btn_map1).setGameClickListener(::playSound) {
            startActivity(Intent(this, Map1Activity::class.java))
        }

        findViewById<ImageButton>(R.id.btn_map2).setGameClickListener(::playSound) {
            // 맵 2로 게임 시작 (나중에 Map2Activity로 바꿔도 됨)
            //startActivity(Intent(this, GameActivity::class.java))
        }
    }
}
