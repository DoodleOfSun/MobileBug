package com.example.prototypebug

class BoneWeapon(override val type: WeaponType) : Weapon {
    override var level = 1
    override fun update(deltaTime: Float, player: PlayerInfo, enemies: List<Enemy>) {
        // 뼈는 한 번 소환되면 계속 존재 (나중에 구현)
    }
    override fun onLevelUp() {}
}