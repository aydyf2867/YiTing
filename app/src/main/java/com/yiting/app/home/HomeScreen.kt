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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ibm.icu.text.CharsetDetector
import com.yiting.core.data.db.PlayRecordEntity
import com.yiting.core.data.db.ReadingProgressEntity
import com.yiting.core.ui.component.YiListItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

private const val MAX_TXT_SIZE_BYTES = 10 * 1024 * 1024

private data class OpenBook(
    val uri: Uri,
    val title: String,
    val text: String,
    val savedScrollFraction: Float
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
    val readerScrollState = rememberScrollState()
    val homeScrollState = rememberScrollState()

    var book by remember { mutableStateOf<OpenBook?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var restoredUri by remember { mutableStateOf<String?>(null) }

    suspend fun openBook(
        uri: Uri,
        knownProgress: ReadingProgressEntity? = null
    ) {
        isLoading = true

        try {
            // OpenDocument grants a persistable read permission. Keep it so
            // the document can be reopened from the recent-reading list.
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }

            val loadedBook = withContext(Dispatchers.IO) {
                val title = queryDisplayName(context, uri)
                val bytes = context.contentResolver
                    .openInputStream(uri)
                    ?.use { input -> readLimited(input, MAX_TXT_SIZE_BYTES) }
                    ?: error("无法打开文件")

                val text = decodeTxt(bytes)
                val progress = knownProgress ?: loadReadingProgress(uri.toString())

                OpenBook(
                    uri = uri,
                    title = title,
                    text = text,
                    savedScrollFraction =
                        (progress?.scrollFraction ?: 0f).coerceIn(0f, 1f)
                )
            }

            restoredUri = null
            readerScrollState.scrollTo(0)
            book = loadedBook
        } catch (_: Exception) {
            Toast.makeText(
                context,
                "读取失败：请确认文件仍可访问，且小于 10 MB",
                Toast.LENGTH_LONG
            ).show()
        } finally {
            isLoading = false
        }
    }

    val txtPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                openBook(uri)
            }
        }
    }

    val currentBook = book

    // Restore after Compose has measured the text and calculated scroll range.
    LaunchedEffect(currentBook?.uri?.toString(), readerScrollState.maxValue) {
        val current = currentBook ?: return@LaunchedEffect
        val uriKey = current.uri.toString()

        if (restoredUri != uriKey) {
            val canRestore =
                readerScrollState.maxValue > 0 ||
                    current.savedScrollFraction <= 0f ||
                    current.text.isEmpty()

            if (canRestore) {
                val target = (readerScrollState.maxValue *
                    current.savedScrollFraction).toInt()

                readerScrollState.scrollTo(target)
                restoredUri = uriKey
            }
        }
    }

    // Save progress after scrolling stops. The stored fraction remains useful
    // when the device size or font scale changes.
    LaunchedEffect(currentBook?.uri?.toString()) {
        val current = currentBook ?: return@LaunchedEffect
        val uriKey = current.uri.toString()

        snapshotFlow {
            if (restoredUri == uriKey) {
                readerScrollState.value to readerScrollState.maxValue
            } else {
                null
            }
        }
            .filterNotNull()
            .distinctUntilChanged()
            .debounce(700)
            .collect { (scrollPosition, maxScroll) ->
                val fraction = if (maxScroll > 0) {
                    (scrollPosition.toFloat() / maxScroll).coerceIn(0f, 1f)
                } else {
                    0f
                }

                saveReadingProgress(
                    ReadingProgressEntity(
                        documentUri = uriKey,
                        title = current.title,
                        scrollFraction = fraction,
                        updatedAtMs = System.currentTimeMillis()
                    )
                )
            }
    }

    // Save when leaving the reader, including when switching tabs or the
    // Activity is recreated. Saving twice is harmless because this is an upsert.
    if (currentBook != null) {
        DisposableEffect(currentBook.uri.toString()) {
            onDispose {
                val maxScroll = readerScrollState.maxValue
                val fraction = if (maxScroll > 0) {
                    (readerScrollState.value.toFloat() / maxScroll)
                        .coerceIn(0f, 1f)
                } else {
                    0f
                }

                saveReadingProgress(
                    ReadingProgressEntity(
                        documentUri = currentBook.uri.toString(),
                        title = currentBook.title,
                        scrollFraction = fraction,
                        updatedAtMs = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun closeBook() {
        val current = book ?: return
        val maxScroll = readerScrollState.maxValue
        val fraction = if (maxScroll > 0) {
            (readerScrollState.value.toFloat() / maxScroll).coerceIn(0f, 1f)
        } else {
            0f
        }

        saveReadingProgress(
            ReadingProgressEntity(
                documentUri = current.uri.toString(),
                title = current.title,
                scrollFraction = fraction,
                updatedAtMs = System.currentTimeMillis()
            )
        )
        book = null
        restoredUri = null
    }

    BackHandler(enabled = currentBook != null) {
        closeBook()
    }

    when {
        isLoading -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("正在读取文件，请稍候", fontSize = 22.sp)
            }
        }

        currentBook != null -> {
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
                    Button(onClick = ::closeBook) {
                        Text("返回首页", fontSize = 20.sp)
                    }

                    Text(
                        text = currentBook.title,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(readerScrollState)
                        .padding(bottom = 20.dp)
                ) {
                    if (currentBook.text.isBlank()) {
                        Text("这个文件没有文字内容", fontSize = 22.sp)
                    } else {
                        Text(
                            text = currentBook.text,
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
                    .verticalScroll(homeScrollState)
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

                Spacer(Modifier.height(24.dp))

                Text("最近在听", fontSize = 28.sp, fontWeight = FontWeight.Bold)

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
                            subtitle = "${formatTime(record.positionMs)} / " +
                                formatTime(record.durationMs),
                            onClick = {
                                Toast.makeText(
                                    context,
                                    "播放功能将在 T03 实现",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                }

                if (readingProgress.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Text("最近阅读", fontSize = 28.sp, fontWeight = FontWeight.Bold)

                    readingProgress.take(5).forEach { progress ->
                        YiListItem(
                            title = progress.title,
                            subtitle = "继续阅读",
                            onClick = {
                                scope.launch {
                                    openBook(
                                        Uri.parse(progress.documentUri),
                                        progress
                                    )
                                }
                            }
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

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
                    Column(Modifier.padding(20.dp)) {
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

                Spacer(Modifier.height(16.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            Toast.makeText(
                                context,
                                "广播功能将在后续版本开放",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text("听广播", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "广播功能将在后续版本开放",
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

private fun queryDisplayName(context: Context, uri: Uri): String {
    context.contentResolver.query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME),
        null,
        null,
        null
    )?.use { cursor ->
        if (cursor.moveToFirst()) {
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0) {
                return cursor.getString(index) ?: "TXT 文档"
            }
        }
    }

    return uri.lastPathSegment?.substringAfterLast('/') ?: "TXT 文档"
}

private fun readLimited(
    input: java.io.InputStream,
    maxBytes: Int
): ByteArray {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(8 * 1024)
    var totalBytes = 0

    while (true) {
        val count = input.read(buffer)
        if (count < 0) break

        totalBytes += count
        require(totalBytes <= maxBytes) { "TXT 文件超过大小限制" }
        output.write(buffer, 0, count)
    }

    return output.toByteArray()
}

private fun decodeTxt(bytes: ByteArray): String {
    if (bytes.isEmpty()) return ""

    val encodings = listOf(
        byteArrayOf(0x00, 0x00, 0xFE.toByte(), 0xFF.toByte()) to "UTF-32BE",
        byteArrayOf(0xFF.toByte(), 0xFE.toByte(), 0x00, 0x00) to "UTF-32LE",
        byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) to "UTF-8",
        byteArrayOf(0xFE.toByte(), 0xFF.toByte()) to "UTF-16BE",
        byteArrayOf(0xFF.toByte(), 0xFE.toByte()) to "UTF-16LE"
    )

    for ((bom, charsetName) in encodings) {
        if (bytes.size >= bom.size && bom.indices.all { bytes[it] == bom[it] }) {
            return String(
                bytes,
                bom.size,
                bytes.size - bom.size,
                Charset.forName(charsetName)
            )
        }
    }

    decodeStrictly(bytes, Charsets.UTF_8)?.let { return it }

    val detectedText = runCatching {
        val detector = CharsetDetector()
        detector.setText(bytes)
        detector.detect()?.getString()
    }.getOrNull()

    return detectedText ?: String(bytes, Charsets.UTF_8)
}

private fun decodeStrictly(bytes: ByteArray, charset: Charset): String? =
    runCatching {
        charset.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes))
            .toString()
    }.getOrNull()

private fun formatTime(milliseconds: Long): String {
    val totalSeconds = milliseconds.coerceAtLeast(0L) / 1_000L
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
