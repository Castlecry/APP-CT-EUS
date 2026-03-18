package com.example.cteus.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cteus.ui.viewmodel.UserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaseManagementScreen(
    onBack: () -> Unit,
    viewModel: UserViewModel = viewModel()
) {
    var caseName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("未选择文件") }
    
    val isUploading by viewModel.isCaseUploading.collectAsState()
    val uploadProgress by viewModel.caseUploadStatus.collectAsState()
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedFileUri = it
            fileName = it.path?.substringAfterLast("/") ?: "已选择文件"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("病例数据管理") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = caseName,
                onValueChange = { caseName = it },
                label = { Text("病例名称") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("描述说明") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { launcher.launch("*/*") }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("选择病例压缩包", style = MaterialTheme.typography.titleMedium)
                        Text(fileName, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (isUploading) {
                CircularProgressIndicator()
                Text(uploadProgress, modifier = Modifier.padding(top = 8.dp))
            } else {
                Button(
                    onClick = {
                        if (caseName.isNotBlank() && selectedFileUri != null) {
                            viewModel.uploadCase(context, caseName, description, selectedFileUri!!)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = caseName.isNotBlank() && selectedFileUri != null
                ) {
                    Text("提交并开始处理")
                }
            }
        }
    }
}
