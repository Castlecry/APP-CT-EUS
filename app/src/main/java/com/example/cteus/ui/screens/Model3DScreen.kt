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
import io.github.sceneview.math.Scale
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import io.github.sceneview.node.SphereNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberOnGestureListener
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberMainLightNode
import com.google.android.filament.utils.Manipulator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import com.google.android.filament.Engine
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

    // 建立唯一的容器节点，它的任务就是负责整体移动以对齐中心
    val pivotNode = remember { Node(engine) }

    val cameraNode = rememberCameraNode(engine) {
        position = Position(0f, 0f, 0.3f)
        lookAt(Position(0f, 0f, 0f))
    }

    val cameraManipulator = remember {
        Manipulator.Builder()
            .orbitHomePosition(0.0f, 0.0f, 0.3f)
            .targetPosition(0.0f, 0.0f, 0.0f) // 只要模型偏移了，镜头看原点就等于看模型中心
            .build(Manipulator.Mode.ORBIT)
    }

    val mainLightNode = rememberMainLightNode(engine) {
        intensity = 500_000f
    }

    val glbNodes = remember { mutableStateListOf<ModelNode>() }
    val sphereNodes = remember { mutableStateListOf<SphereNode>() }

    var isSelectingPoint by remember { mutableStateOf(false) }
    var selectedPointIndices by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var pendingPointIndex by remember { mutableStateOf<Int?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    val pointColors = remember { mutableStateMapOf<Int, Color>() }
    val originalPointColors = remember { mutableStateMapOf<Int, Color>() }
    var selectedModelTypes by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isModelListExpanded by remember { mutableStateOf(true) }

    // 【新增核心功能】：自动计算所有子模型的综合中心，并调整 pivotNode 的位置
    fun updatePivotCenter() {
        var minX = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        var minZ = Float.MAX_VALUE
        var maxZ = -Float.MAX_VALUE
        var hasData = false

        // 1. 优先读取已加载真实 3D 模型的边界框（Bounding Box）
        glbNodes.forEach { node ->
            // 在 SceneView 中，ModelNode 直接提供了 center(中心) 和 extents(包围盒总尺寸)
            val c = node.center
            val e = node.extents

            // extents 是完整长宽高，我们除以 2 算出半尺寸 (halfExtent)
            val hX = e.x / 2f
            val hY = e.y / 2f
            val hZ = e.z / 2f

            hasData = true
            minX = minOf(minX, c.x - hX)
            maxX = maxOf(maxX, c.x + hX)
            minY = minOf(minY, c.y - hY)
            maxY = maxOf(maxY, c.y + hY)
            minZ = minOf(minZ, c.z - hZ)
            maxZ = maxOf(maxZ, c.z + hZ)
        }

        // 2. 如果没有任何 GLB，才退而求其次用点云来算
        if (!hasData && organPointsList.isNotEmpty()) {
            organPointsList.forEach { organ ->
                organ.points.forEach { p ->
                    if (p.size >= 3) {
                        hasData = true
                        minX = minOf(minX, p[0].toFloat())
                        maxX = maxOf(maxX, p[0].toFloat())
                        minY = minOf(minY, p[1].toFloat())
                        maxY = maxOf(maxY, p[1].toFloat())
                        minZ = minOf(minZ, p[2].toFloat())
                        maxZ = maxOf(maxZ, p[2].toFloat())
                    }
                }
            }
        }

        if (hasData) {
            val cx = (minX + maxX) / 2f
            val cy = (minY + maxY) / 2f
            val cz = (minZ + maxZ) / 2f

            // 因为模型缩小了 0.0001，所以偏移距离也乘以 0.0001
            // 负数代表我们将容器往反方向拉，使得中心正好落入 (0,0,0)
            pivotNode.position = Position(
                -cx * 0.0001f,
                -cy * 0.0001f,
                -cz * 0.0001f
            )
            Log.d("Model3DScreen", "中心重新对齐完成！Pivot坐标设为: ${pivotNode.position}")
        }
    }

    fun updateSphereNodeColor(index: Int, color: Color) {
        if (index >= 0 && index < sphereNodes.size) {
            val sphereNode = sphereNodes[index]
            try {
                val materialInstance = sphereNode.materialInstance
                materialInstance.setColor(colorOf(color))
            } catch (e: Exception) {
                Log.e("Model3DScreen", "Error updating sphere color: ${e.message}")
            }
        }
    }

    fun createColoredSphereNode(engine: Engine, materialLoader: MaterialLoader, position: Position, color: Color): SphereNode {
        val sphereNode = SphereNode(
            engine = engine,
            radius = 0.000166f,
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

    fun loadPointCloud(organ: String) {
        val organData = organPointsList.find { it.organ == organ }
        if (organData == null || organData.points.isEmpty()) return

        val scaleFactor = 0.0001f
        val displayCount = minOf(organData.points.size, 2000)
        val orangeColor = Color(0xFFFF9800)
        val startIndex = sphereNodes.size

        for (i in 0 until displayCount) {
            val point = organData.points[i]
            if (point.size >= 3) {
                // 点云使用绝对坐标缩放。它作为 pivotNode 的子节点，会自动跟着模型一起偏移！
                val scaledX = (point[0] * scaleFactor).toFloat()
                val scaledY = (point[1] * scaleFactor).toFloat()
                val scaledZ = (point[2] * scaleFactor).toFloat()

                val pointIndex = startIndex + i
                pointColors[pointIndex] = orangeColor
                originalPointColors[pointIndex] = orangeColor

                val sphereNode = createColoredSphereNode(
                    engine = engine,
                    materialLoader = materialLoader,
                    position = Position(scaledX, scaledY, scaledZ),
                    color = orangeColor
                )
                sphereNodes.add(sphereNode)
                pivotNode.addChildNode(sphereNode) // 添加到中心偏移容器
            }
        }
    }

    fun removePointCloud(organ: String) {
        sphereNodes.forEach { pivotNode.removeChildNode(it) }
        sphereNodes.clear()
        pointColors.clear()
        originalPointColors.clear()
        selectedPointIndices = emptySet()

        selectedModelTypes.forEach { selectedOrgan ->
            loadPointCloud(selectedOrgan)
        }
    }

    LaunchedEffect(glbUrls) {
        // 清理旧节点
        glbNodes.forEach { pivotNode.removeChildNode(it) }
        glbNodes.clear()
        sphereNodes.forEach { pivotNode.removeChildNode(it) }
        sphereNodes.clear()
        selectedModelTypes = emptySet()

        var loadedCount = 0
        val totalModels = glbUrls.size

        if (totalModels == 0) {
            updatePivotCenter()
            return@LaunchedEffect
        }

        glbUrls.forEachIndexed { index, url ->
            launch {
                try {
                    val modelBytes = RetrofitClient.downloadFile(url)
                    if (modelBytes != null) {
                        val buffer = ByteBuffer.allocateDirect(modelBytes.size)
                        buffer.order(ByteOrder.nativeOrder())
                        buffer.put(modelBytes)
                        buffer.flip()

                        val filamentAsset = modelLoader.createModelInstance(buffer)
                        if (filamentAsset != null) {
                            withContext(Dispatchers.Main) {
                                val modelNode = ModelNode(modelInstance = filamentAsset)
                                modelNode.scale = Scale(0.0001f, 0.0001f, 0.0001f)
                                // 模型依然放在它的绝对位置，不作任何调整
                                modelNode.position = Position(0f, 0f, 0f)

                                glbNodes.add(modelNode)
                                pivotNode.addChildNode(modelNode) // 添加到中心偏移容器
                                loadedCount++

                                // 【关键修改】：当最后1个模型下载并加载完成时，触发一次整体中心计算！
                                if (loadedCount == totalModels) {
                                    updatePivotCenter()
                                }
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                loadedCount++
                                if (loadedCount == totalModels) updatePivotCenter()
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            loadedCount++
                            if (loadedCount == totalModels) updatePivotCenter()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        loadedCount++
                        if (loadedCount == totalModels) updatePivotCenter()
                    }
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
            cameraManipulator = cameraManipulator,
            mainLightNode = mainLightNode,
            environmentLoader = environmentLoader,
            // 场景里现在只有这个容器节点
            childNodes = listOf(pivotNode),
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

        // 下面是所有 UI 控制代码，没有变化
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
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
                            val isSelected = selectedModelTypes.contains(modelType)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isSelected) {
                                            selectedModelTypes = selectedModelTypes - modelType
                                            removePointCloud(modelType)
                                        } else {
                                            selectedModelTypes = selectedModelTypes + modelType
                                            loadPointCloud(modelType)
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

        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.5f), shape = MaterialTheme.shapes.medium)
                .padding(8.dp)
        ) {
            Text("Total Nodes: ${glbNodes.size + sphereNodes.size}", color = Color.White)
            Text("GLBs: ${glbNodes.size}", color = Color.White)
            Text("Points: ${sphereNodes.size}", color = Color(0xFF00FF00))
            Text("Selected: ${selectedPointIndices.size}", color = Color.Yellow)
            if (selectedModelTypes.isNotEmpty()) {
                Text("Active: ${selectedModelTypes.size} organs", color = Color(0xFF00BFFF))
            }
        }
    }
}