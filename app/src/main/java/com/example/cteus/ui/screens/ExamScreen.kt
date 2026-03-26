package com.example.cteus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.*
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cteus.data.model.ExamPaper
import com.example.cteus.data.model.ExamQuestion
import com.example.cteus.data.model.ExamResult
import com.example.cteus.data.model.StartExamData
import com.example.cteus.ui.viewmodel.ExamViewModel

@Composable
fun ExamScreen(viewModel: ExamViewModel = viewModel()) {
    val examList by viewModel.examList.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val examDetail by viewModel.examDetail.collectAsState()
    val examResult by viewModel.examResult.collectAsState()
    val startExamData by viewModel.startExamData.collectAsState()
    val userLearningAnalysis by viewModel.userLearningAnalysis.collectAsState()

    var showSuccessSnackbar by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    var showUnfinishedDialog by remember { mutableStateOf(false) }
    var pendingExamId by remember { mutableStateOf<Int?>(null) }
    var showHistoryScreen by remember { mutableStateOf(false) }
    var showAnalysisScreen by remember { mutableStateOf(false) }
    var currentExamId by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(successMessage) {
        successMessage?.let {
            showSuccessSnackbar = true
            snackbarHostState.showSnackbar(it)
            viewModel.clearSuccessMessage()
            showSuccessSnackbar = false
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadExamList()
        viewModel.loadUserLearningAnalysis()
    }

    SnackbarHost(hostState = snackbarHostState)

    if (startExamData != null && startExamData!!.hasUnfinishedAttempt && !showUnfinishedDialog) {
        UnfinishedAttemptDialog(
            startExamData = startExamData!!,
            onContinue = {
                viewModel.continueExam(pendingExamId!!, startExamData!!.unfinishedAttemptId!!)
                showUnfinishedDialog = false
            },
            onDeleteAndRestart = {
                viewModel.deleteAttempt(startExamData!!.unfinishedAttemptId!!) {
                    viewModel.startExam(pendingExamId!!)
                }
            },
            onDismiss = {
                pendingExamId = null
                viewModel.clearExamDetail()
                showUnfinishedDialog = false
            }
        )
    } else if (examResult != null) {
        ExamResultScreen(
            result = examResult!!,
            onBack = { viewModel.clearExamDetail() }
        )
    } else if (examDetail != null) {
        ExamDetailScreen(
            viewModel = viewModel,
            onBack = { viewModel.clearExamDetail() }
        )
    } else if (showHistoryScreen && currentExamId != null) {
        ExamHistoryScreen(
            viewModel = viewModel,
            examId = currentExamId!!,
            onBack = {
                showHistoryScreen = false
                currentExamId = null
                viewModel.clearHistoryData()
            }
        )
    } else if (showAnalysisScreen && currentExamId != null) {
        ExamAnalysisScreen(
            viewModel = viewModel,
            examId = currentExamId!!,
            onBack = {
                showAnalysisScreen = false
                currentExamId = null
                viewModel.clearHistoryData()
            }
        )
    } else {
        ExamListScreen(
            examList = examList,
            isLoading = isLoading,
            error = error,
            userLearningAnalysis = userLearningAnalysis,
            onRetry = { viewModel.loadExamList() },
            onExamClick = { examId ->
                pendingExamId = examId
                viewModel.loadExamDetail(examId)
            },
            onUpdateTitle = { examId, newTitle -> viewModel.updateExamTitle(examId, newTitle) },
            onDeleteExam = { examId -> viewModel.deleteExam(examId) },
            onViewHistory = { examId ->
                currentExamId = examId
                showHistoryScreen = true
            },
            onViewAnalysis = { examId ->
                currentExamId = examId
                showAnalysisScreen = true
            },
            onErrorDismiss = { viewModel.clearError() }
        )
    }
}

@Composable
fun UnfinishedAttemptDialog(
    startExamData: StartExamData,
    onContinue: () -> Unit,
    onDeleteAndRestart: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("检测到未完成的答卷") },
        text = {
            Column {
                Text("您有一套未完成的答卷，是否继续作答？")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "试卷：${startExamData.examTitle}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "开始时间：${startExamData.startTime.replace("T", " ")}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onContinue) {
                Text("继续作答")
            }
        },
        dismissButton = {
            TextButton(onClick = onDeleteAndRestart) {
                Text("删除并重新开始", color = MaterialTheme.colorScheme.error)
            }
        }
    )
}

