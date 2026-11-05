package com.yiting.app.home

import android.icu.text.CharsetDetector
import android.widget.Toast
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
import com.yiting.core.data.db.PlayRecordEntity
import com.yiting.core.ui.component.YiListItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

@Composable
fun HomeScreen(
    recentPlays: List<PlayRecordEntity>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var importedText by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val txtPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            isLoading = true
            coroutineScope.launch {
                try {
                    val text = withContext(Dispatchers.IO) {
                        val bytes = context.contentResolver
                            .openInputStream(uri)
                            ?.use { inputStream -> inputStream.readBytes() }
                            ?: throw IllegalStateException("无法打开文件")

                        decodeTxt(bytes)
                    }

                    importedText = text
                } catch (_: Exception) {
                    Toast.makeText(
                        context,
                        "读取文件失败，请重新选择 TXT 文件",
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
                Text(
                    text = "正在读取文件，请稍候",
                    fontSize = 22.sp
                )
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
                    Button(onClick = { importedText = null }) {
                        Text(
                            text = "返回首页",
                            fontSize = 20.sp
                        )
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
                        Text(
                            text = "这个文件没有文字内容",
                            fontSize = 22.sp
                        )
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
                    modifier = Modifier.padding(top = 24.dp, bottom = 4.dp)
                )

                Text(
                    text = "听戏 · 听书 · 看书",
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = "最近在听",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (recentPlays.isEmpty()) {
                    Text(
                        text = "先去添加一些戏曲或评书吧",
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 32.dp)
                    )
                } else {
                    recentPlays.forEach { record ->
                        YiListItem(
                            title = record.title,
                            subtitle = formatProgress(
                                record.positionMs,
                                record.durationMs
                            ),
                            onClick = { }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFA2D48)
                    )
                ) {
                    Row(
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
                            }
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "看书",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        Text(
                            text = "导入 TXT 开始阅读",
                            fontSize = 18.sp,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "电台功能将在后续版本上线",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            }
        }
    }
}

private fun decodeTxt(bytes: ByteArray): String {
    if (bytes.isEmpty()) {
        return ""
    }

    val bomEncodings = listOf(
        byteArrayOf(0x00, 0x00, 0xFE.toByte(), 0xFF.toByte()) to "UTF-32BE",
        byteArrayOf(0xFF.toByte(), 0xFE.toByte(), 0x00, 0x00) to "UTF-32LE",
        byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) to "UTF-8",
        byteArrayOf(0xFE.toByte(), 0xFF.toByte()) to "UTF-16BE",
        byteArrayOf(0xFF.toByte(), 0xFE.toByte()) to "UTF-16LE"
    )

    for ((bom, encoding) in bomEncodings) {
        if (bytes.startsWith(bom)) {
            return decodeWithCharset(
                bytes = bytes,
                offset = bom.size,
                charsetName = encoding
            )
        }
    }

    detectUtf16WithoutBom(bytes)?.let { encoding ->
        return decodeWithCharset(
            bytes = bytes,
            offset = 0,
            charsetName = encoding
        )
    }

    decodeStrictly(bytes, Charsets.UTF_8)?.let { return it }

    val detectedText = runCatching {
        val detector = CharsetDetector()
        detector.setText(bytes)

        detector.detect()
            ?.getString()
    }.getOrNull()

    if (detectedText != null) {
        return detectedText
    }

    val fallbackEncodings = listOf(
        "GB18030",
        "Big5",
        "Shift_JIS",
        "EUC-JP",
        "EUC-KR",
        "windows-1252",
        "ISO-8859-1"
    )

    for (encoding in fallbackEncodings) {
        val decoded = runCatching {
            Charset.forName(encoding).decode(ByteBuffer.wrap(bytes)).toString()
        }.getOrNull()

        if (decoded != null) {
            return decoded
        }
    }

    return String(bytes, Charsets.UTF_8)
}

private fun detectUtf16WithoutBom(bytes: ByteArray): String? {
    if (bytes.size < 4) {
        return null
    }

    val sampleLength = minOf(bytes.size, 200)
    var evenZeroCount = 0
    var oddZeroCount = 0
    var evenCount = 0
    var oddCount = 0

    for (index in 0 until sampleLength) {
        if (index % 2 == 0) {
            evenCount++
            if (bytes[index] == 0.toByte()) {
                evenZeroCount++
            }
        } else {
            oddCount++
            if (bytes[index] == 0.toByte()) {
                oddZeroCount++
            }
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

private fun decodeWithCharset(
    bytes: ByteArray,
    offset: Int,
    charsetName: String
): String {
    return Charset.forName(charsetName)
        .decode(ByteBuffer.wrap(bytes, offset, bytes.size - offset))
        .toString()
}

private fun decodeStrictly(
    bytes: ByteArray,
    charset: Charset
): String? {
    return runCatching {
        charset.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes))
            .toString()
    }.getOrNull()
}

private fun formatProgress(positionMs: Long, durationMs: Long): String {
    if (durationMs <= 0L) return "还没听过"
    return "${formatTime(positionMs)} / ${formatTime(durationMs)}"
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
