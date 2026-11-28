package com.example.prototypebug

import androidx.constraintlayout.widget.ConstraintLayout
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WeaponChoicesInstrumentedTest {
    @Test
    fun levelUpOffersDistinctImplementedWeapons() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val context = instrumentation.targetContext
            val manager = WeaponManager(ConstraintLayout(context), context)
            val allChoices = manager.getRandomChoices(Int.MAX_VALUE).map { it.id }.toSet()
            assertTrue(allChoices.containsAll(listOf("ring", "bomb", "bone")))
            val unimplemented = setOf("book", "sickle", "missile", "laser", "boomerang", "blackhole")
            assertEquals(emptySet<String>(), allChoices.intersect(unimplemented))
            assertEquals(3, manager.getRandomChoices().map { it.id }.toSet().size)
        }
    }
}
