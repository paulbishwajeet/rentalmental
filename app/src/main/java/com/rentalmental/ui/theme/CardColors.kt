package com.rentalmental.ui.theme

import androidx.compose.ui.graphics.Color

val cardColors = listOf(
    Color(0xFFE8F5E9), // soft green
    Color(0xFFE3F2FD), // soft blue
    Color(0xFFFFF3E0), // soft orange
    Color(0xFFF3E5F5), // soft purple
    Color(0xFFE0F7FA), // soft cyan
    Color(0xFFFCE4EC), // soft pink
    Color(0xFFFFF9C4), // soft yellow
    Color(0xFFE8EAF6), // soft indigo
)

fun cardColorFor(index: Int): Color = cardColors[index % cardColors.size]
