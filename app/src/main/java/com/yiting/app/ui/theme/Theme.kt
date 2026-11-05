package com.yiting.app.ui.theme

import androidx.compose.runtime.Composable
import com.yiting.core.ui.theme.YiTingTheme

/**
 * Compatibility entry point. The shared core-ui theme is the single
 * source of colors and typography.
 */
@Composable
fun YiTINGTheme(content: @Composable () -> Unit) {
    YiTingTheme(content = content)
}
