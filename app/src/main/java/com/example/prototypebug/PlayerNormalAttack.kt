package com.example.prototypebug

import android.widget.ImageView

class PlayerNormalAttack {
    lateinit var sprite: ImageView

    var attackPower : Float = 10f

    // 자동 공격(원) 관련
    private var attackAngle = 0f          // 현재 회전 각도
    private val attackRadius = 300f       // 플레이어로부터의 거리 (px)
    private val attackRotateSpeed = 235f  // 초당 회전 각도 (도)


    fun update(playerX: Float, playerY: Float, playerSize: Float, deltaTime: Float) {
        // 각도 증가
        attackAngle += attackRotateSpeed * deltaTime
        if (attackAngle >= 360f) attackAngle -= 360f

        val rad = Math.toRadians(attackAngle.toDouble())

        // 플레이어 중심
        val centerX = playerX + playerSize / 2f
        val centerY = playerY + playerSize / 2f

        // 원 위치 (스프라이트 중심 기준)
        val attackX = centerX + (attackRadius * Math.cos(rad)).toFloat() - sprite.width / 2f
        val attackY = centerY + (attackRadius * Math.sin(rad)).toFloat() - sprite.height / 2f

        sprite.x = attackX
        sprite.y = attackY
    }

    fun checkCollision(enemyX: Float, enemyY: Float, enemySize: Float): Boolean {
        // 원의 중심
        val attackCenterX = sprite.x + sprite.width / 2f
        val attackCenterY = sprite.y + sprite.height / 2f

        // 적의 중심
        val enemyCenterX = enemyX + enemySize / 2f
        val enemyCenterY = enemyY + enemySize / 2f

        // 두 중심 사이 거리
        val dx = attackCenterX - enemyCenterX
        val dy = attackCenterY - enemyCenterY
        val distance = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()

        // 충돌 판정 (두 반지름의 합보다 작으면 충돌)
        val attackRadius = sprite.width / 2f
        val enemyRadius = enemySize / 2f

        return distance < (attackRadius + enemyRadius)
    }
}