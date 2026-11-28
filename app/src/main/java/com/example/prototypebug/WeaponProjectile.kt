package com.example.prototypebug

import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import kotlin.math.roundToInt
import kotlin.math.sqrt

internal class WeaponProjectile(
    private val arena: ConstraintLayout,
    iconRes: Int,
    tint: Int,
    startX: Float,
    startY: Float,
    sizeDp: Float,
    private val damage: Float
) {
    private val size = (sizeDp * arena.resources.displayMetrics.density).roundToInt().coerceAtLeast(1)
    private val radius = size / 2f
    private val sprite = ImageView(arena.context).apply {
        layoutParams = ConstraintLayout.LayoutParams(size, size)
        setImageResource(iconRes)
        setColorFilter(tint)
        importantForAccessibility = ImageView.IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    var x = startX
        private set
    var y = startY
        private set

    init {
        arena.addView(sprite)
        render(0f)
    }

    fun advance(nextX: Float, nextY: Float, rotation: Float, enemies: List<Enemy>): Boolean {
        val dx = nextX - x
        val dy = nextY - y
        var firstHit: Enemy? = null
        var firstHitFraction = Float.POSITIVE_INFINITY
        for (enemy in enemies) {
            if (enemy.health <= 0f) continue
            val fraction = hitFraction(enemy, dx, dy) ?: continue
            if (fraction < firstHitFraction) {
                firstHit = enemy
                firstHitFraction = fraction
            }
        }
        if (firstHit != null) {
            firstHit.takeDamage(damage)
            return true
        }

        x = nextX
        y = nextY
        render(rotation)
        return false
    }

    fun isOutsideArena(margin: Float = 0f, allowAboveArena: Boolean = false): Boolean {
        return x + radius < -margin || x - radius > arena.width + margin ||
            (!allowAboveArena && y + radius < -margin) || y - radius > arena.height + margin
    }

    fun remove() {
        arena.removeView(sprite)
    }

    private fun hitFraction(enemy: Enemy, dx: Float, dy: Float): Float? {
        val offsetX = x - enemy.getCenterX()
        val offsetY = y - enemy.getCenterY()
        val combinedRadius = radius + enemy.radius
        val distanceSquared = offsetX * offsetX + offsetY * offsetY - combinedRadius * combinedRadius
        if (distanceSquared <= 0f) return 0f

        val stepSquared = dx * dx + dy * dy
        val projection = offsetX * dx + offsetY * dy
        if (stepSquared == 0f || projection >= 0f) return null

        val discriminant = projection * projection - stepSquared * distanceSquared
        if (discriminant < 0f) return null
        val fraction = (-projection - sqrt(discriminant)) / stepSquared
        return fraction.takeIf { it in 0f..1f }
    }

    private fun render(rotation: Float) {
        sprite.x = x - radius
        sprite.y = y - radius
        sprite.rotation = rotation
    }
}
