package com.example.prototypebug

import android.content.Context
import android.view.View
import kotlin.math.sqrt

class PlayerInfo {
    var x = 0f
    var y = 0f

    var attackPower = 1
    var health = 1500
    var maxHealth = 1500

    lateinit var playerSprite: View

    var playerExp : Float = 0f

    fun playerReset(){
        x = 0f
        y = 0f
        attackPower = 1
        health = maxHealth
    }

    val radius: Float
        get() = if (::playerSprite.isInitialized) {
            playerSprite.width.coerceAtLeast(playerSprite.height) / 2f
        } else {
            0f
        }

    fun getCenterX(): Float {
        return if (::playerSprite.isInitialized) x + playerSprite.width / 2f else x
    }

    fun getCenterY(): Float {
        return if (::playerSprite.isInitialized) y + playerSprite.height / 2f else y
    }

    fun playerGetDamaged(damageData : Int){
        if (checkPlayerDied()) {
            // 이미 죽은 상태. 추가 데미지 없음
            return
        }

        health -= damageData

        if (health < 0){
            health = 0
        }

        if (checkPlayerDied()) {
            // 이번에 죽음.
        }
    }

    fun checkPlayerDied() : Boolean{
        // 플레이어 사망
        if(health <= 0){
            return true;
        }

        // 플레이어 아직 죽지 않았다
        else{
            return false;
        }
    }

    fun playerGetExp(exp : Float, cont : Context){
        playerExp += exp
        if(playerExp >= 1){
            playerExp = 0f

            if(cont is MainActivity){
                cont.setPause(true)

                // 렙업했으니 강화 선택
            }
        }
    }
}