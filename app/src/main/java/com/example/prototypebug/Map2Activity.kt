package com.example.prototypebug

import android.graphics.RectF
import android.graphics.drawable.Drawable
import kotlin.math.roundToInt

class Map2Activity : BaseGameActivity() {
    override val battleMusicResource = R.raw.battle_bgm_map2

    private val cover = Map2Cover()
    private val coverDrawables = mutableListOf<Drawable>()

    override fun getLayoutId() = R.layout.activity_map2

    override fun createEnemySpawner() = EnemySpawnAndManagement().apply {
        map2Cover = cover
    }

    override fun initEntireSystem() {
        container.addOnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            if (right - left != oldRight - oldLeft || bottom - top != oldBottom - oldTop) layoutCover()
        }
        super.initEntireSystem()
    }

    private fun layoutCover() {
        coverDrawables.forEach { container.overlay.remove(it) }
        coverDrawables.clear()
        val width = container.width.toFloat()
        val height = container.height.toFloat()
        // Leave a full actor diameter between each wall and the arena edge.
        val lane = maxOf(playerInfo.radius, 60f) * 2f + dpToPx(8f)
        val halfWidth = minOf(width * 0.075f, width * 0.25f - lane, (width * 0.50f - lane) / 2f)
        val halfHeight = minOf(height * 0.15f, height * 0.43f - lane)
        cover.walls = if (halfWidth < 6f || halfHeight < 6f) emptyList() else listOf(
            coverRect(width * 0.25f, height * 0.43f, halfWidth, halfHeight),
            coverRect(width * 0.75f, height * 0.57f, halfWidth, halfHeight)
        )
        for (wall in cover.walls) {
            val drawable = checkNotNull(getDrawable(R.drawable.map2_cover)).mutate()
            drawable.setBounds(wall.left.toInt(), wall.top.toInt(), wall.right.toInt(), wall.bottom.toInt())
            container.overlay.add(drawable)
            coverDrawables.add(drawable)
        }
    }

    private fun coverRect(x: Float, y: Float, halfWidth: Float, halfHeight: Float) = RectF(
        (x - halfWidth).roundToInt().toFloat(), (y - halfHeight).roundToInt().toFloat(),
        (x + halfWidth).roundToInt().toFloat(), (y + halfHeight).roundToInt().toFloat()
    )

    override fun constrainPlayerMovement(previousX: Float, previousY: Float) {
        val radius = playerInfo.radius
        val centerX = playerInfo.getCenterX()
        val centerY = playerInfo.getCenterY()
        val offsetX = centerX - playerInfo.x
        val offsetY = centerY - playerInfo.y
        val position = cover.move(previousX + offsetX, previousY + offsetY, centerX, centerY, radius)
        if (position.x != centerX) velX = 0f
        if (position.y != centerY) velY = 0f
        playerInfo.x = position.x - offsetX
        playerInfo.y = position.y - offsetY
    }
}
