package com.example.prototypebug

import android.content.Context
import android.graphics.Color
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class BoneWeapon(
    override val type: WeaponType,
    private val container: ConstraintLayout,
    private val context: Context
) : Weapon {
    override var level = 1

    private val bones = mutableListOf<Bone>()
    private var spawnTimer = 0f
    private val spawnDelay = 1.2f

    private val damage get() = 30f + 6f * (level - 1)  // 하향: 100 고정 → 30 + 6×(Lv-1)

    // 수정: 같은 적은 뼈 하나당 이 시간(초)에 한 번만 맞는다.
    private val hitCooldown = 0.5f

    private val boneSize = 120

    override fun update(deltaTime: Float, player: PlayerInfo, enemies: List<Enemy>) {
        spawnTimer += deltaTime
        if (spawnTimer >= spawnDelay && bones.size < level) {
            spawnBone(player)
            spawnTimer = 0f
        }

        bones.forEach { bone ->
            bone.x += bone.velX * deltaTime
            bone.y += bone.velY * deltaTime

            val maxX = container.width - bone.size
            val maxY = container.height - bone.size

            if (bone.x < 0f) { bone.x = 0f; bone.velX = -bone.velX }
            if (bone.y < 0f) { bone.y = 0f; bone.velY = -bone.velY }
            if (bone.x > maxX) { bone.x = maxX; bone.velX = -bone.velX }
            if (bone.y > maxY) { bone.y = maxY; bone.velY = -bone.velY }

            // 회전
            bone.rotation += bone.rotationSpeed * deltaTime
            bone.sprite.rotation = bone.rotation

            bone.sprite.x = bone.x
            bone.sprite.y = bone.y

            // 쿨타임 감소 + 끝났거나 사라진 적은 정리
            val iterator = bone.hitCooldowns.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                entry.setValue(entry.value - deltaTime)
                if (entry.value <= 0f || entry.key !in enemies) iterator.remove()
            }

            enemies.forEach { enemy ->
                if (enemy.health > 0f &&
                    enemy !in bone.hitCooldowns &&
                    checkCollision(bone, enemy)
                ) {
                    enemy.takeDamage(damage)
                    bone.hitCooldowns[enemy] = hitCooldown
                }
            }
        }
    }

    private fun spawnBone(player: PlayerInfo) {
        val boneView = ImageView(context)
        val size = boneSize
        boneView.layoutParams = ConstraintLayout.LayoutParams(size, size)
        boneView.setImageResource(R.drawable.bone)
        boneView.setColorFilter(Color.WHITE)  // 하얀색
        container.addView(boneView)

        val bone = Bone()
        bone.sprite = boneView
        bone.x = player.getCenterX() - bone.radius
        bone.y = player.getCenterY() - bone.radius

        val angle = (Math.random() * Math.PI * 2).toFloat()
        val speed = 180f
        bone.velX = cos(angle) * speed
        bone.velY = sin(angle) * speed

        bones.add(bone)
    }

    private fun checkCollision(bone: Bone, enemy: Enemy): Boolean {
        val dx = bone.getCenterX() - enemy.getCenterX()
        val dy = bone.getCenterY() - enemy.getCenterY()
        return sqrt(dx * dx + dy * dy) <= bone.radius + enemy.radius
    }

    override fun onLevelUp() {

    }


    class Bone {
        var x = 0f
        var y = 0f
        var velX = 0f
        var velY = 0f
        // 수정: 스프라이트 크기(boneSize = 120px)와 맞춤. 예전엔 40 이라 오른쪽/아래 벽을 80px 뚫고 나갔음
        val size = 120f
        val radius = size / 2f

        var rotation = 0f

        val rotationSpeed = 360f

        // 적 → 남은 피격 쿨타임(초). Enemy 는 일반 class 라 키 비교는 객체 동일성 기준.
        val hitCooldowns = mutableMapOf<Enemy, Float>()

        lateinit var sprite: ImageView

        fun getCenterX() = x + radius
        fun getCenterY() = y + radius
    }
}