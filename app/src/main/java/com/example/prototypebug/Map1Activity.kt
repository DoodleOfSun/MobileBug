
package com.example.prototypebug

class Map1Activity : BaseGameActivity() {
    override fun getLayoutId() = R.layout.activity_map1

    override fun createEnemySpawner() = EnemySpawnAndManagement(
        enemyDrawable = R.drawable.map1_zombie,
        bossDrawable = R.drawable.map1_boss
    )

    override fun createRewardManagement() = RewardManagement(R.drawable.map1_reward_jewel)

    override fun onNormalAttackKill(enemy: Enemy) {
        container.showEnergyBurst(enemy.getCenterX(), enemy.getCenterY())
    }
}
