package com.example.prototypebug

import kotlin.math.max
import kotlin.math.sqrt

/**
 * 플레이어 레벨에 따른 난이도 수치. 밸런스 조정은 여기 숫자만 바꾸면 된다.
 * level = 지금까지 레벨업한 횟수 (게임 시작 시 0)
 */
object GameBalance {

    /** 다음 레벨업까지 필요한 경험치. 보석 1개 = 0.01 → 처음엔 보석 30개, 레벨마다 3개씩 더 필요 */
    fun expToNextLevel(level: Int): Float = 0.30f + 0.03f * level

    /** 맵1 근거리 몬스터 체력 (레벨 0 = 10, 기존과 같음) */
    fun enemyHealth(level: Int): Float = 10f * sqrt(1f + 1.6f * level)

    /** 맵2 원거리 몬스터 체력 (레벨 0 = 15, 기존과 같음. 항상 근거리의 1.5배) */
    fun rangedEnemyHealth(level: Int): Float = 15f * sqrt(1f + 1.6f * level)

    /** 보스 체력 (근거리·원거리 보스 공용). 레벨 5까지는 기존 800, 그 뒤 레벨마다 +180 */
    fun bossHealth(level: Int): Float = 800f + 180f * max(0, level - 5)
}