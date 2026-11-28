package com.example.prototypebug

import android.graphics.Color
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import kotlin.math.cos
import kotlin.math.roundToInt

class AxeWeapon(
    override val type: WeaponType,
    private val arena: ConstraintLayout
) : Weapon {
    // 수정: 관통하도록 WeaponProjectile(첫 적에 맞으면 멈춤) 대신 스프라이트를 직접 움직인다.
    private class FlyingAxe(
        val sprite: ImageView,
        var x: Float,                 // 중심 좌표
        var y: Float,
        val horizontalSpeed: Float,
        var verticalSpeed: Float,
        val damage: Float,
        var rotation: Float = 0f
    ) {
        // 수정: 같은 적은 도끼 하나당 한 번만 맞는다 (관통 시 매 프레임 데미지 방지)
        val hitEnemies = mutableListOf<Enemy>()
    }

    override var level = 1
    private val density = arena.resources.displayMetrics.density
    private val gravity = 820f * density
    private val axes = mutableListOf<FlyingAxe>()
    private var cooldown = 0f

    private val sizePx = (28f * density).roundToInt().coerceAtLeast(1)
    private val radius = sizePx / 2f

    // 수정: 여러 개 던질 때 가로 속도를 이만큼씩 벌려서 부채꼴로 퍼지게 함
    private val spreadSpeed = 60f * density

    override fun update(deltaTime: Float, player: PlayerInfo, enemies: List<Enemy>) {
        val retired = axes.filter { axe ->
            val nextX = axe.x + axe.horizontalSpeed * deltaTime
            val nextY = axe.y + axe.verticalSpeed * deltaTime +
                    0.5f * gravity * deltaTime * deltaTime
            axe.verticalSpeed += gravity * deltaTime
            axe.rotation = (axe.rotation + 720f * deltaTime) % 360f
            axe.x = nextX
            axe.y = nextY

            hitEnemies(axe, enemies)
            render(axe)
            isOutsideArena(axe)
        }
        for (axe in retired) {
            arena.removeView(axe.sprite)
            axes.remove(axe)
        }

        cooldown -= deltaTime
        if (cooldown <= 0f) {
            cooldown = (3.6f / level).coerceAtLeast(0.4f)  // 하향: 1.8/Lv(최소 0.25) → 3.6/Lv(최소 0.4)
            val facingX = cos(Math.toRadians(player.playerSprite.rotation.toDouble())).toFloat()
            // 수정: 레벨만큼 던진다 (최대 3개)
            val count = level.coerceAtMost(3)
            for (i in 0 until count) {
                val offset = (i - (count - 1) / 2f) * spreadSpeed
                val sprite = createSprite()
                val axe = FlyingAxe(
                    sprite = sprite,
                    x = player.getCenterX(),
                    y = player.getCenterY(),
                    horizontalSpeed = 72f * density * facingX + offset,
                    verticalSpeed = -340f * density,
                    damage = 18f + 4f * level
                )
                render(axe)
                axes.add(axe)
            }
        }
    }

    private fun hitEnemies(axe: FlyingAxe, enemies: List<Enemy>) {
        for (enemy in enemies) {
            if (enemy.health <= 0f) continue
            if (axe.hitEnemies.any { it === enemy }) continue
            val dx = axe.x - enemy.getCenterX()
            val dy = axe.y - enemy.getCenterY()
            val reach = radius + enemy.radius
            if (dx * dx + dy * dy <= reach * reach) {
                enemy.takeDamage(axe.damage)
                axe.hitEnemies.add(enemy)
            }
        }
    }

    // 기존과 같음: 위쪽으로는 화면 밖에 나가도 유지(다시 떨어지므로), 좌우/아래로 나가면 제거
    private fun isOutsideArena(axe: FlyingAxe): Boolean {
        return axe.x + radius < 0f || axe.x - radius > arena.width || axe.y - radius > arena.height
    }

    private fun createSprite(): ImageView = ImageView(arena.context).apply {
        layoutParams = ConstraintLayout.LayoutParams(sizePx, sizePx)
        setImageResource(type.iconRes)
        setColorFilter(Color.rgb(255, 168, 77))
        importantForAccessibility = ImageView.IMPORTANT_FOR_ACCESSIBILITY_NO
        arena.addView(this)
    }

    private fun render(axe: FlyingAxe) {
        axe.sprite.x = axe.x - radius
        axe.sprite.y = axe.y - radius
        axe.sprite.rotation = axe.rotation
    }

    override fun onLevelUp() {}
}