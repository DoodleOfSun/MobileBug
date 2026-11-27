package com.example.prototypebug

import android.widget.ImageView

class PlayerNormalAttack {
    lateinit var sprite: ImageView

    var attackPower : Float = 10f

    // 자동 공격(원) 관련
    private var attackAngle = 0f          // 현재 회전 각도
    private val attackRadius = 300f       // 플레이어로부터의 거리 (px)
    private val attackRotateSpeed = 235f  // 초당 회전 각도 (도)

    // 수정: 같은 적은 이 시간(초)에 한 번만 맞는다. (한 바퀴 ≈ 1.53초라 한 번 지나갈 때 1회)
    private val hitCooldown = 0.5f
    private val hitCooldowns = mutableMapOf<Enemy, Float>()

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

        // 피격 쿨타임 감소, 끝난 항목 정리 (죽어서 사라진 적도 0.5초 뒤 자동으로 빠짐)
        val iterator = hitCooldowns.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            entry.setValue(entry.value - deltaTime)
            if (entry.value <= 0f) iterator.remove()
        }
    }

    /**
     * 충돌 + 쿨타임을 확인하고 피해를 준다.
     * @return 이번 프레임에 실제로 피해를 줬으면 true
     */
    fun tryHit(enemy: Enemy): Boolean {
        if (enemy.health <= 0f) return false          // 이미 다른 무기로 죽은 적
        if (enemy in hitCooldowns) return false
        if (!checkCollision(enemy.getCenterX(), enemy.getCenterY(), enemy.radius)) return false

        enemy.takeDamage(attackPower)
        hitCooldowns[enemy] = hitCooldown
        return true
    }

    // 수정: 인자를 "중심 좌표 + 반지름"으로 받는다. (예전엔 중심에 반지름/2 를 또 더하고, 반지름도 절반으로 써서 판정이 어긋났음)
    fun checkCollision(enemyCenterX: Float, enemyCenterY: Float, enemyRadius: Float): Boolean {
        // 원의 중심
        val attackCenterX = sprite.x + sprite.width / 2f
        val attackCenterY = sprite.y + sprite.height / 2f

        // 두 중심 사이 거리
        val dx = attackCenterX - enemyCenterX
        val dy = attackCenterY - enemyCenterY
        val distance = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()

        // 충돌 판정 (두 반지름의 합보다 작으면 충돌)
        val attackRadius = sprite.width / 2f

        return distance < (attackRadius + enemyRadius)
    }
}