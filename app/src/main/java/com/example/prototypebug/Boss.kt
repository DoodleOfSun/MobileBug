package com.example.prototypebug

class Boss : Enemy() {
    init {
        moveSpeed = 0.35f     // 매우 느림
        health = 800f
        maxHealth = 800f
        attackPower = 8
        radius = 60f
    }
}