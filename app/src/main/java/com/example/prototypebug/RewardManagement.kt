package com.example.prototypebug

import android.content.Context
import android.util.Log
import android.view.ViewGroup
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import kotlin.math.sqrt

class RewardManagement {
    lateinit var container : ViewGroup

    lateinit var context : Context

    val rewards = mutableListOf<Reward>()

    fun spawn(x : Float, y : Float){
        val rewardView = ImageView(context)

        val rewardSizepx = 45
        val radius = 22.5f

        rewardView.layoutParams = ConstraintLayout.LayoutParams(rewardSizepx, rewardSizepx )
        rewardView.setImageResource(R.drawable.rewardjewel)

        container.addView(rewardView)

        val reward = Reward()

        reward.rewardSprite = rewardView
        reward.rewardSprite.x = x - radius
        reward.rewardSprite.y = y - radius

        rewards.add(reward)
    }

    fun destroy(reward : Reward){
        container.removeView(reward.rewardSprite)
        rewards.remove(reward)
    }


    fun update(player : PlayerInfo, deltaTime : Float){
        var rewardGiver : Reward? = null
        var minRemain = Float.MAX_VALUE
        var slop = 0.5f
        val toRemove = mutableListOf<Reward>()

        rewards.forEach { reward ->
            val dx = player.getCenterX() - reward.getCenterX()
            val dy = player.getCenterY() - reward.getCenterY()
            val dist = sqrt(dx*dx + dy * dy)
            val remain = dist - (player.radius + reward.radius)

            if(remain < minRemain) minRemain = remain

            if(dist <= player.radius + reward.radius + slop){
                if(rewardGiver == null) rewardGiver = reward
                toRemove.add(reward)

            }
        }

        toRemove.forEach { reward ->
            player.playerGetExp(reward.exp)
            destroy(reward)

        }

    }

}