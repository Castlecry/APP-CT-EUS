package com.example.cteus.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.cteus.data.model.User
import com.example.cteus.ui.viewmodel.KnowledgeCardViewModel
import com.example.cteus.ui.viewmodel.UserViewModel
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

@Composable
fun ProfileScreen(viewModel: UserViewModel = viewModel(), knowledgeCardViewModel: KnowledgeCardViewModel = viewModel()) {
    val user by viewModel.userProfile.collectAsState()
    val error by viewModel.error.collectAsState()
    var currentScreen by remember { mutableStateOf("profile") }

    when (currentScreen) {
        "edit" -> {
            if (user != null) {
                EditProfileScreen(
                    user = user!!,
                    onSave = { nickname, gender, bio, phone, email ->
                        viewModel.updateProfile(nickname, gender, bio, phone, email)
                        currentScreen = "profile"
                    },
                    onCancel = { currentScreen = "profile" }
                )
            }
        }
        "case_management" -> {
            CaseManagementScreen(onBack = { currentScreen = "profile" }, viewModel = viewModel)
        }
        "case_list" -> {
            CaseListScreen(
                onBack = { currentScreen = "profile" },
                onAddCase = { currentScreen = "case_management" },
                viewModel = viewModel
            )
        }
        "knowledge_cards" -> {
            KnowledgeCardScreen(viewModel = knowledgeCardViewModel)
        }
        else -> {
            ProfileContent(
                user = user,
                viewModel = viewModel,
                onLogout = { viewModel.logout() },
                onEditClick = { currentScreen = "edit" },
                onCaseManagementClick = { currentScreen = "case_management" },
                onCaseListClick = { currentScreen = "case_list" },
                onKnowledgeCardsClick = { currentScreen = "knowledge_cards" }
            )
        }
    }

    error?.let {
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) { Text("确定") }
            },
            text = { Text(it) }
        )
    }
}

@Composable
fun ProfileContent(
    user: User?,
    viewModel: UserViewModel,
    onLogout: () -> Unit,
    onEditClick: () -> Unit,
    onCaseManagementClick: () -> Unit,
    onCaseListClick: () -> Unit,
    onKnowledgeCardsClick: () -> Unit
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val file = context.createTempFileFromUri(it)
            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
            // 后端接口要求的参数名是 "file"，不是 "avatar"
            val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
            viewModel.uploadAvatar(body)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "个人主页", style = MaterialTheme.typography.headlineMedium)
            IconButton(onClick = onEditClick) {
                Icon(Icons.Default.Edit, contentDescription = "编辑资料")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        user?.let { profile ->
            Box {
                AsyncImage(
                    model = profile.avatarUrl ?: "https://via.placeholder.com/150",
                    contentDescription = "头像",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                FilledTonalIconButton(
                    onClick = { launcher.launch("image/*") },
                    modifier = Modifier.align(Alignment.BottomEnd).size(30.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "换头像", modifier = Modifier.size(14.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(text = profile.nickname ?: profile.username, style = MaterialTheme.typography.titleLarge)
            Text(text = "@${profile.username}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
            
            Spacer(modifier = Modifier.height(32.dp))

            // 功能列表
            Card(modifier = Modifier.fillMaxWidth()) {
                Column {
                    MenuRow(
                        icon = Icons.Default.UploadFile,
                        title = "病例数据管理",
                        onClick = onCaseManagementClick
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    MenuRow(
                        icon = Icons.Default.History,
                        title = "病例历史记录",
                        onClick = onCaseListClick
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    MenuRow(
                        icon = Icons.Default.Book,
                        title = "知识卡片",
                        onClick = onKnowledgeCardsClick
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ProfileItem(label = "简介", value = profile.bio ?: "暂无简介")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    ProfileItem(label = "手机", value = profile.phone ?: "未绑定")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    ProfileItem(label = "邮箱", value = profile.email ?: "未绑定")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("退出登录")
            }
        } ?: run {
            CircularProgressIndicator(modifier = Modifier.padding(top = 100.dp))
        }
    }
}

@Composable
fun MenuRow(icon: ImageVector, title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
    }
}

@Composable
fun EditProfileScreen(
    user: User,
    onSave: (String?, Int?, String?, String?, String?) -> Unit,
    onCancel: () -> Unit
) {
    var nickname by remember { mutableStateOf(user.nickname ?: "") }
    var bio by remember { mutableStateOf(user.bio ?: "") }
    var gender by remember { mutableIntStateOf(user.gender) }
    var phone by remember { mutableStateOf(user.phone ?: "") }
    var email by remember { mutableStateOf(user.email ?: "") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("编辑个人资料", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(value = nickname, onValueChange = { nickname = it }, label = { Text("昵称") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = bio, onValueChange = { bio = it }, label = { Text("个性签名") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        
        Text("性别", style = MaterialTheme.typography.labelLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = gender == 1, onClick = { gender = 1 })
            Text("男")
            RadioButton(selected = gender == 2, onClick = { gender = 2 })
            Text("女")
            RadioButton(selected = gender == 0, onClick = { gender = 0 })
            Text("保密")
        }
        
        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("手机号") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("邮箱") }, modifier = Modifier.fillMaxWidth())
        
        Spacer(modifier = Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("取消") }
            Button(onClick = { onSave(nickname, gender, bio, phone, email) }, modifier = Modifier.weight(1f)) { Text("保存") }
        }
    }
}

@Composable
fun ProfileItem(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

fun android.content.Context.createTempFileFromUri(uri: Uri): File {
    val inputStream = contentResolver.openInputStream(uri)
    val file = File(cacheDir, "temp_${System.currentTimeMillis()}")
    val outputStream = FileOutputStream(file)
    inputStream?.copyTo(outputStream)
    inputStream?.close()
    outputStream.close()
    return file
}
