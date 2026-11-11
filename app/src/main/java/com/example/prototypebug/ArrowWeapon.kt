package com.example.prototypebug

import android.util.Log

class ArrowWeapon(override val type: WeaponType) : Weapon {
    override var level = 1
    private var cooldown = 0f
    private val fireInterval = 1.2f

    override fun update(deltaTime: Float, player: PlayerInfo, enemies: List<Enemy>) {
        cooldown -= deltaTime
        if (cooldown <= 0f && enemies.isNotEmpty()) {
            cooldown = fireInterval
            // TODO: 화살 투사체 스폰
            Log.d("Weapon", "화살 발사! 레벨 $level")
        }
    }

    override fun onLevelUp() {
        Log.d("Weapon", "화살 레벨업 → $level")
    }
}