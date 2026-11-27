package com.example.prototypebug

import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * 낫: 타겟 없이 랜덤 방향으로 직선으로 날아간다.
 * 적을 관통하며, 같은 적은 낫 하나당 한 번만 맞는다. 화면 밖으로 나가면 사라진다.
 */
class SickleWeapon(
    override val type: WeaponType,
    private val arena: ConstraintLayout
) : Weapon {
    override var level = 1
    private var cooldown = 0f

    private val density = arena.resources.displayMetrics.density
    private val speed = 420f * density   // 비행 속도
    private val spinSpeed = 720f         // 초당 회전 각도

    private val sizePx = (28f * density).roundToInt().coerceAtLeast(1)
    private val radius = sizePx / 2f

    private val damage get() = 12f + 3f * (level - 1)

    private val sickles = mutableListOf<FlyingSickle>()

    override fun update(deltaTime: Float, player: PlayerInfo, enemies: List<Enemy>) {
        cooldown -= deltaTime
        if (cooldown <= 0f) {
            cooldown = 1.0f / level
            launch(player)
        }

        sickles.removeAll { sickle ->
            sickle.x += sickle.velocityX * deltaTime
            sickle.y += sickle.velocityY * deltaTime
            sickle.rotation = (sickle.rotation + spinSpeed * deltaTime) % 360f

            hitEnemies(sickle, enemies)
            render(sickle)

            val expired = isOutsideArena(sickle)
            if (expired) arena.removeView(sickle.sprite)
            expired
        }
    }

    private fun launch(player: PlayerInfo) {
        val angle = Random.nextDouble(0.0, 2.0 * Math.PI)
        val sprite = ImageView(arena.context).apply {
            layoutParams = ConstraintLayout.LayoutParams(sizePx, sizePx)
            setImageResource(type.iconRes)   // 색 덧씌우기 없음 (원래 이미지 색 그대로)
            importantForAccessibility = ImageView.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        arena.addView(sprite)

        val sickle = FlyingSickle(
            sprite,
            player.getCenterX(),
            player.getCenterY(),
            cos(angle).toFloat() * speed,
            sin(angle).toFloat() * speed
        )
        render(sickle)
        sickles.add(sickle)
    }

    // 관통: 겹친 적은 전부 맞되, 같은 적은 한 번만
    private fun hitEnemies(sickle: FlyingSickle, enemies: List<Enemy>) {
        for (enemy in enemies) {
            if (enemy.health <= 0f) continue
            if (sickle.hitEnemies.any { it === enemy }) continue
            val dx = sickle.x - enemy.getCenterX()
            val dy = sickle.y - enemy.getCenterY()
            val reach = radius + enemy.radius
            if (dx * dx + dy * dy <= reach * reach) {
                enemy.takeDamage(damage)
                sickle.hitEnemies.add(enemy)
            }
        }
    }

    private fun isOutsideArena(sickle: FlyingSickle): Boolean {
        return sickle.x + radius < 0f || sickle.x - radius > arena.width ||
                sickle.y + radius < 0f || sickle.y - radius > arena.height
    }

    // x, y 는 중심 좌표
    private fun render(sickle: FlyingSickle) {
        sickle.sprite.x = sickle.x - radius
        sickle.sprite.y = sickle.y - radius
        sickle.sprite.rotation = sickle.rotation
    }

    override fun onLevelUp() {}

    private class FlyingSickle(
        val sprite: ImageView,
        var x: Float,
        var y: Float,
        val velocityX: Float,
        val velocityY: Float
    ) {
        var rotation = 0f
        val hitEnemies = mutableListOf<Enemy>()
    }
}