package com.example.prototypebug

import android.util.Log

class JewelWeapon(override val type: WeaponType) : Weapon {
    override var level = 1
    private var cooldown = 0f
    override fun update(deltaTime: Float, player: PlayerInfo, enemies: List<Enemy>) {
        cooldown -= deltaTime
        if (cooldown <= 0f) {
            cooldown = 1.0f / level
            Log.d("Weapon", "보석 발사 Lv.$level")
        }
    }
    override fun onLevelUp() {}
}