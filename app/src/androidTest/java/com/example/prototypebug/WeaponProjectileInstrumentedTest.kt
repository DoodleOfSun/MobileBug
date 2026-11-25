package com.example.prototypebug

import android.graphics.Color
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WeaponProjectileInstrumentedTest {
    @Test
    fun longFrameHitsFirstLivingEnemyAndRemovesItsView() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val arena = ConstraintLayout(instrumentation.targetContext)
            arena.layout(0, 0, 1000, 500)
            val near = Enemy().apply { x = 200f; y = 220f }
            val far = Enemy().apply { x = 500f; y = 220f }
            val dead = Enemy().apply { x = 125f; y = 220f; health = 0f }
            val projectile = WeaponProjectile(arena, R.drawable.arrow, Color.CYAN, 100f, 250f, 18f, 4f)

            assertTrue(projectile.advance(700f, 250f, 0f, listOf(far, dead, near)))
            assertEquals(6f, near.health, 0f)
            assertEquals(10f, far.health, 0f)
            assertEquals(0f, dead.health, 0f)
            assertEquals(1, arena.childCount)
            projectile.remove()
            assertEquals(0, arena.childCount)
        }
    }

    @Test
    fun aProjectileCanBeKeptAboveTheArenaUntilItFallsBack() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val arena = ConstraintLayout(instrumentation.targetContext)
            arena.layout(0, 0, 1000, 500)
            val projectile = WeaponProjectile(arena, R.drawable.axe, Color.YELLOW, 400f, 20f, 20f, 8f)

            assertFalse(projectile.advance(420f, -100f, 0f, emptyList()))
            assertTrue(projectile.isOutsideArena())
            assertFalse(projectile.isOutsideArena(allowAboveArena = true))
            assertFalse(projectile.advance(440f, 600f, 0f, emptyList()))
            assertTrue(projectile.isOutsideArena(allowAboveArena = true))
            projectile.remove()
        }
    }
}
