package com.example.prototypebug

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.Window
import android.widget.ImageButton
import android.widget.TextView

class PlayerInfo internal constructor(private val playSound: (GameSound) -> Unit) {
    constructor() : this({})

    var x = 0f
    var y = 0f

    var attackPower = 1
    var health = 1500
    var maxHealth = 1500

    lateinit var playerSprite: View

    var playerExp : Float = 0f

    var playerExpScale = 0.3f

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

    // 수정: 한 프레임에 보석을 여러 개 먹어 레벨업이 연달아 일어나면 선택창이 겹쳐 뜨고,
    // 앞 창을 닫는 순간 뒤 창이 열려 있는데도 게임이 재개되던 문제 → 대기열로 하나씩 띄운다.
    private var pendingLevelUps = 0
    private var isLevelUpDialogShowing = false

    fun playerGetExp(exp: Float, cont: Context) {
        playerExp += exp
        if (exp > 0f) {playSound(GameSound.EXP_PICKUP)}
        // 수정: 넘친 경험치를 버리지 않고 이월
        // (0.01 을 30번 더해도 float 오차로 0.2999.. 가 될 수 있어 작은 여유값을 둠)
        while (playerExp >= playerExpScale - 0.0001f) {
            playerExp -= playerExpScale
            pendingLevelUps++
        }
        showNextLevelUp(cont)
    }

    private fun showNextLevelUp(cont: Context) {
        if (isLevelUpDialogShowing || pendingLevelUps <= 0) return
        pendingLevelUps--
        run {
            playSound(GameSound.LEVEL_UP)

            if (cont is BaseGameActivity) {
                isLevelUpDialogShowing = true
                cont.setPause(true)

                val levelupXML = cont.layoutInflater.inflate(R.layout.level_up_settings, null)
                val dialog = Dialog(cont)
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
                dialog.setContentView(levelupXML)
                dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                dialog.window?.setLayout(
                    cont.dpToPx(550f),
                    cont.dpToPx(330f)
                )
                dialog.setCancelable(false)

                val choices = cont.weaponManager.getRandomChoices(3)

                val text1 = levelupXML.findViewById<TextView>(R.id.text_choose1)
                val btn1  = levelupXML.findViewById<ImageButton>(R.id.btn_choose1)
                val text2 = levelupXML.findViewById<TextView>(R.id.text_choose2)
                val btn2  = levelupXML.findViewById<ImageButton>(R.id.btn_choose2)
                val text3 = levelupXML.findViewById<TextView>(R.id.text_choose3)
                val btn3  = levelupXML.findViewById<ImageButton>(R.id.btn_choose3)

                // 바인딩
                text1.text = choices[0].name
                btn1.setImageResource(choices[0].iconRes)

                text2.text = choices[1].name
                btn2.setImageResource(choices[1].iconRes)

                text3.text = choices[2].name
                btn3.setImageResource(choices[2].iconRes)

                // 선택 처리: 대기 중인 레벨업이 남아 있으면 일시정지를 유지한 채 다음 창을 띄운다
                fun choose(index: Int) {
                    cont.weaponManager.addOrLevelUp(choices[index])
                    dialog.dismiss()
                    isLevelUpDialogShowing = false
                    if (pendingLevelUps > 0) showNextLevelUp(cont) else cont.setPause(false)
                }
                btn1.setGameClickListener(playSound) { choose(0) }
                btn2.setGameClickListener(playSound) { choose(1) }
                btn3.setGameClickListener(playSound) { choose(2) }

                dialog.show()

                // 연출
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