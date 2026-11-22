package com.example.prototypebug

import android.content.Context
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

    private val damage = 100f

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

            enemies.forEach { enemy ->
                if (checkCollision(bone, enemy)) {
                    enemy.takeDamage(damage)
                }
            }
        }
    }

    private fun spawnBone(player: PlayerInfo) {
        val boneView = ImageView(context)
        val size = boneSize
        boneView.layoutParams = ConstraintLayout.LayoutParams(size, size)
        boneView.setImageResource(R.drawable.bone)
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
        val size = 40f
        val radius = 60f

        var rotation = 0f

        val rotationSpeed = 360f

        lateinit var sprite: ImageView

        fun getCenterX() = x + radius
        fun getCenterY() = y + radius
    }
}