package com.example.prototypebug

import android.graphics.Color
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout

/**
 * 미사일: 랜덤한 적의 현재 위치에 착탄 범위를 표시하고, 3초 뒤 미사일이 떨어져 폭발.
 * 폭발 범위 안의 모든 적에게 데미지. (표시된 뒤 적이 범위를 벗어나면 안 맞음)
 *
 * 필요한 이미지:
 *  - R.drawable.missile_target    : 착탄 범위 표시
 *  - R.drawable.explosion         : 폭발 (폭탄과 공용)
 *  - 떨어지는 미사일은 기존 아이콘(R.drawable.missile)을 사용
 */
class MissileWeapon(
    override val type: WeaponType,
    private val arena: ConstraintLayout
) : Weapon {
    override var level = 1
    private var cooldown = 0f

    private val density = arena.resources.displayMetrics.density

    private val warningTime = 3f                   // 표시 후 낙하까지 (초)
    private val dropTime = 0.4f                    // 마지막 몇 초 동안 미사일이 위에서 떨어지는지
    private val blastRadius = 70f * density        // 폭발 범위 (표시 이미지 크기도 이 지름)
    private val missileSize = (36f * density).toInt().coerceAtLeast(1)
    private val explosionDurationMs = 400L

    private val damage get() = 50f + 15f * (level - 1)

    private val strikes = mutableListOf<Strike>()

    override fun update(deltaTime: Float, player: PlayerInfo, enemies: List<Enemy>) {
        strikes.removeAll { strike -> advance(strike, deltaTime, enemies) }

        cooldown -= deltaTime
        if (cooldown <= 0f) {
            mark(enemies)
        }
    }

    private fun mark(enemies: List<Enemy>) {
        val target = enemies.filter { it.health > 0f }.randomOrNull() ?: return // 적이 없으면 다음 프레임에 다시 시도
        cooldown = 3.5f / level

        val x = target.getCenterX()
        val y = target.getCenterY()
        val size = (blastRadius * 2f).toInt()
        val marker = ImageView(arena.context).apply {
            layoutParams = ConstraintLayout.LayoutParams(size, size)
            setImageResource(R.drawable.missile_target)   // 착탄 범위 표시 이미지
            alpha = 0.8f
            this.x = x - size / 2f
            this.y = y - size / 2f
            importantForAccessibility = ImageView.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        arena.addView(marker)
        strikes.add(Strike(x, y, marker))
    }

    /** @return true 이면 이 미사일 처리 끝 */
    private fun advance(strike: Strike, dt: Float, enemies: List<Enemy>): Boolean {
        strike.time += dt

        // 낙하 연출: 마지막 dropTime 동안 화면 위에서 착탄 지점으로 떨어짐
        val dropStart = warningTime - dropTime
        if (strike.time >= dropStart) {
            val missile = strike.missile ?: ImageView(arena.context).apply {
                layoutParams = ConstraintLayout.LayoutParams(missileSize, missileSize)
                setImageResource(type.iconRes)   // 떨어지는 미사일 이미지
                importantForAccessibility = ImageView.IMPORTANT_FOR_ACCESSIBILITY_NO
                arena.addView(this)
                strike.missile = this
            }
            val progress = ((strike.time - dropStart) / dropTime).coerceIn(0f, 1f)
            val startY = -missileSize.toFloat()
            val endY = strike.y - missileSize / 2f
            missile.x = strike.x - missileSize / 2f
            missile.y = startY + (endY - startY) * progress
        }

        if (strike.time < warningTime) return false

        // 폭발: 범위 안의 모든 적에게 데미지
        for (enemy in enemies) {
            if (enemy.health <= 0f) continue
            val dx = enemy.getCenterX() - strike.x
            val dy = enemy.getCenterY() - strike.y
            val reach = blastRadius + enemy.radius
            if (dx * dx + dy * dy <= reach * reach) {
                enemy.takeDamage(damage)
            }
        }

        arena.removeView(strike.marker)
        strike.missile?.let { arena.removeView(it) }
        showExplosion(strike.x, strike.y)
        return true
    }

    private fun showExplosion(x: Float, y: Float) {
        val size = (blastRadius * 2f).toInt()
        val explosion = ImageView(arena.context).apply {
            layoutParams = ConstraintLayout.LayoutParams(size, size)
            setImageResource(R.drawable.explosion)   // 폭발 이미지 (폭탄과 공용)
            setColorFilter(Color.rgb(255, 140, 0))   // 주황색
            this.x = x - size / 2f
            this.y = y - size / 2f
            importantForAccessibility = ImageView.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        arena.addView(explosion)

        explosion.animate()
            .alpha(0f)
            .scaleX(1.3f)
            .scaleY(1.3f)
            .setDuration(explosionDurationMs)
            .withEndAction { arena.removeView(explosion) }
            .start()
    }

    override fun onLevelUp() {}

    private class Strike(val x: Float, val y: Float, val marker: ImageView) {
        var time = 0f
        var missile: ImageView? = null
    }
}