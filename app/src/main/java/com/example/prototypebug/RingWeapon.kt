package com.example.prototypebug

import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.ViewGroup
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout

class RingWeapon(
    override val type: WeaponType,
    private val container: ConstraintLayout,
    private val context: Context
) : Weapon {
    override var level = 1

    private var timer = 0f
    private val interval = 3f
    private val damage = 50f
    private val lightningSize = 300

    override fun update(deltaTime: Float, player: PlayerInfo, enemies: List<Enemy>) {
        timer += deltaTime

        // 수정: 이미 죽은(health <= 0) 적은 대상에서 제외
        if (timer >= interval) {
            val alive = enemies.filter { it.health > 0f }
            if (alive.isNotEmpty()) {
                timer = 0f
                attackRandomEnemies(alive)
            }
        }
    }

    private fun attackRandomEnemies(enemies: List<Enemy>) {
        val targets = enemies.shuffled().take(level)
        Log.d("RingDebug", "공격 실행 - level: $level, 대상 수: ${targets.size}")
        targets.forEach { target ->
            target.takeDamage(damage)
            showLightningEffect(target)
        }
    }

    private fun showLightningEffect(enemy: Enemy) {
        val lightning = ImageView(context)
        val size = lightningSize

        lightning.layoutParams = ConstraintLayout.LayoutParams(size, size)
        lightning.setImageResource(R.drawable.lightningbolt)
        lightning.setColorFilter(Color.rgb(255, 220, 40))  // 노란색
        lightning.alpha = 1f

        // 적의 머리 바로 위에 배치
        lightning.x = enemy.getCenterX() - size / 2f
        lightning.y = enemy.y - size + 10f  // 적 위쪽에 살짝 겹치게

        container.addView(lightning)

        // 서서히 사라지는 애니메이션
        lightning.animate()
            .alpha(0f)
            .setDuration(400)
            .withEndAction {
                container.removeView(lightning)
            }
            .start()
    }

    override fun onLevelUp() {

    }
}