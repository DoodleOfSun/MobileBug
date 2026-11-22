package com.example.prototypebug

import android.widget.ImageView

class EnemyBullet(
    val sprite: ImageView,     // 탄 그림
    var x: Float,              // 탄 중심 (가로)
    var y: Float,              // 탄 중심 (세로)
    private val vx: Float,     // 1초에 가로 이동량
    private val vy: Float,     // 1초에 세로 이동량
    val damage: Int,           // 피해량
    val radius: Float          // 탄 반지름 (충돌 판정용, 쏘는 몬스터가 정해줌)
) {
    var life = 4f              // 수명 4초

    fun update(deltaTime: Float) {
        x += vx * deltaTime
        y += vy * deltaTime
        life -= deltaTime

        sprite.x = x - radius  // 그림은 왼쪽 위 기준이라 중심 맞추기
        sprite.y = y - radius
    }
}
