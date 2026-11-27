package com.example.prototypebug

import android.content.Context
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import kotlin.math.sqrt

class BombWeapon(
    override val type: WeaponType,
    private val container: ConstraintLayout,
    private val context: Context
) : Weapon {
    override var level = 1

    private val bombs = mutableListOf<Bomb>()

    private val bombSize = 120

    private var timer = 0f
    private val spawnInterval = 4f

    data class Bomb(
        var x: Float,
        var y: Float,
        var timer: Float = 0f,
        val explodeTime: Float = 2f,
    ) {

        lateinit var sprite: ImageView
        // 수정: 스프라이트 크기(bombSize = 120px)의 절반. 예전엔 30 이라 폭발이 왼쪽 위로 30px 어긋났음
        val radius = 60f
        fun getCenterX() = x + radius
        fun getCenterY() = y + radius
    }

    override fun update(deltaTime: Float, player: PlayerInfo, enemies: List<Enemy>) {
        // 수정: timer 를 한 프레임에 한 번만 증가시키고, 스폰 체크도 한 번만 한다.
        timer += deltaTime

        if (timer >= spawnInterval) {
            timer = 0f
            spawnBomb(player)
        }

        val toRemove = mutableListOf<Bomb>()

        bombs.forEach { bomb ->
            bomb.timer += deltaTime

            if (bomb.timer >= bomb.explodeTime) {
                explode(bomb, enemies)
                container.removeView(bomb.sprite)
                toRemove.add(bomb)
            }
        }

        bombs.removeAll(toRemove)
    }

    private fun spawnBomb(player: PlayerInfo) {
        val bombView = ImageView(context)
        val size = bombSize

        bombView.layoutParams = ConstraintLayout.LayoutParams(size, size)
        bombView.setImageResource(R.drawable.bomb)  // 폭탄 이미지

        val bomb = Bomb(
            x = player.getCenterX() - size / 2f,
            y = player.getCenterY() - size / 2f
        )
        bomb.sprite = bombView
        bomb.sprite.x = bomb.x
        bomb.sprite.y = bomb.y

        container.addView(bombView)
        bombs.add(bomb)
    }

    private fun explode(bomb: Bomb, enemies: List<Enemy>) {
        val explodeRadius = 200f + (level - 1) * 100f
        val damage = 40f + (level - 1) * 15f

        enemies.forEach { enemy ->
            val dx = bomb.getCenterX() - enemy.getCenterX()
            val dy = bomb.getCenterY() - enemy.getCenterY()
            val dist = sqrt(dx * dx + dy * dy)

            if (dist <= explodeRadius + enemy.radius) {
                enemy.takeDamage(damage)
            }
        }

        // 폭발 이펙트 (나중에 추가 가능)
        showExplosionEffect(bomb)
    }

    private fun showExplosionEffect(bomb: Bomb) {
        val explosion = ImageView(context)
        val size = 600

        explosion.layoutParams = ConstraintLayout.LayoutParams(size, size)
        explosion.setImageResource(R.drawable.explosion)  // 폭발 이미지
        explosion.x = bomb.getCenterX() - size / 2f
        explosion.y = bomb.getCenterY() - size / 2f

        container.addView(explosion)

        explosion.animate()
            .alpha(0f)
            .scaleX(1.5f)
            .scaleY(1.5f)
            .setDuration(400)
            .withEndAction {
                container.removeView(explosion)
            }
            .start()
    }

    override fun onLevelUp() {

    }
}