package com.example.prototypebug

import android.graphics.Color
import androidx.constraintlayout.widget.ConstraintLayout
import kotlin.math.cos
import kotlin.math.sin

class DaggerWeapon(
    override val type: WeaponType,
    private val arena: ConstraintLayout
) : Weapon {
    override var level = 1
    private var cooldown = 0f
    private val speed = 540f * arena.resources.displayMetrics.density
    private val daggers = mutableListOf<FlyingDagger>()

    override fun update(deltaTime: Float, player: PlayerInfo, enemies: List<Enemy>) {
        cooldown -= deltaTime
        if (cooldown <= 0f) {
            cooldown = 0.8f
            launch(player)
        }

        daggers.removeAll { dagger ->
            val projectile = dagger.projectile
            val hit = projectile.advance(
                projectile.x + dagger.velocityX * deltaTime,
                projectile.y + dagger.velocityY * deltaTime,
                dagger.spriteRotation,
                enemies
            )
            val expired = hit || projectile.isOutsideArena()
            if (expired) projectile.remove()
            expired
        }
    }

    private fun launch(player: PlayerInfo) {
        // 수정: 레벨만큼 발사 (최대 3발). 이동 방향을 중심으로 spreadDegrees 간격 부채꼴로 퍼짐
        val count = level.coerceAtMost(3)
        for (i in 0 until count) {
            val heading = player.playerSprite.rotation + (i - (count - 1) / 2f) * spreadDegrees
            val radians = Math.toRadians(heading.toDouble())
            val projectile = WeaponProjectile(
                arena = arena,
                iconRes = type.iconRes,
                tint = Color.rgb(232, 218, 255),
                startX = player.getCenterX(),
                startY = player.getCenterY(),
                sizeDp = 24f,
                damage = 9f + 3f * (level - 1)
            )
            daggers.add(
                FlyingDagger(
                    projectile,
                    cos(radians).toFloat() * speed,
                    sin(radians).toFloat() * speed,
                    // The dagger image points 45 degrees above the positive X axis.
                    heading + 45f
                )
            )
        }
    }

    private val spreadDegrees = 12f

    override fun onLevelUp() {}

    private class FlyingDagger(
        val projectile: WeaponProjectile,
        val velocityX: Float,
        val velocityY: Float,
        val spriteRotation: Float
    )
}