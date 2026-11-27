package com.example.prototypebug

import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * 블랙홀: 플레이어 근처 랜덤 위치에 반투명하게 회전하며 잠시 존재한다.
 * 범위 안의 적은 그동안 플레이어 대신 블랙홀 중심을 향해 이동한다. (데미지 없음)
 */
class BlackHoleWeapon(
    override val type: WeaponType,
    private val arena: ConstraintLayout
) : Weapon {
    override var level = 1
    private var cooldown = 0f

    private val density = arena.resources.displayMetrics.density

    private val pullRadius = 140f * density        // 끌어당기는 범위 (이미지 크기도 이 지름)
    private val duration = 3f                      // 존재 시간 (초)
    private val fadeTime = 0.3f                    // 나타나고 사라지는 시간 (초)
    private val maxAlpha = 0.55f                   // 투명도 (0 = 안 보임, 1 = 불투명)
    private val spinSpeed = 120f                   // 초당 회전 각도
    private val spawnDistanceMin = 120f * density  // 플레이어로부터 생성 거리
    private val spawnDistanceMax = 260f * density

    private val holes = mutableListOf<Hole>()

    override fun update(deltaTime: Float, player: PlayerInfo, enemies: List<Enemy>) {
        holes.removeAll { hole ->
            val finished = tick(hole, deltaTime, enemies)
            if (finished) arena.removeView(hole.sprite)
            finished
        }

        cooldown -= deltaTime
        if (cooldown <= 0f) {
            cooldown = 8f / level
            spawn(player)
        }
    }

    private fun spawn(player: PlayerInfo) {
        val angle = Random.nextDouble(0.0, 2.0 * Math.PI)
        val distance = Random.nextFloat() * (spawnDistanceMax - spawnDistanceMin) + spawnDistanceMin
        val centerX = (player.getCenterX() + cos(angle).toFloat() * distance)
            .coerceIn(0f, arena.width.toFloat())
        val centerY = (player.getCenterY() + sin(angle).toFloat() * distance)
            .coerceIn(0f, arena.height.toFloat())

        val size = (pullRadius * 2f).toInt()
        val sprite = ImageView(arena.context).apply {
            layoutParams = ConstraintLayout.LayoutParams(size, size)
            setImageResource(type.iconRes)   // 블랙홀 이미지 (전용 이펙트 이미지가 생기면 여기만 바꾸면 됨)
            alpha = 0f
            x = centerX - size / 2f
            y = centerY - size / 2f
            importantForAccessibility = ImageView.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        arena.addView(sprite)
        holes.add(Hole(sprite, centerX, centerY))
    }

    /** @return true 이면 블랙홀 제거 */
    private fun tick(hole: Hole, dt: Float, enemies: List<Enemy>): Boolean {
        hole.time += dt
        if (hole.time >= duration) return true

        // 회전 + 서서히 나타났다가 서서히 사라짐
        hole.sprite.rotation = (hole.sprite.rotation + spinSpeed * dt) % 360f
        val fade = minOf(1f, hole.time / fadeTime, (duration - hole.time) / fadeTime)
        hole.sprite.alpha = maxAlpha * fade

        // 범위 안의 적: 다음 프레임 이동 목표를 블랙홀 중심으로 바꿈
        for (enemy in enemies) {
            if (enemy.health <= 0f) continue
            val dx = enemy.getCenterX() - hole.x
            val dy = enemy.getCenterY() - hole.y
            val reach = pullRadius + enemy.radius
            if (dx * dx + dy * dy <= reach * reach) {
                enemy.overrideTargetX = hole.x
                enemy.overrideTargetY = hole.y
            }
        }
        return false
    }

    override fun onLevelUp() {}

    private class Hole(val sprite: ImageView, val x: Float, val y: Float) {
        var time = 0f
    }
}