package com.copdhealthtracker.ui.models

import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes

data class Recipe(
    val name: String,
    val description: String,
    val tags: List<String>,
    @DrawableRes val iconRes: Int,
    @ColorInt val iconTint: Int
)
