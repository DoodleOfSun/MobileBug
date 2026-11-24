package com.example.prototypebug

import android.graphics.Color

// 원거리 보스: 원거리 몬스터(RangedEnemy)를 물려받아 숫자만 보스급으로 키움
// → RangedEnemy의 "멈추기 / 사격" 기능을 그대로 사용
class RangedBoss : RangedEnemy() {
    init {
        moveSpeed = 0.8f        // 기존 보스(0.35)보다 빠르게 → 화면에 빨리 들어옴
        health = 800f           // 기존 보스와 같음
        maxHealth = 800f
        attackPower = 8         // 몸에 닿았을 때 피해 (기존 보스와 같음)
        radius = 60f            // 크기 120px

        attackRange = 950f      // 멀리서 쏨 (졸개들이 만든 벽 바깥에서도 닿게)
        fireInterval = 5f       // 5초마다 발사 (일반보다 느리게)
        fireTimer = 1.5f        // 등장 후 2초 뒤 첫 발사
        bulletDamage = 15       // 한 발이 셈 (일반 원거리 탄 2)
        bulletSize = 48         // 탄 크기 2배 (일반 24)
        bulletSpeed = 300f      // 크고 느린 탄 (일반 420)
        bulletColor = Color.parseColor("#8E24AA")   // 보라색 탄
    }
}
