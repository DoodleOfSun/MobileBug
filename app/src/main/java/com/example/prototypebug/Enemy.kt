package com.example.prototypebug

import android.view.View
import kotlin.math.sqrt

class Enemy {
    var x = 0f
    var y = 0f
    var moveSpeed = 2.5f
    val radius = 30f

    var attackPower = 1
    var health : Float = 10f
    var maxHealth : Float = 10f

    lateinit var enemySprite: View

    fun getCenterX() = x + radius
    fun getCenterY() = y + radius

    fun update(targetX: Float, targetY: Float) {
        val dx = targetX - x
        val dy = targetY - y
        val distance = sqrt(dx * dx + dy * dy)

        if (distance > 1f) {
            x += dx / distance * moveSpeed
            y += dy / distance * moveSpeed
        }

        enemySprite.x = x
        enemySprite.y = y
    }

    fun takeDamage(damage: Float) {
        health -= damage
    }

}