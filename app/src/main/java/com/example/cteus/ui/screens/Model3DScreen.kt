package com.example.cteus.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cteus.data.api.RetrofitClient
import com.example.cteus.data.model.CaseItem
import com.example.cteus.data.model.OrganPoints
import com.example.cteus.ui.viewmodel.Model3DViewModel
import io.github.sceneview.Scene
import io.github.sceneview.math.Position
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import io.github.sceneview.node.SphereNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberOnGestureListener
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberMainLightNode
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import com.google.android.filament.Engine
import com.google.android.filament.MaterialInstance
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.material.setColor
import io.github.sceneview.math.colorOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Model3DScreen(
    viewModel: Model3DViewModel = viewModel()
) {
    val caseList by viewModel.caseList.collectAsState()
    val caseDetail by viewModel.caseDetail.collectAsState()
    val organPointsList by viewModel.organPointsList.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var selectedCaseId by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        viewModel.fetchCaseList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = if (selectedCaseId == null) "病例列表" else (caseDetail?.caseName ?: "3D 模型预览"),
                        style = MaterialTheme.typography.titleMedium
                    ) 
                },
                navigationIcon = {
                    if (selectedCaseId != null) {
                        IconButton(onClick = { 
                            selectedCaseId = null 
                            viewModel.fetchCaseList() 
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (selectedCaseId == null) {
                CaseListContent(
                    cases = caseList,
                    onCaseClick = {
                        selectedCaseId = it.caseId
                        viewModel.fetchCaseDetail(it.caseId)
                    }
                )
            } else {
                if (caseDetail != null && caseDetail?.status == 3) {
                    ThreeDViewerContent(
                        glbUrls = caseDetail?.models?.map { it.glbUrl }?.filter { it.isNotEmpty() } ?: emptyList(),
                        organPointsList = organPointsList,
                        modelTypes = caseDetail?.models?.map { it.modelType }?.filter { it.isNotEmpty() } ?: emptyList()
                    )
                } else if (caseDetail != null) {
                    StatusMessage(caseDetail?.statusMessage ?: "模型正在生成中，请稍候...")
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            errorMessage?.let {
                Snackbar(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("确定")
                        }
                    }
                ) {
                    Text(it)
                }
            }
        }
    }
}

@Composable
fun CaseListContent(
    cases: List<CaseItem>,
    onCaseClick: (CaseItem) -> Unit
) {
    if (cases.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("暂无病例数据", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(cases) { case ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCaseClick(case) },
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = case.caseName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "ID: ${case.caseId}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = case.description,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "创建时间: ${case.createdAt}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatusMessage(message: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun ThreeDViewerContent(
    glbUrls: List<String>,
    organPointsList: List<OrganPoints>,
    modelTypes: List<String>
) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)
    
    val cameraNode = rememberCameraNode(engine)
    val mainLightNode = rememberMainLightNode(engine) {
        intensity = 200_000f
    }

    val nodes = remember { mutableStateListOf<Node>() }
    
    var isSelectingPoint by remember { mutableStateOf(false) }
    var selectedPointIndices by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var pendingPointIndex by remember { mutableStateOf<Int?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    
    val pointColors = remember { mutableStateMapOf<Int, Color>() }
    val originalPointColors = remember { mutableStateMapOf<Int, Color>() }
    
    val sphereNodes = remember { mutableStateListOf<SphereNode>() }
    
    // 当前选中的模型类型（懒加载关键）
    var selectedModelType by remember { mutableStateOf<String?>(null) }
    
    // 底部选择栏展开/收起状态
    var isModelListExpanded by remember { mutableStateOf(true) }
    
    // 创建带颜色的球体节点
    fun updateSphereNodeColor(index: Int, color: Color) {
        if (index >= 0 && index < sphereNodes.size) {
            val sphereNode = sphereNodes[index]
            try {
                val materialInstance = sphereNode.materialInstance
                materialInstance.setColor(colorOf(color))
                Log.d("Model3DScreen", "Updated sphere $index color")
            } catch (e: Exception) {
                Log.e("Model3DScreen", "Error updating sphere color: ${e.message}")
            }
        }
    }
    
    fun createColoredSphereNode(engine: Engine, materialLoader: MaterialLoader, position: Position, color: Color): SphereNode {
        val sphereNode = SphereNode(
            engine = engine,
            radius = 0.03f,
            center = Position(0f, 0f, 0f)
        )
        sphereNode.position = position
        
        try {
            val materialInstance = materialLoader.createColorInstance(color)
            sphereNode.materialInstance = materialInstance
        } catch (e: Exception) {
            Log.e("Model3DScreen", "Error setting material color: ${e.message}")
        }
        
        return sphereNode
    }
    
    // 清理点云节点
    fun clearPointCloud() {
        sphereNodes.forEach { _ ->
            try {
                engine.destroy()
            } catch (e: Exception) {
                Log.e("Model3DScreen", "Error destroying node: ${e.message}")
            }
        }
        sphereNodes.clear()
        // 只移除 SphereNode，保留 GLB 模型
        nodes.removeAll { it is SphereNode }
        selectedPointIndices = emptySet()
        pointColors.clear()
        originalPointColors.clear()
        Log.d("Model3DScreen", "Cleared point cloud, remaining nodes: ${nodes.size}")
    }
    
    // 加载指定器官的点云
    fun loadPointCloud(organ: String) {
        // 先清理现有节点
        clearPointCloud()
        
        val organData: OrganPoints? = organPointsList.find { it.organ == organ }
        if (organData == null || organData.points.isEmpty()) {
            Log.d("Model3DScreen", "No point data for organ: $organ")
            return
        }
        
        Log.d("Model3DScreen", "Loading ${organData.points.size} points for organ: $organ")
        
        // 计算点云中心
        var minX: Double = Double.MAX_VALUE
        var maxX: Double = -Double.MAX_VALUE
        var minY: Double = Double.MAX_VALUE
        var maxY: Double = -Double.MAX_VALUE
        var minZ: Double = Double.MAX_VALUE
        var maxZ: Double = -Double.MAX_VALUE

        organData.points.forEach { point: List<Double> ->
            if (point.size >= 3) {
                val xValue: Double = point[0]
                val yValue: Double = point[1]
                val zValue: Double = point[2]
                
                minX = minOf(minX, xValue)
                maxX = maxOf(maxX, xValue)
                minY = minOf(minY, yValue)
                maxY = maxOf(maxY, yValue)
                minZ = minOf(minZ, zValue)
                maxZ = maxOf(maxZ, zValue)
            }
        }

        val centerX: Double = (minX + maxX) / 2.0
        val centerY: Double = (minY + maxY) / 2.0
        val centerZ: Double = (minZ + maxZ) / 2.0
        
        val sizeX: Double = maxX - minX
        val sizeY: Double = maxY - minY
        val sizeZ: Double = maxZ - minZ
        val maxDim: Double = maxOf(sizeX, maxOf(sizeY, sizeZ))
        val scaleFactor: Double = if (maxDim > 0) 2.0 / maxDim else 1.0

        // 限制最大显示数量，防止 OOM
        val displayCount: Int = minOf(organData.points.size, 2000)
        Log.d("Model3DScreen", "Creating $displayCount SphereNodes with radius 0.03f (ORANGE color)")
        
        val orangeColor = Color(0xFFFF9800)
        
        for (i in 0 until displayCount) {
            val point: List<Double> = organData.points[i]
            if (point.size >= 3) {
                val scaledX: Float = ((point[0] - centerX) * scaleFactor).toFloat()
                val scaledY: Float = ((point[1] - centerY) * scaleFactor).toFloat()
                val scaledZ: Float = ((point[2] - centerZ) * scaleFactor).toFloat()
                
                pointColors[i] = orangeColor
                originalPointColors[i] = orangeColor
                
                val sphereNode = createColoredSphereNode(
                    engine = engine,
                    materialLoader = materialLoader,
                    position = Position(scaledX, scaledY, scaledZ),
                    color = orangeColor
                )
                sphereNodes.add(sphereNode)
                // 将点云节点也添加到 nodes 列表中，确保 Scene 能渲染
                nodes.add(sphereNode)
            }
        }
        
        Log.d("Model3DScreen", "Created ${sphereNodes.size} SphereNodes for organ: $organ, total nodes: ${nodes.size}")
    }

    // 加载所有 GLB 模型
    LaunchedEffect(glbUrls) {
        // 只清空 GLB 节点，不清空点云节点
        nodes.removeAll { it is ModelNode }
        sphereNodes.clear()
        selectedModelType = null
        
        Log.d("Model3DScreen", "Starting to load ${glbUrls.size} GLB models")
        
        glbUrls.forEachIndexed { index, url ->
            launch {
                try {
                    Log.d("Model3DScreen", "Downloading GLB #$index: $url")
                    val modelBytes = RetrofitClient.downloadFile(url)
                    if (modelBytes != null) {
                        Log.d("Model3DScreen", "Downloaded ${modelBytes.size} bytes for GLB #$index")
                        
                        val buffer = ByteBuffer.allocateDirect(modelBytes.size)
                        buffer.order(ByteOrder.nativeOrder())
                        buffer.put(modelBytes)
                        buffer.flip()
                        
                        Log.d("Model3DScreen", "Creating model instance for GLB #$index, buffer size: ${buffer.remaining()}")
                        val modelInstance = modelLoader.createModelInstance(buffer)
                        if (modelInstance != null) {
                            Log.d("Model3DScreen", "Model instance created for GLB #$index, creating ModelNode")
                            val modelNode = ModelNode(
                                modelInstance = modelInstance,
                                scaleToUnits = 2.0f,
                                centerOrigin = Position(0f, 0f, 0f)
                            )
                            Log.d("Model3DScreen", "ModelNode created for GLB #$index, adding to nodes")
                            nodes.add(modelNode)
                            Log.d("Model3DScreen", "Successfully loaded GLB #$index, total nodes: ${nodes.size}")
                            
                            // 加载完所有 GLB 后，调整相机位置到一个默认位置
                            if (index == glbUrls.size - 1 && nodes.isNotEmpty()) {
                                // 简单设置相机位置，让用户可以手动调整
                                cameraNode.position = Position(0f, 0f, 50f)
                                Log.d("Model3DScreen", "Camera positioned at default location")
                            }
                        } else {
                            Log.e("Model3DScreen", "Failed to create model instance for GLB #$index")
                        }
                    } else {
                        Log.e("Model3DScreen", "Failed to download GLB #$index")
                    }
                } catch (e: Exception) {
                    Log.e("Model3DScreen", "Error loading GLB #$index: ${e.message}", e)
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scene(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            modelLoader = modelLoader,
            materialLoader = materialLoader,
            cameraNode = cameraNode,
            mainLightNode = mainLightNode,
            environmentLoader = environmentLoader,
            childNodes = nodes + sphereNodes,
            onGestureListener = rememberOnGestureListener(
                onSingleTapConfirmed = { _, node ->
                    if (isSelectingPoint) {
                        val tappedSphere = node as? SphereNode
                        if (tappedSphere != null) {
                            val index = sphereNodes.indexOf(tappedSphere)
                            if (index != -1) {
                                pendingPointIndex = index
                                showConfirmDialog = true
                            }
                        }
                    }
                }
            )
        )

        // UI Controls
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.5f), shape = MaterialTheme.shapes.medium)
                .padding(8.dp)
        ) {
            IconButton(
                onClick = { isSelectingPoint = !isSelectingPoint },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (isSelectingPoint) MaterialTheme.colorScheme.primary else Color.Transparent
                )
            ) {
                Icon(
                    imageVector = Icons.Default.TouchApp,
                    contentDescription = "Select Point",
                    tint = Color.White
                )
            }
            
            if (selectedPointIndices.isNotEmpty()) {
                IconButton(
                    onClick = {
                        selectedPointIndices.forEach { index ->
                            val originalColor = originalPointColors[index] ?: Color(0xFFFF9800)
                            pointColors[index] = originalColor
                            updateSphereNodeColor(index, originalColor)
                        }
                        selectedPointIndices = emptySet()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Cancel,
                        contentDescription = "Clear Selection",
                        tint = Color.White
                    )
                }
            }
        }
        
        if (showConfirmDialog && pendingPointIndex != null) {
            AlertDialog(
                onDismissRequest = { 
                    showConfirmDialog = false
                    pendingPointIndex = null
                },
                title = { Text("确认选择") },
                text = { Text("您确定要选择这个点吗？") },
                confirmButton = {
                    TextButton(onClick = {
                        pendingPointIndex?.let { index ->
                            val newSelectedIndices = selectedPointIndices.toMutableSet()
                            if (newSelectedIndices.contains(index)) {
                                newSelectedIndices.remove(index)
                                val originalColor = originalPointColors[index] ?: Color(0xFFFF9800)
                                pointColors[index] = originalColor
                                updateSphereNodeColor(index, originalColor)
                            } else {
                                newSelectedIndices.add(index)
                                val highlightColor = Color.Red
                                pointColors[index] = highlightColor
                                updateSphereNodeColor(index, highlightColor)
                            }
                            selectedPointIndices = newSelectedIndices
                        }
                        showConfirmDialog = false
                        pendingPointIndex = null
                    }) {
                        Text("确定")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { 
                        showConfirmDialog = false
                        pendingPointIndex = null
                    }) {
                        Text("取消")
                    }
                }
            )
        }
        
        // 模型类型选择栏（底部，可折叠）
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            onClick = {
                isModelListExpanded = !isModelListExpanded
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "已加载模型 (${modelTypes.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A)
                    )
                    Icon(
                        imageVector = if (isModelListExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                        contentDescription = if (isModelListExpanded) "收起" else "展开",
                        tint = Color.Gray
                    )
                }
                
                if (isModelListExpanded) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (modelTypes.isEmpty()) {
                        Text(
                            text = "暂无模型",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    } else {
                        modelTypes.forEach { modelType ->
                            val isSelected = selectedModelType == modelType
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isSelected) {
                                            // 点击已选中的，清除选择
                                            selectedModelType = null
                                            clearPointCloud()
                                            Log.d("Model3DScreen", "Cleared point cloud for: $modelType")
                                        } else {
                                            // 点击其他，加载对应点云
                                            selectedModelType = modelType
                                            loadPointCloud(modelType)
                                            Log.d("Model3DScreen", "Loaded point cloud for: $modelType")
                                        }
                                    }
                                    .padding(vertical = 8.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = modelType,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF1A1A1A),
                                    fontSize = 18.sp
                                )
                            }
                            HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.3f))
                        }
                    }
                }
            }
        }
        
        // 调试信息叠加
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.5f), shape = MaterialTheme.shapes.medium)
                .padding(8.dp)
        ) {
            Text("Total Nodes: ${nodes.size}", color = Color.White)
            Text("GLBs: ${glbUrls.size}", color = Color.White)
            Text("Points: ${sphereNodes.size}", color = Color(0xFF00FF00))
            Text("Selected: ${selectedPointIndices.size}", color = Color.Yellow)
            if (selectedModelType != null) {
                Text("Active: $selectedModelType", color = Color(0xFF00BFFF))
            }
        }
    }
}
