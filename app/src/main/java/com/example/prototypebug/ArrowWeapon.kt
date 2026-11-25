package com.example.prototypebug

import android.graphics.Color
import androidx.constraintlayout.widget.ConstraintLayout
import kotlin.math.atan2
import kotlin.math.sqrt

class ArrowWeapon(
    override val type: WeaponType,
    private val arena: ConstraintLayout
) : Weapon {
    override var level = 1

    private val density = arena.resources.displayMetrics.density
    private val speed = 380f * density
    private val offscreenMargin = 48f * density
    private val arrows = mutableListOf<Flight>()
    private var cooldown = 0f

    override fun update(deltaTime: Float, player: PlayerInfo, enemies: List<Enemy>) {
        cooldown -= deltaTime
        if (cooldown <= 0f) {
            launch(player, enemies)
        }

        arrows.removeAll { flight ->
            val finished = advance(flight, deltaTime, enemies)
            if (finished) flight.projectile.remove()
            finished
        }
    }

    private fun launch(player: PlayerInfo, enemies: List<Enemy>) {
        val playerX = player.getCenterX()
        val playerY = player.getCenterY()
        val target = enemies.asSequence().filter { it.health > 0f }.minByOrNull { enemy ->
            val dx = enemy.getCenterX() - playerX
            val dy = enemy.getCenterY() - playerY
            dx * dx + dy * dy
        } ?: return

        cooldown = 1.2f
        val projectile = WeaponProjectile(
            arena,
            type.iconRes,
            Color.rgb(175, 250, 100),
            playerX,
            playerY,
            sizeDp = 22f,
            damage = 12f + (level - 1) * 3f
        )
        arrows.add(Flight(projectile, target))
    }

    private fun advance(flight: Flight, deltaTime: Float, enemies: List<Enemy>): Boolean {
        val target = flight.target
        if (target.health <= 0f || target !in enemies) return true

        val projectile = flight.projectile
        val dx = target.getCenterX() - projectile.x
        val dy = target.getCenterY() - projectile.y
        val distance = sqrt(dx * dx + dy * dy)
        val travelTime = minOf(deltaTime, flight.timeLeft)
        val fraction = if (distance > 0f) (speed * travelTime / distance).coerceAtMost(1f) else 0f
        val rotation = Math.toDegrees(atan2(dy, dx).toDouble()).toFloat()
        val hit = projectile.advance(
            projectile.x + dx * fraction,
            projectile.y + dy * fraction,
            rotation,
            enemies
        )
        flight.timeLeft -= travelTime
        return hit || flight.timeLeft <= 0f || projectile.isOutsideArena(offscreenMargin)
    }

    override fun onLevelUp() {}

    private class Flight(val projectile: WeaponProjectile, val target: Enemy) {
        var timeLeft = 3f
    }
}
