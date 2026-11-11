package com.example.prototypebug

import android.util.Log

class BlackHoleWeapon(override val type: WeaponType) : Weapon {
    override var level = 1
    private var cooldown = 0f
    override fun update(deltaTime: Float, player: PlayerInfo, enemies: List<Enemy>) {
        cooldown -= deltaTime
        if (cooldown <= 0f) {
            cooldown = 8f / level
            Log.d("Weapon", "블랙홀 생성 Lv.$level")
        }
    }
    override fun onLevelUp() {}
}