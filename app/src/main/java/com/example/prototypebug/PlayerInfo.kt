package com.example.prototypebug

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.Window
import android.widget.ImageButton
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

                // 렙업했으니 강화 선택 창을 띄운다
                var levelupXML = cont.layoutInflater.inflate(R.layout.level_up_settings, null)
                val btnChoose1 = levelupXML.findViewById<ImageButton>(R.id.btn_choose1)
                val btnChoose2 = levelupXML.findViewById<ImageButton>(R.id.btn_choose2)
                val btnChoose3 = levelupXML.findViewById<ImageButton>(R.id.btn_choose3)


                val dialog = Dialog(cont)
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
                dialog.setContentView(levelupXML)

                dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                dialog.window?.setLayout(
                    cont.dpToPx(550f),
                    cont.dpToPx(330f)
                )


                btnChoose1.setOnClickListener{


                    cont.setPause(false)
                    dialog.dismiss()

                }

                btnChoose2.setOnClickListener{


                    cont.setPause(false)
                    dialog.dismiss()
                }

                btnChoose3.setOnClickListener {


                    cont.setPause(false)
                    dialog.dismiss()
                }

                dialog.show()

                val content = dialog.window?.decorView ?: return
                content.scaleX = 0.3f
                content.scaleY = 0.3f
                content.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(250)
                    .setInterpolator(android.view.animation.OvershootInterpolator())
                    .start()

            }
        }
    }
}