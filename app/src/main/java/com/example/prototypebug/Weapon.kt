package com.example.prototypebug

interface Weapon {
    val type: WeaponType
    var level: Int
    fun update(deltaTime: Float, player: PlayerInfo, enemies: List<Enemy>)
    fun onLevelUp()
}

