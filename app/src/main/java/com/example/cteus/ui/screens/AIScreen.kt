package com.example.cteus.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.cteus.data.model.AIMessage
import com.example.cteus.data.model.AISession
import com.example.cteus.ui.viewmodel.AIViewModel
import kotlinx.coroutines.launch

/**
 * 简单的 Markdown 到 AnnotatedString 的转换器
 * 支持：标题、粗体、斜体、删除线、代码块、列表
 */
fun markdownToAnnotatedString(markdown: String): androidx.compose.ui.text.AnnotatedString {
    return buildAnnotatedString {
        val lines = markdown.split("\n")
        lines.forEachIndexed { index, line ->
            if (line.isNotBlank()) {
                // 处理标题
                when {
                    line.startsWith("### ") -> {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp)) {
                            append(line.removePrefix("### "))
                        }
                    }
                    line.startsWith("## ") -> {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp)) {
                            append(line.removePrefix("## "))
                        }
                    }
                    line.startsWith("# ") -> {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp)) {
                            append(line.removePrefix("# "))
                        }
                    }
                    // 处理列表项
                    line.startsWith("- ") || line.startsWith("* ") || line.startsWith("• ") -> {
                        withStyle(style = SpanStyle(fontFamily = FontFamily.Monospace)) {
                            append("• ")
                            append(line.substring(2))
                        }
                    }
                    // 处理有序列表
                    line.matches(Regex("^\\d+\\.\\s.*")) -> {
                        withStyle(style = SpanStyle(fontFamily = FontFamily.Monospace)) {
                            append(line)
                        }
                    }
                    // 处理引用
                    line.startsWith("> ") -> {
                        withStyle(style = SpanStyle(fontStyle = FontStyle.Italic, color = Color.Gray)) {
                            append(line.removePrefix("> "))
                        }
                    }
                    // 处理代码块
                    line.startsWith("```") -> {
                        // 跳过代码块标记
                    }
                    // 普通文本，处理内联格式
                    else -> {
                        appendInlineMarkdown(line)
                    }
                }
                if (index < lines.size - 1) {
                    append("\n")
                }
            } else {
                append("\n")
            }
        }
    }
}

/**
 * 处理内联 Markdown 格式（粗体、斜体、代码等）
 */
fun androidx.compose.ui.text.AnnotatedString.Builder.appendInlineMarkdown(text: String) {
    var currentIndex = 0
    val length = text.length
    
    while (currentIndex < length) {
        // 处理粗体 **text**
        val boldStart = text.indexOf("**", currentIndex)
        if (boldStart != -1) {
            val boldEnd = text.indexOf("**", boldStart + 2)
            if (boldEnd != -1 && boldEnd > boldStart) {
                // 添加粗体前的普通文本
                append(text.substring(currentIndex, boldStart))
                // 添加粗体文本
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(text.substring(boldStart + 2, boldEnd))
                }
                currentIndex = boldEnd + 2
                continue
            }
        }
        
        // 处理斜体 *text*
        val italicStart = text.indexOf("*", currentIndex)
        if (italicStart != -1) {
            val italicEnd = text.indexOf("*", italicStart + 1)
            if (italicEnd != -1 && italicEnd > italicStart) {
                // 添加斜体前的普通文本
                append(text.substring(currentIndex, italicStart))
                // 添加斜体文本
                withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                    append(text.substring(italicStart + 1, italicEnd))
                }
                currentIndex = italicEnd + 1
                continue
            }
        }
        
        // 处理代码 `text`
        val codeStart = text.indexOf("`", currentIndex)
        if (codeStart != -1) {
            val codeEnd = text.indexOf("`", codeStart + 1)
            if (codeEnd != -1 && codeEnd > codeStart) {
                // 添加代码前的普通文本
                append(text.substring(currentIndex, codeStart))
                // 添加代码文本
                withStyle(style = SpanStyle(fontFamily = FontFamily.Monospace, background = Color.LightGray)) {
                    append(text.substring(codeStart + 1, codeEnd))
                }
                currentIndex = codeEnd + 1
                continue
            }
        }
        
        // 处理删除线 ~~text~~
        val strikeStart = text.indexOf("~~", currentIndex)
        if (strikeStart != -1) {
            val strikeEnd = text.indexOf("~~", strikeStart + 2)
            if (strikeEnd != -1 && strikeEnd > strikeStart) {
                // 添加删除线前的普通文本
                append(text.substring(currentIndex, strikeStart))
                // 添加删除线文本
                withStyle(style = SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                    append(text.substring(strikeStart + 2, strikeEnd))
                }
                currentIndex = strikeEnd + 2
                continue
            }
        }
        
        // 没有更多标记，添加剩余文本
        append(text.substring(currentIndex))
        break
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AIScreen(viewModel: AIViewModel = viewModel()) {
    val context = LocalContext.current
    val sessions by viewModel.sessions.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val currentSessionId by viewModel.currentSessionId.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSending by viewModel.isSending.collectAsState()
    val selectedFiles by viewModel.selectedFiles.collectAsState()
    val error by viewModel.error.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var showRenameDialog by remember { mutableStateOf<AISession?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<AISession?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        uris.forEach { viewModel.addSelectedFile(it) }
    }

    LaunchedEffect(Unit) {
        viewModel.fetchSessions()
        viewModel.fetchHistory(null)
    }

    val currentTitle = sessions.find { it.sessionId == currentSessionId }?.sessionTitle ?: "AI 助手"

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(300.dp)) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("历史会话", style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = { scope.launch { drawerState.close() } }) {
                        Icon(Icons.Default.Close, contentDescription = null)
                    }
                }
                HorizontalDivider()
                
                NavigationDrawerItem(
                    label = { Text("新建会话", fontWeight = FontWeight.Bold) },
                    selected = false,
                    onClick = {
                        viewModel.createSession { newId ->
                            viewModel.fetchHistory(newId)
                            scope.launch { drawerState.close() }
                        }
                    },
                    icon = { Icon(Icons.Default.AddComment, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(sessions) { session ->
                        var showMenu by remember { mutableStateOf(false) }
                        
                        Box(modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)) {
                            Surface(
                                shape = RoundedCornerShape(24.dp), // 使用具体的形状代替不确定的引用
                                color = if (session.sessionId == currentSessionId) 
                                    MaterialTheme.colorScheme.secondaryContainer 
                                else Color.Transparent,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .combinedClickable(
                                        onClick = {
                                            viewModel.fetchHistory(session.sessionId)
                                            scope.launch { drawerState.close() }
                                        },
                                        onLongClick = { showMenu = true }
                                    )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.ChatBubbleOutline, 
                                        contentDescription = null,
                                        tint = if (session.sessionId == currentSessionId) 
                                            MaterialTheme.colorScheme.primary 
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            session.sessionTitle, 
                                            maxLines = 1,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (session.sessionId == currentSessionId) 
                                                MaterialTheme.colorScheme.onSecondaryContainer 
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        session.firstMessage?.let {
                                            Text(
                                                it, 
                                                style = MaterialTheme.typography.bodySmall, 
                                                maxLines = 1, 
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                    IconButton(onClick = { showMenu = true }) {
                                        Icon(Icons.Default.MoreVert, contentDescription = "更多")
                                    }
                                }
                            }
                            
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("重命名") },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        showRenameDialog = session
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("删除", color = Color.Red) },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) },
                                    onClick = {
                                        showMenu = false
                                        showDeleteConfirm = session
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(currentTitle) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.createSession { viewModel.fetchHistory(it) } }) {
                            Icon(Icons.Default.AddCircleOutline, contentDescription = "New Chat", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                )
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    MessageList(
                        messages = messages,
                        modifier = Modifier.weight(1f)
                    )

                    if (selectedFiles.isNotEmpty()) {
                        FilePreviewBar(
                            uris = selectedFiles,
                            onRemove = { viewModel.removeSelectedFile(it) }
                        )
                    }

                    ChatInput(
                        onSendMessage = { content ->
                            viewModel.sendMessage(context, content)
                        },
                        onPickFile = {
                            filePickerLauncher.launch("*/*")
                        },
                        isLoading = isSending,
                        enabled = currentSessionId != null
                    )
                }

                if (isLoading && messages.isEmpty()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }

    showRenameDialog?.let { session ->
        var newTitle by remember { mutableStateOf(session.sessionTitle) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = null },
            title = { Text("重命名会话") },
            text = {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateSessionTitle(session.sessionId, newTitle)
                    showRenameDialog = null
                }) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = null }) { Text("取消") }
            }
        )
    }

    showDeleteConfirm?.let { session ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("确认删除") },
            text = { Text("确定要删除会话 \"${session.sessionTitle}\" 吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSession(session.sessionId) {
                            scope.launch { drawerState.open() }
                        }
                        showDeleteConfirm = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) { Text("取消") }
            }
        )
    }

    error?.let {
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("提示") },
            text = { Text(it) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) { Text("确定") }
            }
        )
    }
}

