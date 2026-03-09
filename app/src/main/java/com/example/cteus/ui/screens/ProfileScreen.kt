package com.example.cteus.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.cteus.ui.viewmodel.UserViewModel

@Composable
fun ProfileScreen(viewModel: UserViewModel) {
    val user by viewModel.userProfile.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "个人主页",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        user?.let { profile ->
            AsyncImage(
                model = profile.avatarUrl ?: "https://via.placeholder.com/150",
                contentDescription = "头像",
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(text = profile.nickname ?: profile.username, style = MaterialTheme.typography.titleLarge)
            Text(text = "@${profile.username}", style = MaterialTheme.typography.bodyMedium)

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ProfileItem(label = "简介", value = profile.bio ?: "暂无简介")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    ProfileItem(label = "性别", value = when(profile.gender) {
                        1 -> "男"
                        2 -> "女"
                        else -> "保密"
                    })
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    ProfileItem(label = "邮箱", value = profile.email ?: "未绑定")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    ProfileItem(label = "手机", value = profile.phone ?: "未绑定")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    ProfileItem(label = "加入时间", value = profile.createdAt)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { viewModel.logout() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("退出登录")
            }
        } ?: run {
            CircularProgressIndicator()
            Text("正在加载资料...", modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
fun ProfileItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}
