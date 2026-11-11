package com.example.prototypebug

import android.util.Log

class RingWeapon(override val type: WeaponType) : Weapon {
    override var level = 1
    private var cooldown = 0f
    override fun update(deltaTime: Float, player: PlayerInfo, enemies: List<Enemy>) {
        cooldown -= deltaTime
        if (cooldown <= 0f && enemies.isNotEmpty()) {
            cooldown = 4f / level
            Log.d("Weapon", "반지 즉사 Lv.$level")
        }
    }
    override fun onLevelUp() {}
}