@Composable
fun FilePreviewBar(uris: List<Uri>, onRemove: (Uri) -> Unit) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(uris) { uri ->
            Box(modifier = Modifier.size(60.dp)) {
                AsyncImage(
                    model = uri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)).border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                IconButton(
                    onClick = { onRemove(uri) },
                    modifier = Modifier.size(20.dp).align(Alignment.TopEnd).background(Color.Black.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                }
            }
        }
    }
}

@Composable
fun MessageList(messages: List<AIMessage>, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(messages) { message ->
            MessageItem(message)
        }
    }
}

@Composable
fun MessageItem(message: AIMessage) {
    val isUser = message.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(8.dp))
        }

        Column(horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
            Surface(
                color = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ),
                modifier = Modifier.widthIn(max = 280.dp)
            ) {
                if (isUser) {
                    // 用户消息直接显示普通文本
                    Text(
                        text = message.content,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        fontSize = 15.sp
                    )
                } else {
                    // AI 消息使用 Markdown 格式化显示
                    Text(
                        text = markdownToAnnotatedString(message.content),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        fontSize = 15.sp
                    )
                }
            }
            
            if (!message.imageUrls.isNullOrEmpty()) {
                Spacer(Modifier.height(4.dp))
                message.imageUrls.forEach { uri ->
                    AsyncImage(
                        model = uri,
                        contentDescription = null,
                        modifier = Modifier.size(150.dp).clip(RoundedCornerShape(8.dp))
                    )
                }
            }
        }

        if (isUser) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.White)
            }
        }
    }
}

@Composable
fun ChatInput(onSendMessage: (String) -> Unit, onPickFile: () -> Unit, isLoading: Boolean, enabled: Boolean) {
    var text by remember { mutableStateOf("") }

    Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp).navigationBarsPadding().imePadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPickFile, enabled = enabled && !isLoading) {
                Icon(Icons.Default.Add, contentDescription = "Add File", tint = MaterialTheme.colorScheme.primary)
            }
            
            TextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text(if (enabled) "输入消息..." else "请先选择会话") },
                enabled = enabled && !isLoading,
                maxLines = 4,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                )
            )
            
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp).padding(4.dp), strokeWidth = 2.dp)
            } else {
                IconButton(
                    onClick = {
                        onSendMessage(text)
                        text = ""
                    },
                    enabled = enabled && (text.isNotBlank() || !isLoading)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (enabled && text.isNotBlank()) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }
            }
        }
    }
}
