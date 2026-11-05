package com.yiting.app

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yiting.app.home.HomeScreen
import com.yiting.app.home.HomeViewModel
import com.yiting.core.ui.theme.YiTingTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            YiTingTheme {
                val viewModel: HomeViewModel = viewModel()
                MainScreen(viewModel)
            }
        }
    }
}

private data class TabItem(
    val label: String,
    val icon: ImageVector
)

@Composable
fun MainScreen(viewModel: HomeViewModel) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    var lastBackPressAt by remember { mutableStateOf(0L) }

    BackHandler {
        val now = System.currentTimeMillis()
        if (now - lastBackPressAt < 2_000) {
            (context as? Activity)?.finish()
        } else {
            lastBackPressAt = now
            Toast.makeText(context, "再按一次返回键退出颐听", Toast.LENGTH_SHORT).show()
        }
    }

    val tabs = listOf(
        TabItem("首页", Icons.Filled.Home),
        TabItem("听戏", Icons.Filled.LibraryMusic),
        TabItem("听书", Icons.Filled.Headphones)
    )

    val recentPlays by viewModel.recentPlays.collectAsState()
    val readingProgress by viewModel.readingProgress.collectAsState()

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label, fontSize = 20.sp) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> HomeScreen(
                    recentPlays = recentPlays,
                    readingProgress = readingProgress,
                    loadReadingProgress = viewModel::getReadingProgress,
                    saveReadingProgress = viewModel::saveReadingProgress
                )
                1 -> PlaceholderScreen("听戏功能待 T03")
                2 -> PlaceholderScreen("听书功能待 T03")
                else -> PlaceholderScreen("功能待开发")
            }
        }
    }
}

@Composable
fun PlaceholderScreen(text: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 24.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
