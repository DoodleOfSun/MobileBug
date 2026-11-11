package com.example.prototypebug

object WeaponDatabase {
    val all = listOf(
        WeaponType("axe",      "도끼",     R.drawable.axe,      "위로 튀어오른 뒤 떨어진다"),
        WeaponType("book",     "책",       R.drawable.book,     "주변을 회전하며 공격"),
        WeaponType("arrow",    "화살",     R.drawable.arrow,    "가장 가까운 적을 추적"),
        WeaponType("sickle",    "낫",     R.drawable.sickle,    "직선으로 날아간다"),
        WeaponType("dagger",   "단검",     R.drawable.dagger,   "이동 방향으로 발사"),
        WeaponType("missile",  "미사일",   R.drawable.missile,  "범위 표시 후 낙하"),
        WeaponType("laser",    "레이저",   R.drawable.laser,    "랜덤 방향으로 관통"),
        WeaponType("boomerang","부메랑",   R.drawable.boomerang,"날아갔다가 돌아온다"),
        WeaponType("ring",     "반지",     R.drawable.ring,     "랜덤 적 즉사"),
        WeaponType("bone",     "뼈",       R.drawable.bone,     "벽에 튕기며 유지"),
        WeaponType("blackhole","블랙홀",   R.drawable.blackhole,"적을 끌어당긴다"),
        WeaponType("bomb",     "폭탄",     R.drawable.bomb,     "일정 시간마다 설치")
    )

    fun getById(id: String) = all.find { it.id == id }
}