package com.example.prototypebug

import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.ExecutionException
import java.util.concurrent.FutureTask

@RunWith(AndroidJUnit4::class)
class DaggerWeaponInstrumentedTest {
    @Test
    fun daggerKeepsItsLaunchHeadingWhenThePlayerTurns() = onMainThread {
        val game = DaggerGame()
        val below = game.enemyAt(100f, 260f)
        val right = game.enemyAt(260f, 100f)
        val enemies = listOf(right, below)

        game.player.playerSprite.rotation = 90f
        game.weapons.updateAll(0.02f, game.player, enemies)
        assertEquals("A dagger should appear after equipping it", 1, game.arena.childCount)
        assertEquals(135f, game.arena.getChildAt(0).rotation, 0.01f)

        game.player.playerSprite.rotation = 0f
        game.weapons.updateAll(0.35f, game.player, enemies)

        assertEquals(21f, below.health, 0f)
        assertEquals(30f, right.health, 0f)
        assertEquals(0, game.arena.childCount)
    }

    @Test
    fun upgradedDaggerDamagesOnlyTheFirstEnemyAlongItsPath() = onMainThread {
        val game = DaggerGame()
        game.weapons.addOrLevelUp(game.dagger)
        val near = game.enemyAt(260f, 100f)
        val far = game.enemyAt(340f, 100f)

        game.weapons.updateAll(0.02f, game.player, listOf(far, near))
        game.weapons.updateAll(0.52f, game.player, listOf(far, near))

        assertEquals(18f, near.health, 0f)
        assertEquals(30f, far.health, 0f)
        assertEquals(0, game.arena.childCount)
    }

    @Test
    fun missedDaggerDisappearsAfterLeavingTheArena() = onMainThread {
        val game = DaggerGame()
        game.player.x = game.dp(470f)

        game.weapons.updateAll(0.02f, game.player, emptyList())
        assertEquals(1, game.arena.childCount)

        game.weapons.updateAll(0.2f, game.player, emptyList())

        assertEquals(0, game.arena.childCount)
    }

    private fun onMainThread(test: () -> Unit) {
        val task = FutureTask(test)
        InstrumentationRegistry.getInstrumentation().runOnMainSync(task)
        try {
            task.get()
        } catch (failure: ExecutionException) {
            throw failure.cause ?: failure
        }
    }

    private class DaggerGame {
        private val context = InstrumentationRegistry.getInstrumentation().targetContext
        val arena = ConstraintLayout(context).apply { layout(0, 0, dp(500f).toInt(), dp(500f).toInt()) }
        val player = PlayerInfo().apply {
            x = dp(100f)
            y = dp(100f)
            playerSprite = View(context)
        }
        val dagger = requireNotNull(WeaponDatabase.getById("dagger"))
        val weapons = WeaponManager(arena, context).apply { addOrLevelUp(dagger) }

        fun enemyAt(centerX: Float, centerY: Float) = Enemy().apply {
            radius = dp(12f)
            x = dp(centerX) - radius
            y = dp(centerY) - radius
            health = 30f
        }

        fun dp(value: Float) = value * context.resources.displayMetrics.density
    }
}
