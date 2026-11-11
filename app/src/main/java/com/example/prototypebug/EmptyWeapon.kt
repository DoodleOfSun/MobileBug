package com.example.prototypebug

class EmptyWeapon(override val type: WeaponType) : Weapon {
    override var level: Int = 1

    override fun update(deltaTime: Float, player: PlayerInfo, enemies: List<Enemy>) {
        // 아무것도 안 함
    }

    override fun onLevelUp() {}
}