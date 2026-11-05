package com.yiting.app.audio

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.yiting.core.data.db.MediaItemEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AudioLibraryScreen(
    mediaItems: List<MediaItemEntity>,
    onScanComplete: (List<MediaItemEntity>) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(false) }
    var permissionDenied by remember { mutableStateOf(false) }
    var scanFailed by remember { mutableStateOf(false) }

    val audioPermission = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    fun scanAudio() {
        isLoading = true
        scanFailed = false

        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    queryLocalAudio(context.contentResolver)
                }
                onScanComplete(result)
            } catch (_: SecurityException) {
                permissionDenied = true
            } catch (_: Exception) {
                scanFailed = true
            } finally {
                isLoading = false
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionDenied = !granted
        if (granted) {
            scanAudio()
        }
    }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            audioPermission
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            scanAudio()
        } else {
            permissionLauncher.launch(audioPermission)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = "听戏 · 本地音频",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(12.dp))

        when {
            isLoading -> {
                Text("正在扫描本地音频…", fontSize = 20.sp)
            }

            permissionDenied -> {
                Text(
                    text = "需要允许访问音频，才能显示手机里的本地文件。",
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.error
                )
                Button(
                    onClick = { permissionLauncher.launch(audioPermission) }
                ) {
                    Text("重新授权", fontSize = 20.sp)
                }
            }

            scanFailed -> {
                Text("扫描失败，请检查权限后重试。", fontSize = 20.sp)
                Button(onClick = ::scanAudio) {
                    Text("重试", fontSize = 20.sp)
                }
            }

            mediaItems.isEmpty() -> {
                Text(
                    text = "没有扫描到本地音频文件。",
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = ::scanAudio) {
                    Text("重新扫描", fontSize = 20.sp)
                }
            }

            else -> {
                Button(onClick = ::scanAudio) {
                    Text("重新扫描", fontSize = 20.sp)
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = mediaItems,
                        key = { item -> item.id }
                    ) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(
                                    text = item.title,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = item.category,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatDuration(item.durationMs),
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun queryLocalAudio(
    resolver: android.content.ContentResolver
): List<MediaItemEntity> {
    val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.DURATION
    )
    val results = mutableListOf<MediaItemEntity>()

    resolver.query(
        collection,
        projection,
        null,
        null,
        "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"
    )?.use { cursor ->
        val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val titleIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val durationIndex =
            cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

        while (cursor.moveToNext()) {
            val id = cursor.getLong(idIndex)
            val uri = ContentUris.withAppendedId(collection, id)

            results += MediaItemEntity(
                id = uri.toString(),
                title = cursor.getString(titleIndex) ?: "未命名音频",
                category = "未分类",
                source = "MediaStore",
                uri = uri.toString(),
                durationMs = cursor.getLong(durationIndex).coerceAtLeast(0L),
                addedAtMs = System.currentTimeMillis()
            )
        }
    }

    return results
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs.coerceAtLeast(0L) / 1_000L
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
