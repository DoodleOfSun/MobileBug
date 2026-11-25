package com.example.prototypebug

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout

data class WeaponType(
    val id: String,
    val name: String,
    @param:DrawableRes val iconRes: Int,
    val description: String,
    val create: ((WeaponType, ConstraintLayout, Context) -> Weapon)? = null
)
