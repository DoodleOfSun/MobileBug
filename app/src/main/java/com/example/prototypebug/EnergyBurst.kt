package com.example.prototypebug

import android.animation.ValueAnimator
import android.view.ViewGroup
import androidx.core.animation.doOnEnd
import kotlin.math.roundToInt

internal fun ViewGroup.showEnergyBurst(centerX: Float, centerY: Float) {
    if (!isAttachedToWindow) return
    val burst = context.getDrawable(R.drawable.map1_normal_attack_burst)?.mutate() ?: return
    val burstOverlay = overlay
    val halfWidth = burst.intrinsicWidth / 2f
    val halfHeight = burst.intrinsicHeight / 2f

    val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 230L
        addUpdateListener {
            val progress = it.animatedValue as Float
            val scale = 0.45f + 0.65f * progress
            burst.setBounds(
                (centerX - halfWidth * scale).roundToInt(),
                (centerY - halfHeight * scale).roundToInt(),
                (centerX + halfWidth * scale).roundToInt(),
                (centerY + halfHeight * scale).roundToInt()
            )
            burst.alpha = (255f * (1f - progress)).roundToInt()
        }
        doOnEnd { burstOverlay.remove(burst) }
    }
    burstOverlay.add(burst)
    animator.start()
}
