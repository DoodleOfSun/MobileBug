package com.example.prototypebug

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import kotlin.jvm.java

class MapSelectActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mapselect)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        findViewById<ImageButton>(R.id.btn_map1).setOnClickListener {
            startActivity(Intent(this, Map1Activity::class.java))
        }

        findViewById<ImageButton>(R.id.btn_map2).setOnClickListener {
            startActivity(Intent(this, Map2Activity::class.java))
        }
    }
}