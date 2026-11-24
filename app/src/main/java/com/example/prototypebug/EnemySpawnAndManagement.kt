package com.example.prototypebug

import android.content.Context
import android.util.Log
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class EnemySpawnAndManagement(
    @param:DrawableRes private val enemyDrawable: Int = R.drawable.enemycircle,
    @param:DrawableRes private val bossDrawable: Int = R.drawable.enemycircle
) {
    lateinit var container: ViewGroup
    lateinit var context: Context

    private var monsterDamageDelay = 2f

    val enemies = mutableListOf<Enemy>()
    val bullets = mutableListOf<EnemyBullet>()
    var playerAndEnemyCollisionSecTimer = monsterDamageDelay

    fun spawn(x: Float, y: Float) {
        val enemyView = ImageView(context)

        val enemySizePx = 60

        enemyView.layoutParams = ConstraintLayout.LayoutParams(enemySizePx, enemySizePx)
        enemyView.setImageResource(enemyDrawable)

        container.addView(enemyView)

        val enemy = Enemy()
        enemy.x = x
        enemy.y = y
        enemy.enemySprite = enemyView

        enemies.add(enemy)
    }

    fun spawnBoss() {
        val width = container.width.toFloat()
        val height = container.height.toFloat()
        if (width == 0f || height == 0f) return

        val centerX = width / 2f
        val centerY = height / 2f
        val spawnRadius = sqrt(centerX * centerX + centerY * centerY) + 150f
        val randomAngle = Random.nextDouble(0.0, 2.0 * Math.PI)

        val spawnX = centerX + (spawnRadius * cos(randomAngle)).toFloat()
        val spawnY = centerY + (spawnRadius * sin(randomAngle)).toFloat()

        val enemyView = ImageView(context)
        val bossSizePx = 120

        enemyView.layoutParams = ConstraintLayout.LayoutParams(bossSizePx, bossSizePx)
        enemyView.setImageResource(bossDrawable)
        container.addView(enemyView)

        val boss = Boss()
        boss.radius = bossSizePx / 2f
        boss.x = spawnX
        boss.y = spawnY
        boss.enemySprite = enemyView

        enemies.add(boss)
        Log.d("GameDebug", "보스 스폰!")
    }

    // ── 원거리 보스 1마리 등장 (Map2용, spawnBoss를 복사해서 종류만 바꿈) ──
    fun spawnRangedBoss() {
        val width = container.width.toFloat()
        val height = container.height.toFloat()
        if (width == 0f || height == 0f) return

        val centerX = width / 2f
        val centerY = height / 2f
        val spawnRadius = sqrt(centerX * centerX + centerY * centerY) + 150f
        val randomAngle = Random.nextDouble(0.0, 2.0 * Math.PI)

        val spawnX = centerX + (spawnRadius * cos(randomAngle)).toFloat()
        val spawnY = centerY + (spawnRadius * sin(randomAngle)).toFloat()

        val enemyView = ImageView(context)
        val bossSizePx = 120

        enemyView.layoutParams = ConstraintLayout.LayoutParams(bossSizePx, bossSizePx)
        enemyView.setImageResource(R.drawable.round_grey_detailed_red)   // 임시 그림 (원거리 몬스터와 같은 그림, 크기만 2배)
        container.addView(enemyView)

        val boss = RangedBoss()
        boss.radius = bossSizePx / 2f
        boss.x = spawnX
        boss.y = spawnY
        boss.screenWidth = width
        boss.screenHeight = height
        boss.enemySprite = enemyView

        enemies.add(boss)
        Log.d("GameDebug", "원거리 보스 스폰!")
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

    // ── 원거리 몬스터 1마리 등장 ──
    fun spawnRangedFromEdge() {
        val width = container.width.toFloat()
        val height = container.height.toFloat()
        if (width == 0f || height == 0f) return

        val centerX = width / 2f
        val centerY = height / 2f
        val spawnRadius = sqrt(centerX * centerX + centerY * centerY) + 100f
        val angle = Random.nextDouble(0.0, 2.0 * Math.PI)

        val view = ImageView(context)
        view.layoutParams = ConstraintLayout.LayoutParams(60, 60)
        view.setImageResource(R.drawable.round_grey_detailed_red)   // 임시 그림
        container.addView(view)

        val ranged = RangedEnemy()
        ranged.radius = 30f
        ranged.x = centerX + (spawnRadius * cos(angle)).toFloat()
        ranged.y = centerY + (spawnRadius * sin(angle)).toFloat()
        ranged.screenWidth = width
        ranged.screenHeight = height
        ranged.enemySprite = view
        enemies.add(ranged)
    }

    // ── 탄 발사 ──
    private fun fireBullet(shooter: RangedEnemy, player: PlayerInfo) {
        val startX = shooter.getCenterX()
        val startY = shooter.getCenterY()
        val dx = player.getCenterX() - startX
        val dy = player.getCenterY() - startY
        val dist = sqrt(dx * dx + dy * dy)
        if (dist < 1f) return

        val view = ImageView(context)
        view.layoutParams = ConstraintLayout.LayoutParams(shooter.bulletSize, shooter.bulletSize)
        view.setBackgroundResource(R.drawable.enemy_bullet)
        view.background.mutate().setTint(shooter.bulletColor)   // 이 탄만 따로 색칠
        container.addView(view)

        val bullet = EnemyBullet(
            view, startX, startY,
            dx / dist * shooter.bulletSpeed,
            dy / dist * shooter.bulletSpeed,
            shooter.bulletDamage,
            shooter.bulletSize / 2f
        )
        bullet.update(0f)
        bullets.add(bullet)
    }

    // ── 탄 관리: 이동 → 맞았나 → 지울까 ──
    private fun updateBullets(player: PlayerInfo, deltaTime: Float) {
        val width = container.width.toFloat()
        val height = container.height.toFloat()

        val iterator = bullets.iterator()
        while (iterator.hasNext()) {
            val bullet = iterator.next()
            bullet.update(deltaTime)

            val dx = player.getCenterX() - bullet.x
            val dy = player.getCenterY() - bullet.y
            val hit = sqrt(dx * dx + dy * dy) < player.radius + bullet.radius
            val out = bullet.x < -50f || bullet.x > width + 50f ||
                      bullet.y < -50f || bullet.y > height + 50f

            if (hit) player.playerGetDamaged(bullet.damage)

            if (hit || out || bullet.life <= 0f) {
                container.removeView(bullet.sprite)
                iterator.remove()
            }
        }
    }

    fun updateAll(player: PlayerInfo, deltaTime: Float) {

        // 플레이어를 추적
        // ====================================================================================

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

        // ====================================================================================



        // 충돌 처리
        // ====================================================================================

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

        // ====================================================================================

        // 원거리 몬스터 사격 + 탄 처리
        enemies.forEach { enemy ->
            if (enemy is RangedEnemy) {
                enemy.fireTimer += deltaTime
                if (enemy.fireTimer >= enemy.fireInterval &&
                    enemy.isOnScreen() && enemy.isInRange(player)) {
                    enemy.fireTimer = 0f
                    fireBullet(enemy, player)
                }
            }
        }
        updateBullets(player, deltaTime)
    }

    fun resetAllEnemies(){
        enemies.forEach{ enemy ->
            container.removeView(enemy.enemySprite)
        }
        enemies.clear()
        bullets.forEach { container.removeView(it.sprite) }
        bullets.clear()
    }

    fun removeEnemy(enemy: Enemy) {
        container.removeView(enemy.enemySprite)
        enemies.remove(enemy)
    }
}
