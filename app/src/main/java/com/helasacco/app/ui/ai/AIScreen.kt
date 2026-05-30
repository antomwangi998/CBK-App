package com.helasacco.app.ui.ai

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.helasacco.app.ui.theme.HelaColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val content: String,
    val isUser: Boolean,
    val timestamp: String = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")),
)

data class AIUiState(
    val messages: List<ChatMessage> = listOf(
        ChatMessage(content = "Hello! I'm your SACCO AI assistant. I can help you with loan calculations, member queries, financial summaries, and more. How can I help you today?", isUser = false),
    ),
    val inputText: String = "",
    val isTyping: Boolean = false,
)

@HiltViewModel
class AIViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(AIUiState())
    val uiState = _uiState.asStateFlow()

    private val systemPrompt = """
        You are a helpful AI assistant for Hela Smart SACCO, a Kenyan savings and credit cooperative organization.
        You help staff and members with:
        - Loan calculations (EMI, amortization schedules, interest)
        - Member account queries and guidance
        - SACCO regulations and policies
        - Financial literacy advice
        - Reporting summaries
        Keep answers concise, practical, and relevant to Kenya's financial context.
        When giving amounts, use KES (Kenyan Shillings).
    """.trimIndent()

    fun onInputChange(text: String) { _uiState.update { it.copy(inputText = text) } }

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isBlank()) return

        val userMsg = ChatMessage(content = text, isUser = true)
        _uiState.update { it.copy(messages = it.messages + userMsg, inputText = "", isTyping = true) }

        viewModelScope.launch {
            try {
                val history = _uiState.value.messages.dropLast(0).map {
                    mapOf("role" to if (it.isUser) "user" else "assistant", "content" to it.content)
                }
                val response = callClaudeAPI(history, systemPrompt)
                val assistantMsg = ChatMessage(content = response, isUser = false)
                _uiState.update { it.copy(messages = it.messages + assistantMsg, isTyping = false) }
            } catch (e: Exception) {
                val errorMsg = ChatMessage(content = "Sorry, I couldn't process that request. Please try again.", isUser = false)
                _uiState.update { it.copy(messages = it.messages + errorMsg, isTyping = false) }
            }
        }
    }

    private suspend fun callClaudeAPI(history: List<Map<String, String>>, system: String): String {
        // Uses OkHttp — add implementation("com.squareup.okhttp3:okhttp:4.12.0") to build.gradle.kts
        val client = okhttp3.OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        val body = org.json.JSONObject().apply {
            put("model", "claude-sonnet-4-20250514")
            put("max_tokens", 1024)
            put("system", system)
            put("messages", org.json.JSONArray().apply {
                history.forEach { msg ->
                    put(org.json.JSONObject().apply {
                        put("role", msg["role"])
                        put("content", msg["content"])
                    })
                }
            })
        }

        val request = okhttp3.Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .addHeader("x-api-key", com.helasacco.app.BuildConfig.ANTHROPIC_API_KEY)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("content-type", "application/json")
            .build()

        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val response = client.newCall(request).execute()
            val json = org.json.JSONObject(response.body?.string() ?: "")
            json.getJSONArray("content").getJSONObject(0).getString("text")
        }
    }

    fun clearChat() {
        _uiState.update {
            it.copy(messages = listOf(ChatMessage(content = "Chat cleared. How can I help you?", isUser = false)))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIAssistantScreen(
    onBack: () -> Unit,
    viewModel: AIViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.SmartToy, null, modifier = Modifier.size(24.dp))
                        Column {
                            Text("AI Assistant", style = MaterialTheme.typography.titleMedium)
                            Text("Powered by Claude", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f))
                        }
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } },
                actions = { IconButton(onClick = viewModel::clearChat) { Icon(Icons.Filled.DeleteSweep, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary, titleContentColor = MaterialTheme.colorScheme.onPrimary, navigationIconContentColor = MaterialTheme.colorScheme.onPrimary, actionIconContentColor = MaterialTheme.colorScheme.onPrimary),
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp).navigationBarsPadding(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    OutlinedTextField(
                        value = uiState.inputText,
                        onValueChange = viewModel::onInputChange,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Ask me anything…") },
                        maxLines = 4,
                        shape = RoundedCornerShape(20.dp),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Send),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSend = { viewModel.sendMessage() }),
                    )
                    FloatingActionButton(
                        onClick = viewModel::sendMessage,
                        modifier = Modifier.size(48.dp),
                        containerColor = if (uiState.inputText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (uiState.inputText.isNotBlank()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        elevation = FloatingActionButtonDefaults.elevation(0.dp),
                    ) {
                        Icon(Icons.Filled.Send, "Send", modifier = Modifier.size(20.dp))
                    }
                }
            }
        },
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(uiState.messages, key = { it.id }) { message ->
                ChatBubble(message = message)
            }
            if (uiState.isTyping) {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                        Card(shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Row(modifier = Modifier.padding(12.dp, 10.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                repeat(3) { i ->
                                    val anim = rememberInfiniteTransition(label = "dot$i")
                                    val alpha by anim.animateFloat(initialValue = 0.3f, targetValue = 1f, animationSpec = infiniteRepeatable(animation = androidx.compose.animation.core.tween(600, delayMillis = i * 200), repeatMode = androidx.compose.animation.core.RepeatMode.Reverse), label = "alpha$i")
                                    Box(modifier = Modifier.size(8.dp).padding(1.dp).background(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha), shape = androidx.compose.foundation.shape.CircleShape))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start,
    ) {
        Card(
            shape = if (message.isUser) RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp) else RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp),
            colors = CardDefaults.cardColors(containerColor = if (message.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.widthIn(max = 300.dp),
        ) {
            Column(modifier = Modifier.padding(12.dp, 8.dp)) {
                Text(message.content, style = MaterialTheme.typography.bodyMedium, color = if (message.isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                Text(message.timestamp, style = MaterialTheme.typography.labelSmall, color = if (message.isUser) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.End))
            }
        }
    }
}

