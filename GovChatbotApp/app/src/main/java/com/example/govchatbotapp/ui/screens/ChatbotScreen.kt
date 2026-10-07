package com.example.govchatbotapp.ui.screens

import android.speech.tts.TextToSpeech
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.util.Locale
import com.example.govchatbotapp.BuildConfig
import com.example.govchatbotapp.data.FirebaseRepository
import com.example.govchatbotapp.data.ComplaintEntity
import java.net.HttpURLConnection
import java.net.URL
import java.io.OutputStreamWriter
import org.json.JSONObject
import org.json.JSONArray
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class Message(val text: String, val isUser: Boolean, val image: Bitmap? = null)
data class ApiMessage(val role: String, val content: String)

suspend fun callGroqApi(messages: List<ApiMessage>, apiKey: String): String {
    return withContext(Dispatchers.IO) {
        val url = URL("https://api.groq.com/openai/v1/chat/completions")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Authorization", "Bearer $apiKey")
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true

        val jsonBody = JSONObject()
        jsonBody.put("model", "openai/gpt-oss-120b")
        val jsonMessages = JSONArray()
        messages.forEach {
            val msg = JSONObject()
            msg.put("role", it.role)
            msg.put("content", it.content)
            jsonMessages.put(msg)
        }
        jsonBody.put("messages", jsonMessages)

        val writer = OutputStreamWriter(connection.outputStream)
        writer.write(jsonBody.toString())
        writer.flush()
        writer.close()

        val responseCode = connection.responseCode
        if (responseCode == HttpURLConnection.HTTP_OK) {
            val responseStr = connection.inputStream.bufferedReader().use { it.readText() }
            val responseJson = JSONObject(responseStr)
            responseJson.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
        } else {
            val errorStr = connection.errorStream?.bufferedReader()?.use { it.readText() }
            throw Exception("API Error: $responseCode - $errorStr")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatbotScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    
    DisposableEffect(context) {
        var localTts: TextToSpeech? = null
        val listener = TextToSpeech.OnInitListener { status ->
            if (status == TextToSpeech.SUCCESS) {
                localTts?.language = Locale.US
                tts = localTts
            }
        }
        localTts = TextToSpeech(context, listener)
        
        onDispose {
            localTts?.stop()
            localTts?.shutdown()
        }
    }

    var capturedImage by remember { mutableStateOf<Bitmap?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        capturedImage = bitmap
    }

    var messages by remember { mutableStateOf(listOf(
        Message("Hello! I am the GovConnect assistant. How can I help you file a complaint today?", false)
    )) }
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    var apiMessages by remember { mutableStateOf(listOf(
        ApiMessage("system", "You are a GovConnect assistant. Help citizens file complaints about water, roads, or electricity. Once you have enough details, reply normally and append exactly [FILE_COMPLAINT:department] at the end, replacing 'department' with the relevant department.")
    )) }
    
    // Automatically speak the first message
    LaunchedEffect(tts) {
        if (tts != null) {
            tts?.speak(messages.first().text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("GovConnect Support") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        )
        
        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(messages) { message ->
                MessageBubble(message)
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            }
        }
        
        if (capturedImage != null) {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                Image(
                    bitmap = capturedImage!!.asImageBitmap(),
                    contentDescription = "Preview",
                    modifier = Modifier.size(80.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                )
                IconButton(
                    onClick = { capturedImage = null },
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Remove")
                }
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { cameraLauncher.launch(null) }) {
                Icon(Icons.Default.CameraAlt, contentDescription = "Take Photo")
            }
            Spacer(modifier = Modifier.width(4.dp))
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type your issue...") }
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (inputText.isNotBlank() || capturedImage != null) {
                        val query = inputText
                        val imageToSend = capturedImage
                        messages = messages + Message(query, true, imageToSend)
                        inputText = ""
                        capturedImage = null
                        
                        
                        isLoading = true
                        scope.launch(Dispatchers.IO) {
                            try {
                                if (query.isNotBlank()) {
                                    apiMessages = apiMessages + ApiMessage("user", query)
                                } else {
                                    apiMessages = apiMessages + ApiMessage("user", "[Image Uploaded]")
                                }
                                
                                val responseStr = callGroqApi(apiMessages, BuildConfig.GROQ_API_KEY)
                                apiMessages = apiMessages + ApiMessage("assistant", responseStr)
                                
                                var botResponse = responseStr
                                
                                val regex = Regex("\\[FILE_COMPLAINT:(.*?)\\]")
                                val match = regex.find(botResponse)
                                if (match != null) {
                                    val department = match.groupValues[1].trim()
                                    botResponse = botResponse.replace(regex, "").trim()
                                    val id = "${department.uppercase().take(3)}-${(1000..9999).random()}"
                                    
                                    val repository = FirebaseRepository()
                                    
                                    val chatHistoryForDesc = apiMessages.joinToString("\n") { it.role + ": " + it.content }
                                    repository.addComplaint(
                                        ComplaintEntity(
                                            id = id,
                                            department = department,
                                            description = chatHistoryForDesc,
                                            status = "Pending Review"
                                        )
                                    ) { success, errorMsg ->
                                        if (!success) {
                                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                                android.widget.Toast.makeText(context, "Cloud sync failed: $errorMsg", android.widget.Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                    
                                    botResponse += "\n\nYour complaint has been registered with ID: $id"
                                }
                                
                                messages = messages + Message(botResponse, false)
                                tts?.speak(botResponse, TextToSpeech.QUEUE_FLUSH, null, null)
                            } catch (e: Exception) {
                                messages = messages + Message("Error: ${e.message}", false)
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                },
                modifier = Modifier.background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50))
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

@Composable
fun MessageBubble(message: Message) {
    val backgroundColor = if (message.isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
    val textColor = if (message.isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
    val alignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
    
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Column(
            modifier = Modifier
                .background(backgroundColor, RoundedCornerShape(12.dp))
                .padding(12.dp)
                .widthIn(max = 280.dp)
        ) {
            if (message.image != null) {
                Image(
                    bitmap = message.image.asImageBitmap(),
                    contentDescription = "Attached Image",
                    modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (message.text.isNotBlank()) {
                Text(
                    text = message.text,
                    color = textColor
                )
            }
        }
    }
}

