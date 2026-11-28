package com.example.prototypebug

import androidx.constraintlayout.widget.ConstraintLayout
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ArrowWeaponInstrumentedTest {
    @Test
    fun arrowHitsNearestLivingEnemyOnceAndRemovesItsView() = onMainThread {
        val game = TestGame()
        val near = game.enemyAt(180f, 200f)
        val far = game.enemyAt(100f, 400f)
        val dead = game.enemyAt(150f, 200f).apply { health = 0f }
        val enemies = listOf(far, dead, near)

        game.advance(0.05f, enemies)
        game.advance(0.4f, enemies)
        game.advance(0.25f, enemies)

        assertEquals(88f, near.health, 0f)
        assertEquals(100f, far.health, 0f)
        assertEquals(0f, dead.health, 0f)
        assertEquals(0, game.arena.childCount)
    }

    @Test
    fun arrowKeepsTrackingItsOriginalTargetAfterItMoves() = onMainThread {
        val game = TestGame()
        val target = game.enemyAt(260f, 200f)
        val other = game.enemyAt(100f, 380f)
        val enemies = listOf(other, target)

        game.advance(0.1f, enemies)
        assertEquals(1, game.arena.childCount)
        target.y += game.px(180f)
        game.advance(0.5f, enemies)
        game.advance(0.3f, enemies)

        assertEquals(88f, target.health, 0f)
        assertEquals(100f, other.health, 0f)
        assertEquals(0, game.arena.childCount)
    }

    @Test
    fun arrowDisappearsWhenItsTargetDiesBeforeImpact() = onMainThread {
        val game = TestGame()
        val target = game.enemyAt(260f, 200f)
        val other = game.enemyAt(100f, 400f)
        val enemies = listOf(other, target)

        game.advance(0.1f, enemies)
        assertEquals(1, game.arena.childCount)
        target.health = 0f
        game.advance(0.1f, enemies)

        assertEquals(0, game.arena.childCount)
        assertEquals(0f, target.health, 0f)
        assertEquals(100f, other.health, 0f)
    }

    private fun onMainThread(test: () -> Unit) {
        var result: Result<Unit>? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            result = runCatching(test)
        }
        checkNotNull(result).getOrThrow()
    }

    private class TestGame {
        private val context = InstrumentationRegistry.getInstrumentation().targetContext
        private val density = context.resources.displayMetrics.density
        val arena = ConstraintLayout(context).apply {
            layout(0, 0, px(600f).toInt(), px(500f).toInt())
        }
        private val manager = WeaponManager(arena, context).apply {
            addOrLevelUp(checkNotNull(WeaponDatabase.getById("arrow")))
        }
        private val player = PlayerInfo().apply {
            x = px(100f)
            y = px(200f)
        }

        fun px(dp: Float) = dp * density

        fun advance(deltaTime: Float, enemies: List<Enemy>) {
            manager.updateAll(deltaTime, player, enemies)
        }

        fun enemyAt(centerX: Float, centerY: Float) = Enemy().apply {
            radius = px(8f)
            x = px(centerX) - radius
            y = px(centerY) - radius
            health = 100f
        }
    }
}
