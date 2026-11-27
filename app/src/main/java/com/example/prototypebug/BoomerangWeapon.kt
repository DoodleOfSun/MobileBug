package com.example.prototypebug

import android.graphics.Color
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 부메랑: 가장 가까운 적(없으면 바라보는 방향)으로 던져져 점점 느려지다가,
 * 멈추거나 벽에 닿으면 플레이어에게 가속하며 돌아온다.
 * 적을 관통하며, 날아갈 때 한 번 / 돌아올 때 한 번씩 같은 적을 때릴 수 있다.
 */
class BoomerangWeapon(
    override val type: WeaponType,
    private val arena: ConstraintLayout
) : Weapon {
    override var level = 1

    private val density = arena.resources.displayMetrics.density

    // 이동 관련 (dp 기준 → px 변환)
    private val launchSpeed = 520f * density        // 처음 던지는 속도
    private val deceleration = 700f * density       // 날아가는 동안 감속
    private val returnAcceleration = 1100f * density // 돌아올 때 가속
    private val maxReturnSpeed = 640f * density     // 돌아올 때 최대 속도
    private val catchDistance = 28f * density       // 이 거리 안에 오면 회수
    private val spinSpeed = 900f                    // 초당 회전 각도
    private val maxLifetime = 5f                    // 안전장치: 최대 생존 시간

    private val sizePx = (30f * density).roundToInt().coerceAtLeast(1)
    private val radius = sizePx / 2f

    private val boomerangs = mutableListOf<Boomerang>()
    private var cooldown = 0f

    private val damage get() = 15f + (level - 1) * 5f
    private val count get() = (1 + (level - 1) / 2).coerceAtMost(4) // Lv1:1, Lv3:2, Lv5:3, Lv7+:4
    private val spreadDegrees = 18f

    override fun update(deltaTime: Float, player: PlayerInfo, enemies: List<Enemy>) {
        boomerangs.removeAll { boomerang ->
            val finished = advance(boomerang, deltaTime, player, enemies)
            if (finished) arena.removeView(boomerang.sprite)
            finished
        }

        cooldown -= deltaTime
        if (cooldown <= 0f) {
            cooldown = (1.6f - 0.1f * (level - 1)).coerceAtLeast(0.8f)
            launch(player, enemies)
        }
    }

    private fun launch(player: PlayerInfo, enemies: List<Enemy>) {
        val startX = player.getCenterX()
        val startY = player.getCenterY()

        // 가장 가까운 살아있는 적 방향, 없으면 플레이어가 바라보는 방향
        val target = enemies.asSequence().filter { it.health > 0f }.minByOrNull { enemy ->
            val dx = enemy.getCenterX() - startX
            val dy = enemy.getCenterY() - startY
            dx * dx + dy * dy
        }
        val baseAngle = if (target != null) {
            Math.toDegrees(
                atan2(target.getCenterY() - startY, target.getCenterX() - startX).toDouble()
            ).toFloat()
        } else {
            player.playerSprite.rotation
        }

        // 여러 개일 때 부채꼴로 퍼뜨림
        val n = count
        for (i in 0 until n) {
            val offset = (i - (n - 1) / 2f) * spreadDegrees
            val rad = Math.toRadians((baseAngle + offset).toDouble())
            boomerangs.add(
                Boomerang(
                    sprite = createSprite(),
                    x = startX,
                    y = startY,
                    dirX = cos(rad).toFloat(),
                    dirY = sin(rad).toFloat(),
                    speed = launchSpeed
                ).also { render(it) }
            )
        }
    }

    /** @return true 이면 부메랑을 제거 */
    private fun advance(b: Boomerang, dt: Float, player: PlayerInfo, enemies: List<Enemy>): Boolean {
        b.lifetime += dt
        b.rotation = (b.rotation + spinSpeed * dt) % 360f

        if (!b.returning) {
            // 날아가는 중: 직진하며 감속
            b.x += b.dirX * b.speed * dt
            b.y += b.dirY * b.speed * dt
            b.speed -= deceleration * dt

            if (b.speed <= 0f || hitWall(b)) {
                b.returning = true
                b.speed = 0f
                b.hitEnemies.clear() // 돌아올 때 같은 적을 다시 때릴 수 있게
            }
        } else {
            // 돌아오는 중: 플레이어의 현재 위치로 가속하며 추적
            val dx = player.getCenterX() - b.x
            val dy = player.getCenterY() - b.y
            val dist = sqrt(dx * dx + dy * dy)
            b.speed = (b.speed + returnAcceleration * dt).coerceAtMost(maxReturnSpeed)
            val step = b.speed * dt

            if (dist <= catchDistance || step >= dist) return true // 회수

            b.x += dx / dist * step
            b.y += dy / dist * step
        }

        hitEnemies(b, enemies)
        render(b)
        return b.lifetime >= maxLifetime
    }

    private fun hitEnemies(b: Boomerang, enemies: List<Enemy>) {
        for (enemy in enemies) {
            if (enemy.health <= 0f) continue
            if (b.hitEnemies.any { it === enemy }) continue
            val dx = b.x - enemy.getCenterX()
            val dy = b.y - enemy.getCenterY()
            val reach = radius + enemy.radius
            if (dx * dx + dy * dy <= reach * reach) {
                enemy.takeDamage(damage)
                b.hitEnemies.add(enemy)
            }
        }
    }

    private fun hitWall(b: Boomerang): Boolean {
        val w = arena.width.toFloat()
        val h = arena.height.toFloat()
        var hit = false
        if (b.x - radius < 0f) { b.x = radius; hit = true }
        if (b.y - radius < 0f) { b.y = radius; hit = true }
        if (b.x + radius > w) { b.x = w - radius; hit = true }
        if (b.y + radius > h) { b.y = h - radius; hit = true }
        return hit
    }

    private fun createSprite(): ImageView = ImageView(arena.context).apply {
        layoutParams = ConstraintLayout.LayoutParams(sizePx, sizePx)
        setImageResource(type.iconRes)
        setColorFilter(Color.rgb(110, 200, 255))
        importantForAccessibility = ImageView.IMPORTANT_FOR_ACCESSIBILITY_NO
        arena.addView(this)
    }

    // x, y 는 중심 좌표 (WeaponProjectile 과 동일한 방식)
    private fun render(b: Boomerang) {
        b.sprite.x = b.x - radius
        b.sprite.y = b.y - radius
        b.sprite.rotation = b.rotation
    }

    override fun onLevelUp() {}

    private class Boomerang(
        val sprite: ImageView,
        var x: Float,
        var y: Float,
        val dirX: Float,
        val dirY: Float,
        var speed: Float
    ) {
        var returning = false
        var rotation = 0f
        var lifetime = 0f
        val hitEnemies = mutableListOf<Enemy>()
    }
}