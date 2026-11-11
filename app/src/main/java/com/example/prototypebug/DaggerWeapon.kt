package com.example.prototypebug

import android.util.Log

class DaggerWeapon(override val type: WeaponType) : Weapon {
    override var level = 1
    private var cooldown = 0f

    override fun update(deltaTime: Float, player: PlayerInfo, enemies: List<Enemy>) {
        cooldown -= deltaTime
        if (cooldown <= 0f) {
            cooldown = 0.8f
            Log.d("Weapon", "단검 발사 (Lv.$level)")
        }
    }

    override fun onLevelUp() {}
}