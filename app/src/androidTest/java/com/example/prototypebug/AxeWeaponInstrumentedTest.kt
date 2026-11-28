package com.example.prototypebug

import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AxeWeaponInstrumentedTest {
    @Test
    fun axeRisesAboveArenaThenFallsBackAndLeavesAtBottom() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val context = instrumentation.targetContext
            val density = context.resources.displayMetrics.density
            val arena = ConstraintLayout(context).apply {
                layout(0, 0, (500f * density).toInt(), (300f * density).toInt())
            }
            val player = PlayerInfo().apply {
                x = 250f * density
                y = 35f * density
                playerSprite = View(context).apply { rotation = 180f }
            }
            val manager = WeaponManager(arena, context)
            manager.addOrLevelUp(requireNotNull(WeaponDatabase.getById("axe")))

            manager.updateAll(0f, player, emptyList())
            assertEquals(1, arena.childCount)
            val axe = arena.getChildAt(0)
            val launchX = axe.x
            val launchY = axe.y
            val launchRotation = axe.rotation

            manager.updateAll(0.4f, player, emptyList())
            assertEquals(1, arena.childCount)
            assertTrue(axe.y + axe.layoutParams.height < 0f)
            assertTrue(axe.x < launchX)
            assertTrue(axe.rotation != launchRotation)

            manager.updateAll(0.5f, player, emptyList())
            assertEquals(1, arena.childCount)
            assertTrue(axe.y > launchY)
            assertTrue(axe.y + axe.layoutParams.height < arena.height)

            manager.updateAll(0.6f, player, emptyList())
            assertEquals(0, arena.childCount)
        }
    }

    @Test
    fun axeDamagesAnEnemyInItsArcAndDisappears() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val context = instrumentation.targetContext
            val density = context.resources.displayMetrics.density
            val arena = ConstraintLayout(context).apply {
                layout(0, 0, (500f * density).toInt(), (300f * density).toInt())
            }
            val player = PlayerInfo().apply {
                x = 250f * density
                y = 200f * density
                playerSprite = View(context)
            }
            val enemy = Enemy().apply {
                x = 253f * density
                y = 147f * density
                radius = 8f * density
                health = 50f
            }
            val manager = WeaponManager(arena, context)
            manager.addOrLevelUp(requireNotNull(WeaponDatabase.getById("axe")))

            manager.updateAll(0f, player, listOf(enemy))
            assertEquals(1, arena.childCount)
            manager.updateAll(0.25f, player, listOf(enemy))
            assertEquals(28f, enemy.health, 0f)
            assertEquals(0, arena.childCount)

            manager.updateAll(0.25f, player, listOf(enemy))
            assertEquals(28f, enemy.health, 0f)
        }
    }
}
