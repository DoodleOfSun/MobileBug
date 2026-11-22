package com.example.prototypebug

import android.content.Context
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

        if (timer >= interval && enemies.isNotEmpty()) {
            timer = 0f
            Log.d("RingDebug", "공격 실행 - level: $level, 대상 수: ${enemies.shuffled().take(level).size}")
            attackRandomEnemies(enemies)
        }
    }

    private fun attackRandomEnemies(enemies: List<Enemy>) {
        val targets = enemies.shuffled().take(level)
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