package com.yiting.app.home

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ibm.icu.text.CharsetDetector
import com.yiting.app.YiTingApplication
import com.yiting.core.data.db.PlayRecordEntity
import com.yiting.core.data.db.ReadingProgressEntity
import com.yiting.core.ui.component.YiListItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

private const val MAX_TXT_SIZE_BYTES = 10 * 1024 * 1024

@Composable
fun HomeScreen(
    recentPlays: List<PlayRecordEntity>,
    readingProgress: List<ReadingProgressEntity>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var importedText by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var currentUri by remember { mutableStateOf<Uri?>(null) }
    var currentTitle by remember { mutableStateOf("未命名文档") }

    BackHandler(enabled = importedText != null) {
        if (importedText != null && currentUri != null) {
            val savedOffset = importedText?.length ?: 0
            coroutineScope.launch(Dispatchers.IO) {
                val app = context.applicationContext as YiTingApplication
                app.repository.saveReadingProgress(
                    ReadingProgressEntity(
                        documentUri = currentUri.toString(),
                        title = currentTitle,
                        characterOffset = savedOffset,
                        updatedAtMs = System.currentTimeMillis()
                    )
                )
            }
        }
        importedText = null
        currentUri = null
    }

    val txtPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            isLoading = true
            currentUri = uri
            currentTitle = uri.lastPathSegment?.substringAfterLast('/') ?: "TXT阅读"
            coroutineScope.launch {
                try {
                    val text = withContext(Dispatchers.IO) {
                        val bytes = context.contentResolver
                            .openInputStream(uri)
                            ?.use { inputStream -> readBounded(inputStream, MAX_TXT_SIZE_BYTES) }
                            ?: throw IllegalStateException("无法打开文件")
                        decodeTxt(bytes)
                    }
                    importedText = text
                } catch (_: Exception) {
                    Toast.makeText(
                        context,
                        "读取失败。请确认文件是可读取的 TXT，且不超过 10 MB",
                        Toast.LENGTH_LONG
                    ).show()
                } finally {
                    isLoading = false
                }
            }
        }
    }

    when {
        isLoading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("正在读取文件，请稍候", fontSize = 22.sp)
            }
        }

        importedText != null -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(onClick = {
                        if (currentUri != null) {
                            val savedOffset = importedText?.length ?: 0
                            coroutineScope.launch(Dispatchers.IO) {
                                val app = context.applicationContext as YiTingApplication
                                app.repository.saveReadingProgress(
                                    ReadingProgressEntity(
                                        documentUri = currentUri.toString(),
                                        title = currentTitle,
                                        characterOffset = savedOffset,
                                        updatedAtMs = System.currentTimeMillis()
                                    )
                                )
                            }
                        }
                        importedText = null
                        currentUri = null
                    }) {
                        Text("返回首页", fontSize = 20.sp)
                    }

                    Text(
                        text = "正在阅读",
                        modifier = Modifier.padding(start = 16.dp),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 20.dp)
                ) {
                    if (importedText.isNullOrBlank()) {
                        Text("这个文件没有文字内容", fontSize = 22.sp)
                    } else {
                        Text(
                            text = importedText.orEmpty(),
                            fontSize = 22.sp,
                            lineHeight = 34.sp
                        )
                    }
                }
            }
        }

        else -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                Text(
                    text = "颐听",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 24.dp, bottom = 4.dp)
                )
                Text(
                    text = "听戏 · 听书 · 看书",
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(28.dp))
                Text("最近在听", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                if (recentPlays.isEmpty()) {
                    Text(
                        text = "先去添加一些戏曲或评书吧",
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                } else {
                    recentPlays.forEach { record ->
                        YiListItem(
                            title = record.title,
                            subtitle = formatProgress(record.positionMs, record.durationMs),
                            onClick = {
                                Toast.makeText(
                                    context,
                                    "播放功能将在后续版本上线",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (readingProgress.isNotEmpty()) {
                    Text(
                        text = "最近阅读",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    readingProgress.take(5).forEach { progress ->
                        YiListItem(
                            title = progress.title,
                            subtitle = "已读 ${progress.characterOffset} 字符",
                            onClick = {
                                Toast.makeText(
                                    context,
                                    "继续阅读会在后续版本实现",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            txtPicker.launch(
                                arrayOf(
                                    "text/plain",
                                    "text/*",
                                    "application/octet-stream",
                                    "application/x-txt"
                                )
                            )
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "看书",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "选择 TXT 文件开始阅读",
                                fontSize = 20.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("听广播", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "电台功能将在后续版本开放",
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

private fun readBounded(input: java.io.InputStream, maxBytes: Int): ByteArray {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(8 * 1024)
    var total = 0
    while (true) {
        val count = input.read(buffer)
        if (count < 0) break
        total += count
        require(total <= maxBytes) { "TXT 文件过大" }
        output.write(buffer, 0, count)
    }
    return output.toByteArray()
}

private fun decodeTxt(bytes: ByteArray): String {
    if (bytes.isEmpty()) return ""

    val bomEncodings = listOf(
        byteArrayOf(0x00, 0x00, 0xFE.toByte(), 0xFF.toByte()) to "UTF-32BE",
        byteArrayOf(0xFF.toByte(), 0xFE.toByte(), 0x00, 0x00) to "UTF-32LE",
        byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) to "UTF-8",
        byteArrayOf(0xFE.toByte(), 0xFF.toByte()) to "UTF-16BE",
        byteArrayOf(0xFF.toByte(), 0xFE.toByte()) to "UTF-16LE"
    )
    for ((bom, encoding) in bomEncodings) {
        if (hasPrefix(bytes, bom)) {
            return String(bytes, bom.size, bytes.size - bom.size, Charset.forName(encoding))
        }
    }

    detectUtf16WithoutBom(bytes)?.let { encoding ->
        return String(bytes, Charset.forName(encoding))
    }
    decodeStrictly(bytes, Charsets.UTF_8)?.let { return it }

    val detectedText = runCatching {
        CharsetDetector().setText(bytes).detect()?.getString()
    }.getOrNull()
    return detectedText ?: String(bytes, Charsets.UTF_8)
}

private fun hasPrefix(bytes: ByteArray, prefix: ByteArray): Boolean =
    bytes.size >= prefix.size && prefix.indices.all { bytes[it] == prefix[it] }

private fun detectUtf16WithoutBom(bytes: ByteArray): String? {
    if (bytes.size < 4) return null
    val sampleLength = minOf(bytes.size, 200)
    var evenZeroCount = 0
    var oddZeroCount = 0
    var evenCount = 0
    var oddCount = 0
    for (index in 0 until sampleLength) {
        if (index % 2 == 0) {
            evenCount++
            if (bytes[index] == 0.toByte()) evenZeroCount++
        } else {
            oddCount++
            if (bytes[index] == 0.toByte()) oddZeroCount++
        }
    }
    val evenZeroRatio = evenZeroCount.toFloat() / evenCount
    val oddZeroRatio = oddZeroCount.toFloat() / oddCount
    return when {
        oddZeroRatio > 0.3f && evenZeroRatio < 0.1f -> "UTF-16LE"
        evenZeroRatio > 0.3f && oddZeroRatio < 0.1f -> "UTF-16BE"
        else -> null
    }
}

private fun decodeStrictly(bytes: ByteArray, charset: Charset): String? = runCatching {
    charset.newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT)
        .decode(ByteBuffer.wrap(bytes))
        .toString()
}.getOrNull()

private fun formatProgress(positionMs: Long, durationMs: Long): String {
    if (durationMs <= 0L) return "还没听过"
    return "${formatTime(positionMs)} / ${formatTime(durationMs)}"
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
