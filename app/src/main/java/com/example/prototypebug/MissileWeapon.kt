package com.example.prototypebug

import android.util.Log

// MissileWeapon.kt
class MissileWeapon(override val type: WeaponType) : Weapon {
    override var level = 1
    private var cooldown = 0f
    override fun update(deltaTime: Float, player: PlayerInfo, enemies: List<Enemy>) {
        cooldown -= deltaTime
        if (cooldown <= 0f) {
            cooldown = 3.5f / level
            Log.d("Weapon", "미사일 호출 Lv.$level")
        }
    }
    override fun onLevelUp() {}
}