package com.example.cteus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cteus.ui.screens.AIScreen
import com.example.cteus.ui.screens.ExamScreen
import com.example.cteus.ui.screens.LoginScreen
import com.example.cteus.ui.screens.Model3DScreen
import com.example.cteus.ui.screens.ProfileScreen
import com.example.cteus.ui.theme.CTEUSTheme
import com.example.cteus.ui.viewmodel.KnowledgeCardViewModel
import com.example.cteus.ui.viewmodel.UserViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CTEUSTheme {
                MainContent()
            }
        }
    }
}

@Composable
fun MainContent(viewModel: UserViewModel = viewModel()) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()

    when (isLoggedIn) {
        true -> CTEUSApp(viewModel)
        false -> LoginScreen(viewModel)
        null -> {
            // Loading state
            Scaffold { innerPadding ->
                Text("正在检查登录状态...", modifier = Modifier.padding(innerPadding))
            }
        }
    }
}

@Composable
fun CTEUSApp(viewModel: UserViewModel) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            imageVector = it.icon,
                            contentDescription = it.label
                        )
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            val modifier = Modifier.padding(innerPadding)
            when (currentDestination) {
                AppDestinations.HOME -> ExamScreen()
                AppDestinations.MODEL_3D -> Model3DScreen()
                AppDestinations.AI_ASSISTANT -> AIScreen()
                AppDestinations.PROFILE -> ProfileScreen(viewModel, viewModel())
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    HOME("知识训练", Icons.Default.Home),
    MODEL_3D("3D模型", Icons.Default.ViewInAr),
    AI_ASSISTANT("AI助手", Icons.Default.AutoAwesome),
    PROFILE("个人主页", Icons.Default.AccountCircle),
}
