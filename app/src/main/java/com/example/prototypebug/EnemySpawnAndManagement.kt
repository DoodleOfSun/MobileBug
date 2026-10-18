package com.example.prototypebug

import android.content.Context
import android.util.Log
import android.view.ViewGroup
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class EnemySpawnAndManagement {
    lateinit var container: ViewGroup
    lateinit var context: Context

    private var monsterDamageDelay = 2f

    val enemies = mutableListOf<Enemy>()
    var playerAndEnemyCollisionSecTimer = monsterDamageDelay

    fun spawn(x: Float, y: Float) {
        val enemyView = ImageView(context)

        val enemySizePx = 60

        enemyView.layoutParams = ConstraintLayout.LayoutParams(enemySizePx, enemySizePx)
        enemyView.setImageResource(R.drawable.enemycircle)

        container.addView(enemyView)

        val enemy = Enemy()
        enemy.x = x
        enemy.y = y
        enemy.enemySprite = enemyView

        enemies.add(enemy)
    }

    fun spawnFromEdge() {
        val width = container.width.toFloat()
        val height = container.height.toFloat()
        if (width == 0f || height == 0f) return

        val centerX = width / 2f
        val centerY = height / 2f

        val spawnRadius = sqrt(centerX * centerX + centerY * centerY) + 100f

        val randomAngle = Random.nextDouble(0.0, 2.0 * Math.PI)

        val spawnX = centerX + (spawnRadius * cos(randomAngle)).toFloat()
        val spawnY = centerY + (spawnRadius * sin(randomAngle)).toFloat()

        spawn(spawnX, spawnY)
    }

    fun updateAll(player: PlayerInfo, deltaTime: Float) {
        enemies.forEach { it.update(player.getCenterX(), player.getCenterY()) }

        val slop = 0.5f
        repeat(4) {
            for (i in 0 until enemies.size) {
                for (j in i + 1 until enemies.size) {
                    val a = enemies[i]
                    val b = enemies[j]
                    var dx = b.getCenterX() - a.getCenterX()
                    var dy = b.getCenterY() - a.getCenterY()
                    var dist = sqrt(dx * dx + dy * dy)
                    val minDist = a.radius + b.radius + slop
                    if (dist < minDist) {
                        if (dist <= 0.0001f) {
                            dx = 1f
                            dy = 0f
                            dist = 0.0001f
                        }
                        val overlap = minDist - dist
                        val nx = dx / dist
                        val ny = dy / dist
                        a.x -= nx * overlap * 0.5f
                        a.y -= ny * overlap * 0.5f
                        b.x += nx * overlap * 0.5f
                        b.y += ny * overlap * 0.5f
                        a.enemySprite.x = a.x
                        a.enemySprite.y = a.y
                        b.enemySprite.x = b.x
                        b.enemySprite.y = b.y
                    }
                }
            }

            enemies.forEach { enemy ->
                var dx = player.getCenterX() - enemy.getCenterX()
                var dy = player.getCenterY() - enemy.getCenterY()
                var dist = sqrt(dx * dx + dy * dy)
                val minDist = player.radius + enemy.radius + slop
                if (dist < minDist) {
                    if (dist <= 0.0001f) {
                        dx = 1f
                        dy = 0f
                        dist = 0.0001f
                    }
                    val overlap = minDist - dist
                    val nx = dx / dist
                    val ny = dy / dist
                    enemy.x -= nx * overlap
                    enemy.y -= ny * overlap
                    enemy.enemySprite.x = enemy.x
                    enemy.enemySprite.y = enemy.y
                }
            }
        }

        var isTouchingAnyEnemy = false
        var damageDealer: Enemy? = null
        var minRemain = Float.MAX_VALUE

        enemies.forEach { enemy ->
            val dx = player.getCenterX() - enemy.getCenterX()
            val dy = player.getCenterY() - enemy.getCenterY()
            val dist = sqrt(dx * dx + dy * dy)
            val remain = dist - (player.radius + enemy.radius)

            if (remain < minRemain) minRemain = remain

            if (dist <= player.radius + enemy.radius + slop) {
                isTouchingAnyEnemy = true
                if (damageDealer == null) damageDealer = enemy
            }
        }

        if (isTouchingAnyEnemy) {
//            Log.d("GameDebug", "적과 닿았습니다. 남은거리: $minRemain")
            if (deltaTime > 0f) {
                playerAndEnemyCollisionSecTimer += deltaTime
                if (playerAndEnemyCollisionSecTimer >= monsterDamageDelay) {
                    player.playerGetDamaged(damageDealer!!.attackPower)
                    Log.d("GameDebug", "플레이어가 데미지를 입습니다.")
                    playerAndEnemyCollisionSecTimer = 0f
                }
            }
        } else {
            if (enemies.isEmpty()) {
//                Log.d("GameDebug", "적이 없음")
            } else {
//                Log.d("GameDebug", "적과 안 닿음. 닿기까지 남은 거리: $minRemain")
            }
            playerAndEnemyCollisionSecTimer = monsterDamageDelay
        }

    }

    fun resetAllEnemies(){
        enemies.forEach{ enemy ->
            container.removeView(enemy.enemySprite)
        }
        enemies.clear()
    }

    fun removeEnemy(enemy: Enemy) {
        container.removeView(enemy.enemySprite)
        enemies.remove(enemy)
    }
}