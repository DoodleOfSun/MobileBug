package com.example.prototypebug

import android.view.View
import kotlin.math.sqrt

open class Enemy {
    var x = 0f
    var y = 0f
    var moveSpeed = 2.5f
    var radius = 30f

    var attackPower = 1
    var health : Float = 10f
    var maxHealth : Float = 10f

    lateinit var enemySprite: View

    // 수정: 프레임 속도와 무관하게 움직이도록 하는 시간 배율.
    // 60fps 의 한 프레임(1/60초)을 1 로 본다. → 60Hz 에서 1, 120Hz 에서 0.5, 30fps 에서 2.
    // EnemySpawnAndManagement.updateAll 이 매 프레임 설정한다.
    // moveSpeed 는 지금처럼 "60fps 기준 한 프레임당 px" 값을 그대로 쓰면 된다.
    var frameScale = 1f

    /** 이번 프레임에 실제로 움직일 거리(px). 이동 코드에서는 moveSpeed 대신 이 값을 쓴다. */
    val frameStep get() = moveSpeed * frameScale

    // 블랙홀 등이 설정하는 임시 이동 목표. null 이면 평소처럼 플레이어를 쫓는다.
    // EnemySpawnAndManagement.updateAll 이 한 프레임 쓰고 비우므로, 끌어당기는 쪽이 매 프레임 다시 설정해야 유지된다.
    var overrideTargetX: Float? = null
    var overrideTargetY: Float? = null

    fun getCenterX() = x + radius
    fun getCenterY() = y + radius

    open fun update(targetX: Float, targetY: Float) {
        // 수정: 적의 왼쪽 위(x, y)가 아니라 중심 기준으로 목표까지의 방향/거리를 계산
        val dx = targetX - getCenterX()
        val dy = targetY - getCenterY()
        val distance = sqrt(dx * dx + dy * dy)

        if (distance > 1f) {
            x += dx / distance * frameStep
            y += dy / distance * frameStep
        }

        enemySprite.x = x
        enemySprite.y = y
    }

    fun takeDamage(damage: Float) {
        health -= damage
    }

}