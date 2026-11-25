package com.example.prototypebug

import android.graphics.Color
import androidx.constraintlayout.widget.ConstraintLayout
import kotlin.math.cos

class AxeWeapon(
    override val type: WeaponType,
    private val arena: ConstraintLayout
) : Weapon {
    private class FlyingAxe(
        val projectile: WeaponProjectile,
        val horizontalSpeed: Float,
        var verticalSpeed: Float,
        var rotation: Float = 0f
    )

    override var level = 1
    private val density = arena.resources.displayMetrics.density
    private val gravity = 820f * density
    private val axes = mutableListOf<FlyingAxe>()
    private var cooldown = 0f

    override fun update(deltaTime: Float, player: PlayerInfo, enemies: List<Enemy>) {
        val retired = axes.filter { axe ->
            val projectile = axe.projectile
            val nextX = projectile.x + axe.horizontalSpeed * deltaTime
            val nextY = projectile.y + axe.verticalSpeed * deltaTime +
                0.5f * gravity * deltaTime * deltaTime
            axe.verticalSpeed += gravity * deltaTime
            axe.rotation = (axe.rotation + 720f * deltaTime) % 360f

            projectile.advance(nextX, nextY, axe.rotation, enemies) ||
                projectile.isOutsideArena(allowAboveArena = true)
        }
        for (axe in retired) {
            axe.projectile.remove()
            axes.remove(axe)
        }

        cooldown -= deltaTime
        if (cooldown <= 0f) {
            cooldown = (1.8f / level).coerceAtLeast(0.25f)
            val facingX = cos(Math.toRadians(player.playerSprite.rotation.toDouble())).toFloat()
            val projectile = WeaponProjectile(
                arena = arena,
                iconRes = type.iconRes,
                tint = Color.rgb(255, 168, 77),
                startX = player.getCenterX(),
                startY = player.getCenterY(),
                sizeDp = 28f,
                damage = 18f + 4f * level
            )
            axes.add(FlyingAxe(projectile, 72f * density * facingX, -340f * density))
        }
    }

    override fun onLevelUp() {}
}
