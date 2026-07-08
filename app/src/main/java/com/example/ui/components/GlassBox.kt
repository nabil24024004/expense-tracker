package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalAppColors
import com.example.ui.theme.DarkAppColors

@Composable
fun GlassBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    isDark: Boolean = LocalAppColors.current == DarkAppColors,
    content: @Composable BoxScope.() -> Unit
) {
    val backgroundBrush = remember(isDark) {
        if (isDark) {
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFF020203).copy(alpha = 0.95f),
                    Color(0xFF020203).copy(alpha = 0.90f)
                )
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFFE5E5E2).copy(alpha = 0.95f),
                    Color(0xFFE5E5E2).copy(alpha = 0.80f)
                )
            )
        }
    }

    val borderBrush = remember(isDark) {
        if (isDark) {
            Brush.linearGradient(
                colors = listOf(
                    Color(0x33FFFFFF),
                    Color(0x0DFFFFFF)
                )
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    Color(0x1A020203),
                    Color(0x05020203)
                )
            )
        }
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundBrush)
            .border(1.dp, borderBrush, shape)
    ) {
        content()
    }
}

