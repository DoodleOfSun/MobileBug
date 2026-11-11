package com.example.prototypebug

import android.util.Log

class WeaponManager {
    private val ownedWeapons = mutableMapOf<String, Weapon>()

    fun getRandomChoices(count: Int = 3): List<WeaponType> {
        return WeaponDatabase.all.shuffled().take(count)
    }

    fun addOrLevelUp(type: WeaponType) {
        val existing = ownedWeapons[type.id]
        if (existing != null) {
            existing.level++
            existing.onLevelUp()
        } else {
            ownedWeapons[type.id] = createWeapon(type)
        }
        Log.d("Weapon", "${type.name} → 레벨 ${ownedWeapons[type.id]?.level}")
    }

    fun updateAll(deltaTime: Float, player: PlayerInfo, enemies: List<Enemy>) {
        ownedWeapons.values.forEach {
            it.update(deltaTime, player, enemies)
        }
    }

    private fun createWeapon(type: WeaponType): Weapon {
        return when (type.id) {
            "arrow"  -> ArrowWeapon(type)
            "dagger" -> DaggerWeapon(type)
            else     -> EmptyWeapon(type)
        }
    }

    fun getLevel(id: String): Int = ownedWeapons[id]?.level ?: 0
}