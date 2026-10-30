package com.example.prototypebug

import android.widget.ImageView

class Reward {
    lateinit var rewardSprite : ImageView
    var exp : Float = 0.01f

    var radius : Float = 22.5f

    fun getCenterX() = rewardSprite.x + radius
    fun getCenterY() = rewardSprite.y + radius




}