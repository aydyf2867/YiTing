package com.yiting.app.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ibm.icu.text.CharsetDetector
import com.yiting.core.data.db.PlayRecordEntity
import com.yiting.core.data.db.ReadingProgressEntity
import com.yiting.core.ui.component.YiListItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.debounce
import kotlinx.coroutines.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

private const val MAX_TXT_SIZE = 10 * 1024 * 1024

private data class OpenBook(
    val uri: Uri,
    val title: String,
    val text: String,
    val savedCharacterOffset: Int
)

@OptIn(FlowPreview::class)
@Composable
fun HomeScreen(
    recentPlays: List<PlayRecordEntity>,
    readingProgress: List<ReadingProgressEntity>,
    loadReadingProgress: suspend (String) -> ReadingProgressEntity?,
    saveReadingProgress: (ReadingProgressEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var book by remember { mutableStateOf<OpenBook?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    var restoredUri by remember { mutableStateOf<String?>(null) }

    suspend fun openBook(uri: Uri, knownProgress: ReadingProgressEntity? = null) {
        loading = true
        try {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            val result = withContext(Dispatchers.IO) {
                val name = queryDisplayName(context, uri)
                val bytes = context.contentResolver.openInputStream(uri)?.use {
                    readLimited(it, MAX_TXT_SIZE)
                } ?: error("无法打开文件")
                val text = decodeText(bytes)
                val progress = knownProgress ?: loadReadingProgress(uri.toString())
                OpenBook(uri, name, text, progress?.characterOffset ?: 0)
            }
            restoredUri = null
            scrollState.scrollTo(0)
            book = result
        } catch (_: Exception) {
            Toast.makeText(context, "读取失败：请确认文件可访问且不超过 10 MB", Toast.LENGTH_LONG).show()
        } finally {
            loading = false
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch { openBook(uri) }
    }

    LaunchedEffect(book?.uri?.toString(), scrollState.maxValue) {
        val current = book ?: return@LaunchedEffect
        val key = current.uri.toString()
        if (restoredUri != key && (scrollState.maxValue > 0 || current.text.isEmpty())) {
            val fraction = if (current.text.isNotEmpty()) {
                (current.savedCharacterOffset.toFloat() / current.text.length).coerceIn(0f, 1f)
            } else 0f
            scrollState.scrollTo((scrollState.maxValue * fraction).toInt())
            restoredUri = key
        }
    }

    LaunchedEffect(book?.uri?.toString()) {
        val current = book ?: return@LaunchedEffect
        snapshotFlow {
            if (restoredUri == current.uri.toString()) scrollState.value to scrollState.maxValue else null
        }.filterNotNull().distinctUntilChanged().debounce(600).collect { (position, max) ->
            val fraction = if (max > 0) position.toFloat() / max else 0f
            saveReadingProgress(
                ReadingProgressEntity(
                    documentUri = current.uri.toString(),
                    title = current.title,
                    characterOffset = (current.text.length * fraction).toInt(),
                    updatedAtMs = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun closeBook() {
        val current = book ?: return
        val fraction = if (scrollState.maxValue > 0) {
            scrollState.value.toFloat() / scrollState.maxValue
        } else 0f
        saveReadingProgress(
            ReadingProgressEntity(
                documentUri = current.uri.toString(),
                title = current.title,
                characterOffset = (current.text.length * fraction).toInt(),
                updatedAtMs = System.currentTimeMillis()
            )
        )
        book = null
    }

    BackHandler(enabled = book != null) { scope.launch { closeBook() } }

    if (loading) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("正在读取文件，请稍候", fontSize = 22.sp, modifier = Modifier.padding(32.dp))
        }
    } else if (book != null) {
        val current = book!!
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = { scope.launch { closeBook() } }) { Text("返回首页", fontSize = 20.sp) }
                Text(current.title, Modifier.padding(start = 12.dp), fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
            Column(Modifier.fillMaxSize().verticalScroll(scrollState).padding(bottom = 20.dp)) {
                if (current.text.isBlank()) Text("这个文件没有文字内容", fontSize = 22.sp)
                else Text(current.text, fontSize = 22.sp, lineHeight = 34.sp)
            }
        }
    } else {
        Column(
            modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)
        ) {
            Text("颐听", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 24.dp, bottom = 4.dp))
            Text("听戏 · 听书 · 看书", fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
            Text("最近在听", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            if (recentPlays.isEmpty()) {
                Text("还没有播放记录。添加本地内容后，会在这里看到最近播放。", fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 24.dp))
            } else {
                recentPlays.forEach { record ->
                    YiListItem(record.title, "${formatTime(record.positionMs)} / ${formatTime(record.durationMs)}") {
                        Toast.makeText(context, "播放功能将在 T03 实现", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            if (readingProgress.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text("最近阅读", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                readingProgress.take(5).forEach { progress ->
                    YiListItem(progress.title, "继续阅读") {
                        scope.launch { openBook(Uri.parse(progress.documentUri), progress) }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Card(
                Modifier.fillMaxWidth().clickable { picker.launch(arrayOf("text/plain", "text/*", "application/octet-stream", "application/x-txt")) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("看书", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("选择 TXT 文件开始阅读", fontSize = 20.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Spacer(Modifier.height(16.dp))
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(20.dp)) {
                    Text("听广播", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("电台功能将在后续版本开放", fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun queryDisplayName(context: Context, uri: Uri): String {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0) return cursor.getString(index) ?: "TXT 文档"
        }
    }
    return uri.lastPathSegment?.substringAfterLast('/') ?: "TXT 文档"
}

private fun readLimited(input: java.io.InputStream, limit: Int): ByteArray {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    var countTotal = 0
    while (true) {
        val count = input.read(buffer)
        if (count < 0) break
        countTotal += count
        require(countTotal <= limit) { "文件过大" }
        output.write(buffer, 0, count)
    }
    return output.toByteArray()
}

private fun decodeText(bytes: ByteArray): String {
    if (bytes.isEmpty()) return ""
    val boms = listOf(
        byteArrayOf(0x00, 0x00, 0xFE.toByte(), 0xFF.toByte()) to "UTF-32BE",
        byteArrayOf(0xFF.toByte(), 0xFE.toByte(), 0x00, 0x00) to "UTF-32LE",
        byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) to "UTF-8",
        byteArrayOf(0xFE.toByte(), 0xFF.toByte()) to "UTF-16BE",
        byteArrayOf(0xFF.toByte(), 0xFE.toByte()) to "UTF-16LE"
    )
    boms.firstOrNull { (bom, _) -> bytes.size >= bom.size && bom.indices.all { bytes[it] == bom[it] } }?.let { (bom, charset) ->
        return String(bytes, bom.size, bytes.size - bom.size, Charset.forName(charset))
    }
    decodeStrict(bytes, Charsets.UTF_8)?.let { return it }
    return runCatching { CharsetDetector().setText(bytes).detect()?.getString() }.getOrNull()
        ?: String(bytes, Charsets.UTF_8)
}

private fun decodeStrict(bytes: ByteArray, charset: Charset): String? = runCatching {
    charset.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString()
}.getOrNull()

private fun formatTime(ms: Long): String {
    val seconds = (ms.coerceAtLeast(0) / 1000)
    return "%d:%02d".format(seconds / 60, seconds % 60)
}
