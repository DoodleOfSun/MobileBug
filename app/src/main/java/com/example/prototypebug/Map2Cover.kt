package com.example.prototypebug

import android.graphics.PointF
import android.graphics.RectF
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

internal class Map2Cover {
    var walls: List<RectF> = emptyList()

    fun blocks(x: Float, y: Float, endX: Float, endY: Float, radius: Float): Boolean =
        walls.any { intersects(it, x, y, endX, endY, radius) }

    fun constrainEnemy(enemy: Enemy, previousX: Float, previousY: Float) {
        val radius = enemy.radius
        val position = move(previousX + radius, previousY + radius, enemy.getCenterX(), enemy.getCenterY(), radius)
        enemy.x = position.x - radius
        enemy.y = position.y - radius
        enemy.enemySprite.x = enemy.x
        enemy.enemySprite.y = enemy.y
    }

    // Resolve each axis separately so actors slide along the cover.
    fun move(x: Float, y: Float, endX: Float, endY: Float, radius: Float): PointF {
        var nextX = endX
        var nextY = endY
        for (wall in walls) {
            if (y > wall.top - radius && y < wall.bottom + radius) {
                nextX = clipAxis(x, nextX, wall.left - radius, wall.right + radius)
            }
        }
        for (wall in walls) {
            if (nextX > wall.left - radius && nextX < wall.right + radius) {
                nextY = clipAxis(y, nextY, wall.top - radius, wall.bottom + radius)
            }
        }
        return PointF(nextX, nextY)
    }

    fun detour(x: Float, y: Float, endX: Float, endY: Float, radius: Float): PointF? {
        val wall = walls.firstOrNull { intersects(it, x, y, endX, endY, radius - 0.5f) }
            ?: return null
        val margin = radius + 4f
        val corners = listOf(
            PointF(wall.left - margin, wall.top - margin),
            PointF(wall.right + margin, wall.top - margin),
            PointF(wall.right + margin, wall.bottom + margin),
            PointF(wall.left - margin, wall.bottom + margin)
        )
        var best: PointF? = null
        var bestDistance = Float.POSITIVE_INFINITY
        var goalX = endX
        var goalY = endY
        // The boss can be wider than the player's clearance from a wall.
        if (goalX in corners[0].x..corners[2].x && goalY in corners[0].y..corners[2].y) {
            val edgeX = if (goalX < wall.centerX()) corners[0].x else corners[2].x
            val edgeY = if (goalY < wall.centerY()) corners[0].y else corners[2].y
            if (kotlin.math.abs(goalX - edgeX) < kotlin.math.abs(goalY - edgeY)) goalX = edgeX
            else goalY = edgeY
        }
        for ((index, first) in corners.withIndex()) {
            val approach = hypot(first.x - x, first.y - y)
            if (approach < 1f || blocks(x, y, first.x, first.y, radius - 0.5f)) continue
            for (last in listOf(first, corners[(index + 1) % 4], corners[(index + 3) % 4])) {
                if (intersects(wall, last.x, last.y, goalX, goalY, radius - 0.5f)) continue
                val distance = approach + hypot(last.x - first.x, last.y - first.y) +
                    hypot(goalX - last.x, goalY - last.y)
                if (distance < bestDistance) {
                    best = first
                    bestDistance = distance
                }
            }
        }
        return best
    }

    private fun clipAxis(start: Float, end: Float, low: Float, high: Float): Float = when {
        start <= low && end > low -> low
        start >= high && end < high -> high
        start > low && start < high && end in low..high ->
            if (start - low < high - start) low else high
        else -> end
    }

    private fun intersects(wall: RectF, x: Float, y: Float, endX: Float, endY: Float, radius: Float): Boolean {
        var entry = 0f
        var exit = 1f
        fun clip(start: Float, delta: Float, low: Float, high: Float): Boolean {
            if (delta == 0f) return start in low..high
            val a = (low - start) / delta
            val b = (high - start) / delta
            entry = max(entry, min(a, b))
            exit = min(exit, max(a, b))
            return entry <= exit
        }
        return clip(x, endX - x, wall.left - radius, wall.right + radius) &&
            clip(y, endY - y, wall.top - radius, wall.bottom + radius)
    }
}
