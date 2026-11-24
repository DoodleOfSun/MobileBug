package com.example.prototypebug

import android.util.Log
import androidx.constraintlayout.widget.ConstraintLayout

class WeaponManager(
    private val container: ConstraintLayout,
    private val context: android.content.Context){
    private val ownedWeapons = mutableMapOf<String, Weapon>()


    fun getRandomChoices(count: Int = 3): List<WeaponType> {
        return WeaponDatabase.all.filter { it.create != null }.shuffled().take(count)
    }

    fun addOrLevelUp(type: WeaponType) {
        val existing = ownedWeapons[type.id]
        if (existing != null) {
            existing.level++
            existing.onLevelUp()
        } else {
            ownedWeapons[type.id] = type.create?.invoke(type, container, context) ?: EmptyWeapon(type)
        }
        Log.d("Weapon", "${type.name} → 레벨 ${ownedWeapons[type.id]?.level}")
    }

    fun updateAll(deltaTime: Float, player: PlayerInfo, enemies: List<Enemy>) {
        ownedWeapons.values.forEach {
            it.update(deltaTime, player, enemies)
        }
    }

    fun getLevel(id: String): Int = ownedWeapons[id]?.level ?: 0
}
