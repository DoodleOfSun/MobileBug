package com.example.prototypebug

import android.graphics.Color
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

/**
 * 레이저: 플레이어에서 랜덤 방향으로 화면 끝까지 즉시 발사.
 * 선 위의 모든 적을 관통해 한 번씩 데미지를 주고, 빔은 서서히 사라진다.
 * 빔은 이미지 없이 색이 칠해진 막대(View)로 그린다.
 */
class LaserWeapon(
    override val type: WeaponType,
    private val arena: ConstraintLayout
) : Weapon {
    override var level = 1
    private var cooldown = 0f

    private val density = arena.resources.displayMetrics.density

    private val beamThickness = (10f * density).toInt().coerceAtLeast(1) // 빔 두께
    private val beamColor = Color.argb(230, 255, 70, 70)                // 빔 색
    private val fadeDurationMs = 450L                                   // 사라지는 시간

    private val damage get() = 20f + 5f * (level - 1)

    override fun update(deltaTime: Float, player: PlayerInfo, enemies: List<Enemy>) {
        cooldown -= deltaTime
        if (cooldown <= 0f) {
            cooldown = 2.0f / level
            fire(player, enemies)
        }
    }

    private fun fire(player: PlayerInfo, enemies: List<Enemy>) {
        val startX = player.getCenterX()
        val startY = player.getCenterY()
        val angle = Random.nextDouble(0.0, 2.0 * Math.PI)
        val dirX = cos(angle).toFloat()
        val dirY = sin(angle).toFloat()
        val length = hypot(arena.width.toFloat(), arena.height.toFloat()) // 어디서 쏴도 화면 끝까지 닿는 길이
        val halfThickness = beamThickness / 2f

        // 관통 판정: 플레이어 앞쪽(t >= 0)에 있고, 빔 선과의 거리가 (빔 반폭 + 적 반지름) 이내
        for (enemy in enemies) {
            if (enemy.health <= 0f) continue
            val offsetX = enemy.getCenterX() - startX
            val offsetY = enemy.getCenterY() - startY
            val along = offsetX * dirX + offsetY * dirY
            if (along < 0f || along > length) continue
            val across = abs(offsetX * dirY - offsetY * dirX)
            if (across <= halfThickness + enemy.radius) {
                enemy.takeDamage(damage)
            }
        }

        // 빔 그리기: 왼쪽 끝을 플레이어 중심에 두고 발사 방향으로 회전
        val beam = View(arena.context).apply {
            layoutParams = ConstraintLayout.LayoutParams(length.toInt(), beamThickness)
            setBackgroundColor(beamColor)
            pivotX = 0f
            pivotY = halfThickness
            x = startX
            y = startY - halfThickness
            rotation = Math.toDegrees(atan2(dirY, dirX).toDouble()).toFloat()
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        arena.addView(beam)

        beam.animate()
            .alpha(0f)
            .setDuration(fadeDurationMs)
            .withEndAction { arena.removeView(beam) }
            .start()
    }

    override fun onLevelUp() {}
}