package com.example.prototypebug

import androidx.annotation.DrawableRes

data class WeaponType(
    val id: String,
    val name: String,
    @DrawableRes val iconRes: Int,
    val description: String
)