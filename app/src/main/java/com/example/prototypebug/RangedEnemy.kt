package com.example.prototypebug

import android.graphics.Color
import kotlin.math.sqrt

open class RangedEnemy : Enemy() {

    // ── 능력치 (난이도 조절은 여기 숫자만) ──
    var attackRange = 380f      // 이 거리가 되면 멈추고 쏨
    var fireInterval = 2.5f     // 2.5초마다 발사
    var fireTimer = 1.0f        // 발사 타이머 (등장 후 1.5초 뒤 첫 발사)
    var bulletSpeed = 420f      // 탄 속도 (1초에 420px)
    var bulletDamage = 2        // 탄 피해량
    var bulletSize = 24         // 탄 크기 (지름 px)
    var bulletColor = Color.parseColor("#E53935")   // 탄 색 (빨강)

    // ── 화면 크기 (화면 밖에서 멈추거나 쏘지 않게, 등장할 때 넣어줌) ──
    var screenWidth = 0f
    var screenHeight = 0f

    init {
        moveSpeed = 1.8f        // 일반(2.5)보다 느림
        health = 15f            // 일반(10)보다 튼튼
        maxHealth = 15f
    }

    // 화면 안에 완전히 들어와 있나?
    fun isOnScreen(): Boolean {
        return x >= 0f && y >= 0f &&
               x + radius * 2 <= screenWidth &&
               y + radius * 2 <= screenHeight
    }

    // 플레이어가 사거리 안에 있나?
    fun isInRange(player: PlayerInfo): Boolean {
        val dx = player.getCenterX() - getCenterX()
        val dy = player.getCenterY() - getCenterY()
        return sqrt(dx * dx + dy * dy) <= attackRange + 40f
    }

    // 움직임: 멀거나 화면 밖이면 다가감, 아니면 멈춤
    override fun update(targetX: Float, targetY: Float) {
        moveToward(targetX, targetY, detouring = false)
    }

    internal fun moveToward(targetX: Float, targetY: Float, detouring: Boolean) {
        val dx = targetX - getCenterX()
        val dy = targetY - getCenterY()
        val dist = sqrt(dx * dx + dy * dy)

        if (dist > 1f && (detouring || dist > attackRange || !isOnScreen())) {
            val step = if (detouring) minOf(moveSpeed, dist) else moveSpeed
            x += dx / dist * step
            y += dy / dist * step
        }

        enemySprite.x = x
        enemySprite.y = y
    }
}
