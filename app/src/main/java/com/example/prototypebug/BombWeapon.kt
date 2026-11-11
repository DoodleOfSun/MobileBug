package com.example.prototypebug

import android.util.Log

class BombWeapon(override val type: WeaponType) : Weapon {
    override var level = 1
    private var cooldown = 0f
    override fun update(deltaTime: Float, player: PlayerInfo, enemies: List<Enemy>) {
        cooldown -= deltaTime
        if (cooldown <= 0f) {
            cooldown = 2.5f / level
            Log.d("Weapon", "폭탄 설치 Lv.$level")
        }
    }
    override fun onLevelUp() {}
}