@Composable
fun ExamListScreen(
    examList: List<ExamPaper>,
    isLoading: Boolean,
    error: String?,
    userLearningAnalysis: com.example.cteus.data.model.UserLearningAnalysisData?,
    onRetry: () -> Unit,
    onExamClick: (Int) -> Unit,
    onUpdateTitle: (Int, String) -> Unit,
    onDeleteExam: (Int) -> Unit,
    onViewHistory: (Int) -> Unit,
    onViewAnalysis: (Int) -> Unit,
    onErrorDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "知识训练",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "选择试卷开始练习",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            error != null -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onRetry) {
                        Text("重试")
                    }
                }
            }
            examList.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Quiz,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "当前还没有试卷哦",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(examList) { exam ->
                        ExamPaperCard(
                            exam = exam,
                            onClick = { onExamClick(exam.examId) },
                            onUpdateTitle = { newTitle -> onUpdateTitle(exam.examId, newTitle) },
                            onDeleteExam = { onDeleteExam(exam.examId) },
                            onViewHistory = { onViewHistory(exam.examId) },
                            onViewAnalysis = { onViewAnalysis(exam.examId) }
                        )
                    }
                }

                if (userLearningAnalysis != null) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "我的综合评价",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${userLearningAnalysis.totalExamsTaken}",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text("作答试卷", fontSize = 12.sp)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${userLearningAnalysis.totalQuestionsAnswered}",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text("总题数", fontSize = 12.sp)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${userLearningAnalysis.overallAverageScore}",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text("平均分", fontSize = 12.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "能力维度",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            userLearningAnalysis.abilityRadar.forEach { ability ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(ability.dimensionName, fontSize = 13.sp)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "${ability.percentage}%",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = when (ability.level) {
                                                "excellent" -> MaterialTheme.colorScheme.primary
                                                "good" -> MaterialTheme.colorScheme.tertiary
                                                "fair" -> MaterialTheme.colorScheme.secondary
                                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )
                                        Text(
                                            text = " (${ability.score})",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "整体评估",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = userLearningAnalysis.overallAssessment,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (userLearningAnalysis.comprehensiveSuggestions.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "学习建议",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                userLearningAnalysis.comprehensiveSuggestions.take(3).forEach { suggestion ->
                                    Row(
                                        modifier = Modifier.padding(vertical = 2.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text("• ", fontSize = 13.sp)
                                        Text(
                                            text = suggestion,
                                            fontSize = 13.sp,
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
    }
}

@Composable
fun ExamPaperCard(
    exam: ExamPaper,
    onClick: () -> Unit,
    onUpdateTitle: (String) -> Unit,
    onDeleteExam: () -> Unit,
    onViewHistory: () -> Unit,
    onViewAnalysis: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var editedTitle by remember { mutableStateOf(exam.examTitle) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exam.examTitle,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "总分: ${exam.totalScore}分",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "题目数: ${exam.questionCount}题",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "创建时间: ${exam.createdAt.replace("T", " ")}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "更多")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("修改名称") },
                        onClick = {
                            showMenu = false
                            editedTitle = exam.examTitle
                            showEditDialog = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("删除试卷", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showMenu = false
                            showDeleteDialog = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("作答历史") },
                        onClick = {
                            showMenu = false
                            onViewHistory()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("综合分析") },
                        onClick = {
                            showMenu = false
                            onViewAnalysis()
                        }
                    )
                }
            }
        }
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("修改试卷名称") },
            text = {
                OutlinedTextField(
                    value = editedTitle,
                    onValueChange = { editedTitle = it },
                    label = { Text("试卷名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (editedTitle.isNotBlank()) {
                            onUpdateTitle(editedTitle)
                            showEditDialog = false
                        }
                    }
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除试卷") },
            text = { Text("确定要删除这份试卷吗？删除后无法恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteExam()
                        showDeleteDialog = false
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamDetailScreen(
    viewModel: ExamViewModel,
    onBack: () -> Unit
) {
    val examDetail by viewModel.examDetail.collectAsState()
    val userAnswers by viewModel.userAnswers.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val isSubmitting by viewModel.isSubmitting.collectAsState()
    val currentAttemptId by viewModel.currentAttemptId.collectAsState()

    val detail = examDetail ?: return

    var showSubmitDialog by remember { mutableStateOf(false) }
    var showSavedDialog by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }
    val hasStartedExam = currentAttemptId != null || userAnswers.isNotEmpty()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(detail.examTitle, maxLines = 1) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                }
            },
            actions = {
                if (hasStartedExam) {
                    TextButton(
                        onClick = { viewModel.saveAnswer { showSavedDialog = true } },
                        enabled = !isSaving && !isSubmitting
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("暂存")
                        }
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        )

        if (isLoading || isSubmitting) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    if (isSubmitting) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("正在提交...")
                    }
                }
            }
        } else if (!hasStartedExam) {
            // 显示试卷详情预览和开始作答按钮
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Text(
                    text = detail.examTitle,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("总分：${detail.totalScore}分")
                            Text("题目数量：${detail.questionCount}道")
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "试卷结构",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                detail.sections.forEach { section ->
                    var isExpanded by remember { mutableStateOf(false) }
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = section.sectionName,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "本部分共 ${section.questions.size} 道题，总分 ${section.sectionScore} 分",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                IconButton(onClick = { isExpanded = !isExpanded }) {
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = if (isExpanded) "收起" else "展开"
                                    )
                                }
                            }
                            
                            if (isExpanded) {
                                Spacer(modifier = Modifier.height(12.dp))
                                section.questions.forEachIndexed { index, question ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp)
                                        ) {
                                            Text(
                                                text = "${index + 1}. ${question.questionText}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                            if (!question.options.isNullOrEmpty()) {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                question.options.forEach { option ->
                                                    Text(
                                                        text = "  • $option",
                                                        style = MaterialTheme.typography.bodySmall,
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
                }
            }

            // 开始作答按钮
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = { viewModel.startExam(detail.examId) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    Text("开始作答")
                }
            }
        } else {
            // 继续作答界面
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                detail.sections.forEachIndexed { sectionIndex, section ->
                    Text(
                        text = section.sectionName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    section.questions.forEachIndexed { index, question ->
                        QuestionCard(
                            question = question,
                            questionNumber = index + 1,
                            selectedAnswer = userAnswers[question.questionId],
                            onAnswerSelected = { answer ->
                                viewModel.setAnswer(question.questionId, answer)
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = { showSubmitDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = userAnswers.size == detail.questionCount && !isSubmitting
                ) {
                    Text(
                        if (userAnswers.size == detail.questionCount) "提交试卷"
                        else "请完成所有题目 (${userAnswers.size}/${detail.questionCount})"
                    )
                }
            }
        }
    }

    if (showSavedDialog) {
        AlertDialog(
            onDismissRequest = { showSavedDialog = false },
            title = { Text("答案已保存") },
            text = { Text("您的答案已保存，可以继续作答。") },
            confirmButton = {
                TextButton(onClick = { showSavedDialog = false }) {
                    Text("继续作答")
                }
            }
        )
    }

    if (validationError != null) {
        AlertDialog(
            onDismissRequest = { validationError = null },
            title = { Text("提示") },
            text = { Text(validationError!!) },
            confirmButton = {
                TextButton(onClick = { validationError = null }) {
                    Text("确定")
                }
            }
        )
    }

    if (showSubmitDialog) {
        AlertDialog(
            onDismissRequest = { showSubmitDialog = false },
            title = { Text("确认提交") },
            text = { Text("确定要提交试卷吗？提交后将无法修改答案。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSubmitDialog = false
                        viewModel.submitExam(
                            onSuccess = { response ->
                                onBack()
                            },
                            onValidationError = { error ->
                                validationError = error
                            }
                        )
                    }
                ) {
                    Text("确认提交")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSubmitDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun QuestionCard(
    question: ExamQuestion,
    questionNumber: Int,
    selectedAnswer: Any?,
    onAnswerSelected: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "第${questionNumber}题. ${question.questionText}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (!question.options.isNullOrEmpty()) {
                val isMultipleChoice = question.questionType == "multiple_choice"
                val selectedOptions = if (selectedAnswer is List<*>) {
                    (selectedAnswer as List<*>).map { it.toString() }.toSet()
                } else if (selectedAnswer != null) {
                    selectedAnswer.toString().split(",").map { it.trim() }.toSet()
                } else {
                    emptySet()
                }

                question.options.forEach { option ->
                    val optionLetter = option.substringBefore(".")
                    val isSelected = optionLetter in selectedOptions

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { 
                                if (isMultipleChoice) {
                                    val newOptions = if (isSelected) {
                                        selectedOptions - optionLetter
                                    } else {
                                        selectedOptions + optionLetter
                                    }
                                    onAnswerSelected(newOptions.joinToString(","))
                                } else {
                                    onAnswerSelected(optionLetter)
                                }
                            }
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .border(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isMultipleChoice) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { 
                                    val newOptions = if (isSelected) {
                                        selectedOptions - optionLetter
                                    } else {
                                        selectedOptions + optionLetter
                                    }
                                    onAnswerSelected(newOptions.joinToString(","))
                                }
                            )
                        } else {
                            RadioButton(
                                selected = isSelected,
                                onClick = { onAnswerSelected(optionLetter) }
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = option,
                            fontSize = 14.sp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            } else {
                OutlinedTextField(
                    value = if (selectedAnswer is List<*>) {
                        (selectedAnswer as List<*>).joinToString(",")
                    } else {
                        selectedAnswer?.toString() ?: ""
                    },
                    onValueChange = { onAnswerSelected(it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("请输入答案") },
                    placeholder = { Text("在此输入您的答案...") }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamHistoryScreen(
    viewModel: ExamViewModel,
    examId: Int,
    onBack: () -> Unit
) {
    val historyData by viewModel.historyExamData.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val attemptAnalysisData by viewModel.attemptAnalysisData.collectAsState()

    var selectedAttemptId by remember { mutableStateOf<Int?>(null) }
    var showAnalysisDialog by remember { mutableStateOf(false) }
    var selectedAttemptIndex by remember { mutableStateOf(0) }

    LaunchedEffect(examId) {
        viewModel.loadExamHistory(examId)
    }

    val history = historyData

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("作答历史") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        )

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (history != null) {
            if (history.attempts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无作答记录",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    TabRow(
                        selectedTabIndex = selectedAttemptIndex,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        history.attempts.forEachIndexed { index, attempt ->
                            Tab(
                                selected = selectedAttemptIndex == index,
                                onClick = { selectedAttemptIndex = index },
                                text = {
                                    Column {
                                        Text(
                                            text = "第 ${index + 1} 次",
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "${attempt.totalScore}分",
                                            fontSize = 12.sp,
                                            color = if (attempt.totalScore >= 60) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            )
                        }
                    }

                    val currentAttempt = history.attempts[selectedAttemptIndex]
                    val answersMap = currentAttempt.answers?.associate { it.questionId to it.userAnswer } ?: emptyMap()

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        text = history.examTitle,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "总分: ${currentAttempt.totalScore}分",
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "用时: ${currentAttempt.durationSeconds}秒",
                                            fontSize = 14.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "提交时间: ${currentAttempt.submitTime?.replace("T", " ") ?: "未提交"}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            viewModel.loadAttemptAnalysis(currentAttempt.attemptId)
                                            showAnalysisDialog = true
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("查看本次测评分析")
                                    }
                                }
                            }
                        }

                        history.sections.forEach { section ->
                            item {
                                Text(
                                    text = section.sectionName,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }

                            section.questions.forEach { question ->
                                item {
                                    val userAnswer = answersMap[question.questionId]
                                    val correctAnswer = question.correctAnswer

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (userAnswer != null) {
                                                val isCorrect = if (question.questionType == "multiple_choice") {
                                                    val selectedOptions = if (userAnswer is List<*>) {
                                                        (userAnswer as List<*>).map { it.toString() }
                                                    } else {
                                                        userAnswer.toString().split(",")
                                                    }
                                                    val correctOptions = correctAnswer?.split(",")?.toSet() ?: emptySet()
                                                    val userOptions = selectedOptions.map { it.trim() }.toSet()
                                                    userOptions == correctOptions
                                                } else {
                                                    userAnswer.toString().trim() == correctAnswer
                                                }
                                                if (isCorrect) MaterialTheme.colorScheme.primaryContainer
                                                else MaterialTheme.colorScheme.errorContainer
                                            } else {
                                                MaterialTheme.colorScheme.surface
                                            }
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp)
                                        ) {
                                            Text(
                                                text = "${question.questionText}",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            
                                            if (question.options != null) {
                                                val selectedOptions = if (userAnswer is List<*>) {
                                                    (userAnswer as List<*>).map { it.toString() }
                                                } else if (userAnswer != null) {
                                                    userAnswer.toString().split(",")
                                                } else {
                                                    emptyList()
                                                }
                                                question.options.forEach { option ->
                                                    val optionLetter = option.substringBefore(".")
                                                    val isCorrect = correctAnswer?.contains(optionLetter) == true
                                                    val isSelected = optionLetter in selectedOptions
                                                    
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(vertical = 2.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = if (isCorrect) "✓" else if (isSelected) "✗" else "  ",
                                                            color = if (isCorrect) MaterialTheme.colorScheme.primary
                                                                   else if (isSelected) MaterialTheme.colorScheme.error
                                                                   else Color.Transparent,
                                                            modifier = Modifier.width(20.dp)
                                                        )
                                                        Text(
                                                            text = option,
                                                            fontSize = 14.sp,
                                                            color = if (isSelected) {
                                                                if (isCorrect) MaterialTheme.colorScheme.primary
                                                                else MaterialTheme.colorScheme.error
                                                            } else if (isCorrect) {
                                                                MaterialTheme.colorScheme.primary
                                                            } else {
                                                                MaterialTheme.colorScheme.onSurface
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "正确答案: ${correctAnswer ?: "暂无"}",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = "您的答案: ${if (userAnswer != null) {
                                                    if (userAnswer is List<*>) {
                                                        (userAnswer as List<*>).joinToString(",")
                                                    } else {
                                                        userAnswer.toString()
                                                    }
                                                } else "未作答"}",
                                                fontSize = 12.sp,
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
        }
    }

    if (showAnalysisDialog && attemptAnalysisData != null) {
        AttemptAnalysisDialog(
            analysisData = attemptAnalysisData!!,
            onDismiss = {
                showAnalysisDialog = false
                selectedAttemptId = null
            }
        )
    }
}

@Composable
fun AttemptAnalysisDialog(
    analysisData: com.example.cteus.data.model.AttemptAnalysisData,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("测评分析")
                Text(
                    text = analysisData.examTitle,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${analysisData.totalScore}/${analysisData.maxScore}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "总分",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${analysisData.scorePercentage}%",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (analysisData.scorePercentage >= 60) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "得分率",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${analysisData.timeMetrics.durationSeconds}秒",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "用时",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "成绩分布",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("客观题: ${analysisData.scoreBreakdown.objectiveScore}/${analysisData.scoreBreakdown.objectiveMaxScore}")
                            Text("主观题: ${analysisData.scoreBreakdown.subjectiveScore}/${analysisData.scoreBreakdown.subjectiveMaxScore}")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "题型分析",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                analysisData.questionTypeAnalysis.forEach { type ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (type.accuracyRate >= 0.8) MaterialTheme.colorScheme.primaryContainer
                                            else if (type.accuracyRate >= 0.5) MaterialTheme.colorScheme.tertiaryContainer
                                            else MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = when (type.questionType) {
                                        "single_choice" -> "单选题"
                                        "multiple_choice" -> "多选题"
                                        "fill_in_blank" -> "填空题"
                                        "short_answer" -> "简答题"
                                        else -> type.questionType
                                    },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${type.correctCount}/${type.questionCount} 正确",
                                    fontSize = 13.sp,
                                    color = if (type.accuracyRate >= 0.8) MaterialTheme.colorScheme.primary
                                            else if (type.accuracyRate >= 0.5) MaterialTheme.colorScheme.tertiary
                                            else MaterialTheme.colorScheme.error
                                )
                            }
                            Text(
                                text = "得分率: ${(type.avgScoreRate * 100).toInt()}%",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = type.reason,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (analysisData.abilityDimensions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "能力维度分析",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    analysisData.abilityDimensions.forEach { dimension ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = when (dimension.level) {
                                    "excellent" -> MaterialTheme.colorScheme.primaryContainer
                                    "good" -> MaterialTheme.colorScheme.secondaryContainer
                                    "fair" -> MaterialTheme.colorScheme.tertiaryContainer
                                    else -> MaterialTheme.colorScheme.surface
                                }
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = dimension.dimensionName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "${dimension.score}/${dimension.maxScore} (${dimension.percentage}%)",
                                        fontSize = 13.sp,
                                        color = when (dimension.level) {
                                            "excellent" -> MaterialTheme.colorScheme.primary
                                            "good" -> MaterialTheme.colorScheme.secondary
                                            "fair" -> MaterialTheme.colorScheme.tertiary
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }
                                Text(
                                    text = dimension.reason,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                if (analysisData.weaknesses.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "薄弱环节",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    analysisData.weaknesses.forEach { weakness ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = when (weakness.severity) {
                                    "high" -> MaterialTheme.colorScheme.errorContainer
                                    "medium" -> MaterialTheme.colorScheme.tertiaryContainer
                                    else -> MaterialTheme.colorScheme.secondaryContainer
                                }
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = weakness.description,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                if (analysisData.learningSuggestions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "学习建议",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    analysisData.learningSuggestions.forEach { suggestion ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = suggestion.title,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "优先级: ${suggestion.priority}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    text = suggestion.description,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "综合评价",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = analysisData.overallAssessment,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "优势分析",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = analysisData.strengthAnalysis,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamAnalysisScreen(
    viewModel: ExamViewModel,
    examId: Int,
    onBack: () -> Unit
) {
    val analysisData by viewModel.examAnalysisData.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(examId) {
        viewModel.loadExamAnalysis(examId)
    }

    val analysis = analysisData

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("综合分析") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        )

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (analysis != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Text(
                    text = analysis.examTitle,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "成绩统计",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${analysis.scoreStatistics.totalAttempts}",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text("作答次数", fontSize = 12.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${analysis.scoreStatistics.bestScore}",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text("最高分", fontSize = 12.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${analysis.scoreStatistics.averageScore}",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("平均分", fontSize = 12.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${analysis.scoreStatistics.latestScore}",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("最近一次", fontSize = 12.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${analysis.scoreStatistics.improvementRate}%",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (analysis.scoreStatistics.improvementRate > 0) MaterialTheme.colorScheme.primary
                                           else MaterialTheme.colorScheme.error
                                )
                                Text("进步率", fontSize = 12.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "%.1f".format(analysis.scoreStatistics.stdDeviation),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("标准差", fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "趋势分析",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("趋势方向", fontSize = 14.sp)
                            Text(
                                text = when (analysis.trendAnalysis.trendDirection) {
                                    "up" -> "上升"
                                    "down" -> "下降"
                                    "stable" -> "稳定"
                                    else -> analysis.trendAnalysis.trendDirection
                                },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = when (analysis.trendAnalysis.trendDirection) {
                                    "up" -> MaterialTheme.colorScheme.primary
                                    "down" -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.tertiary
                                }
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("波动程度", fontSize = 14.sp)
                            Text(
                                text = when (analysis.trendAnalysis.volatility) {
                                    "low" -> "低"
                                    "medium" -> "中"
                                    "high" -> "高"
                                    else -> analysis.trendAnalysis.volatility
                                },
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = analysis.trendDescription,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "作答历史",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                analysis.attemptHistory.forEachIndexed { index, attempt ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "第 ${index + 1} 次",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = attempt.submitTime.replace("T", " "),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "${attempt.score}分",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (attempt.score >= 60) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "稳定性评估",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("一致性", fontSize = 14.sp)
                            Text(
                                text = when (analysis.stabilityMetrics.consistencyLevel) {
                                    "high" -> "高"
                                    "medium" -> "中"
                                    "low" -> "低"
                                    else -> analysis.stabilityMetrics.consistencyLevel
                                },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = when (analysis.stabilityMetrics.consistencyLevel) {
                                    "high" -> MaterialTheme.colorScheme.primary
                                    "medium" -> MaterialTheme.colorScheme.tertiary
                                    "low" -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("分差", fontSize = 14.sp)
                            Text("${analysis.stabilityMetrics.scoreRange}分", fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = analysis.stabilityMetrics.reason,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "整体评估",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Text(
                        text = analysis.overallAssessment,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (analysis.keyWeaknesses.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "薄弱环节",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    analysis.keyWeaknesses.forEach { weakness ->
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text("• ", fontSize = 14.sp)
                            Text(text = weakness, fontSize = 14.sp)
                        }
                    }
                }

                if (analysis.learningSuggestions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "学习建议",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    analysis.learningSuggestions.forEachIndexed { index, suggestion ->
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text("${index + 1}. ", fontSize = 14.sp)
                            Text(text = suggestion, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamResultScreen(
    result: ExamResult,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("考试结果") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (result.earnedScore >= result.totalScore * 0.6)
                        Color(0xFF4CAF50).copy(alpha = 0.1f)
                    else
                        Color(0xFFF44336).copy(alpha = 0.1f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${result.earnedScore}分",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (result.earnedScore >= result.totalScore * 0.6)
                            Color(0xFF4CAF50)
                        else
                            Color(0xFFF44336)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "满分 ${result.totalScore} 分",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "答对 ${result.correctCount}/${result.totalCount} 题",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "答题详情",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            result.results.forEachIndexed { index, questionResult ->
                ResultQuestionCard(
                    index = index + 1,
                    result = questionResult
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun ResultQuestionCard(
    index: Int,
    result: com.example.cteus.data.model.QuestionResult
) {
    val isCorrect = result.isCorrect

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCorrect)
                Color(0xFF4CAF50).copy(alpha = 0.05f)
            else
                Color(0xFFF44336).copy(alpha = 0.05f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    contentDescription = null,
                    tint = if (isCorrect) Color(0xFF4CAF50) else Color(0xFFF44336),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "第 $index 题",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isCorrect) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (!isCorrect && result.userAnswer != null) {
                Text(
                    text = "你的答案: ${result.userAnswer}",
                    fontSize = 14.sp,
                    color = Color(0xFFF44336)
                )
            }

            Text(
                text = "正确答案: ${result.correctAnswer}",
                fontSize = 14.sp,
                color = Color(0xFF4CAF50)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "解析: ${result.explanation}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
