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
        // 수정: 레벨만큼 발사 (최대 3발). 가까운 적부터 서로 다른 적을 하나씩 노린다 (적이 더 적으면 그 수만큼만)
        val targets = enemies.filter { it.health > 0f }.sortedBy { enemy ->
            val dx = enemy.getCenterX() - playerX
            val dy = enemy.getCenterY() - playerY
            dx * dx + dy * dy
        }.take(level.coerceAtMost(3))
        if (targets.isEmpty()) return

        cooldown = 1.2f
        for (target in targets) {
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