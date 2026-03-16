package com.onurkukal.transkriptapk

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TranscriptionScreen()
                }
            }
        }
    }
}

@Composable
fun TranscriptionScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var apiKey by remember { mutableStateOf("") }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("gpt-4o-mini-transcribe") }
    var language by remember { mutableStateOf("tr") }
    var transcript by remember { mutableStateOf("") }
    var statusText by remember { mutableStateOf("Ses dosyası seçilmedi.") }
    var isLoading by remember { mutableStateOf(false) }

    val picker = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        selectedUri = uri
        selectedFileName = queryFileName(context, uri) ?: "secilen_dosya"
        statusText = "Dosya seçildi: $selectedFileName"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Ses Kaydı Transkript",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = "OpenAI API anahtarını gir, ses dosyasını seç ve transkript oluştur.",
            style = MaterialTheme.typography.bodyMedium
        )

        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it.trim() },
            label = { Text("OpenAI API Key") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = model,
            onValueChange = { model = it.trim() },
            label = { Text("Model") },
            supportingText = { Text("Öneri: gpt-4o-mini-transcribe veya gpt-4o-transcribe") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = language,
            onValueChange = { language = it.trim() },
            label = { Text("Dil Kodu") },
            supportingText = { Text("Türkçe için: tr") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { picker.launch(arrayOf("audio/*", "video/*")) }) {
                Text("Dosya Seç")
            }

            Button(
                onClick = {
                    if (apiKey.isBlank()) {
                        toast(context, "Önce API key gir.")
                        return@Button
                    }
                    if (selectedUri == null) {
                        toast(context, "Önce ses dosyası seç.")
                        return@Button
                    }

                    isLoading = true
                    statusText = "Transkript alınıyor..."
                    transcript = ""

                    scope.launch {
                        val result = runCatching {
                            transcribeAudio(
                                context = context,
                                uri = selectedUri!!,
                                apiKey = apiKey,
                                model = model.ifBlank { "gpt-4o-mini-transcribe" },
                                language = language.ifBlank { "tr" }
                            )
                        }

                        isLoading = false
                        result.onSuccess {
                            transcript = it
                            statusText = "Transkript tamamlandı."
                        }.onFailure {
                            statusText = "Hata: ${it.message ?: "Bilinmeyen hata"}"
                            toast(context, statusText)
                        }
                    }
                },
                enabled = !isLoading
            ) {
                Text("Transkript Başlat")
            }
        }

        Text(text = statusText, style = MaterialTheme.typography.bodyMedium)
        if (selectedFileName.isNotBlank()) {
            Text(text = "Seçilen dosya: $selectedFileName")
        }

        if (isLoading) {
            CircularProgressIndicator()
        }

        Divider()

        OutlinedTextField(
            value = transcript,
            onValueChange = { transcript = it },
            label = { Text("Transkript Sonucu") },
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp),
            textStyle = MaterialTheme.typography.bodyMedium,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = {
                    copyToClipboard(context, transcript)
                    toast(context, "Metin panoya kopyalandı.")
                },
                enabled = transcript.isNotBlank()
            ) {
                Text("Kopyala")
            }

            Button(
                onClick = {
                    val saved = saveTranscriptToDownloads(context, transcript)
                    toast(context, saved)
                },
                enabled = transcript.isNotBlank()
            ) {
                Text("TXT Kaydet")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

private suspend fun transcribeAudio(
    context: Context,
    uri: Uri,
    apiKey: String,
    model: String,
    language: String
): String = withContext(Dispatchers.IO) {
    val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(180, TimeUnit.SECONDS)
        .build()

    val fileName = queryFileName(context, uri) ?: "audio_upload"
    val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
    val tempFile = copyUriToTempFile(context, uri, fileName)

    val requestBody = MultipartBody.Builder()
        .setType(MultipartBody.FORM)
        .addFormDataPart("model", model)
        .addFormDataPart("language", language)
        .addFormDataPart("response_format", "json")
        .addFormDataPart(
            "file",
            tempFile.name,
            tempFile.asRequestBody(mimeType.toMediaTypeOrNull())
        )
        .build()

    val request = Request.Builder()
        .url("https://api.openai.com/v1/audio/transcriptions")
        .addHeader("Authorization", "Bearer $apiKey")
        .post(requestBody)
        .build()

    client.newCall(request).execute().use { response ->
        val body = response.body?.string().orEmpty()
        tempFile.delete()

        if (!response.isSuccessful) {
            throw IllegalStateException("HTTP ${response.code}: $body")
        }

        val json = JSONObject(body)
        json.optString("text").ifBlank {
            throw IllegalStateException("Yanıtta transkript metni bulunamadı.")
        }
    }
}

private fun queryFileName(context: Context, uri: Uri): String? {
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (index >= 0 && cursor.moveToFirst()) {
            return cursor.getString(index)
        }
    }
    return null
}

private fun copyUriToTempFile(context: Context, uri: Uri, fileName: String): File {
    val safeName = fileName.ifBlank { "audio_input" }
    val tempFile = File(context.cacheDir, safeName)
    context.contentResolver.openInputStream(uri)?.use { input ->
        tempFile.outputStream().use { output ->
            input.copyTo(output)
        }
    } ?: throw IllegalStateException("Dosya okunamadı.")
    return tempFile
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("transcript", text))
}

private fun saveTranscriptToDownloads(context: Context, text: String): String {
    return try {
        val file = File(context.getExternalFilesDir(null), "transcript_${System.currentTimeMillis()}.txt")
        file.writeText(text)
        "Kaydedildi: ${file.absolutePath}"
    } catch (e: Exception) {
        "Kaydedilemedi: ${e.message}"
    }
}

private fun toast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}
