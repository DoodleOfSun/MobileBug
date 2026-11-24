package com.example.prototypebug

import android.util.Log
import androidx.constraintlayout.widget.ConstraintLayout

class WeaponManager(
    private val container: ConstraintLayout,
    private val context: android.content.Context){
    private val ownedWeapons = mutableMapOf<String, Weapon>()


    fun getRandomChoices(count: Int = 3): List<WeaponType> {
        return WeaponDatabase.all.shuffled().take(count)
    }

    fun getSpecificChoices(): List<WeaponType> {
        return listOf(
            WeaponDatabase.getById("ring")!!,
            WeaponDatabase.getById("bomb")!!,
            WeaponDatabase.getById("bone")!!
        )
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
            "bone" -> BoneWeapon(type, container, context)
            "ring" -> RingWeapon(type, container, context)
            "bomb" -> BombWeapon(type, container, context)
            else     -> EmptyWeapon(type)
        }
    }

    fun getLevel(id: String): Int = ownedWeapons[id]?.level ?: 0
}