package com.yiting.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val PrimaryOrange = Color(0xFFFF6B35)
val SurfaceCream = Color(0xFFFFFBF5)
val BackgroundCream = Color(0xFFFDF8F2)

private val LightColors = lightColorScheme(
    primary = PrimaryOrange,
    surface = SurfaceCream,
    background = BackgroundCream
)

@Composable
fun YiTINGTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content
    )
}
