package com.example.prototypebug

import android.util.Log

class WeaponManager {
    // 현재 보유 중인 무기 (id → level)
    private val ownedWeapons = mutableMapOf<String, Int>()

    fun getRandomChoices(count: Int = 3): List<WeaponType> {
        return WeaponDatabase.all.shuffled().take(count)
    }

    fun addOrLevelUp(type: WeaponType) {
        val currentLevel = ownedWeapons[type.id] ?: 0
        ownedWeapons[type.id] = currentLevel + 1
        Log.d("Weapon", "${type.name} 레벨: ${ownedWeapons[type.id]}")
    }

    fun getLevel(typeId: String): Int = ownedWeapons[typeId] ?: 0

    fun hasWeapon(typeId: String): Boolean = ownedWeapons.containsKey(typeId)
}