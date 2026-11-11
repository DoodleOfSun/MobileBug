package com.example.prototypebug

import android.util.Log

// LaserWeapon.kt
class LaserWeapon(override val type: WeaponType) : Weapon {
    override var level = 1
    private var cooldown = 0f
    override fun update(deltaTime: Float, player: PlayerInfo, enemies: List<Enemy>) {
        cooldown -= deltaTime
        if (cooldown <= 0f) {
            cooldown = 2.0f / level
            Log.d("Weapon", "레이저 발사 Lv.$level")
        }
    }
    override fun onLevelUp() {}
}