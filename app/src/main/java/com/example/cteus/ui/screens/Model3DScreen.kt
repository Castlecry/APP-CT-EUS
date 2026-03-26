package com.example.cteus.ui.screens

import android.util.Log
import android.widget.Toast
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
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
import io.github.sceneview.node.CubeNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberOnGestureListener
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberMainLightNode
import com.google.android.filament.MaterialInstance
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
import kotlin.math.sqrt

// 创面功能状态机
enum class PlaneCreateState {
    IDLE,               // 初始状态
    ROTATING_PLANE,     // 正在手势旋转面
    PLANE_ROTATED,      // 面已固定，等待点击选择方向
    SELECTING_DIRECTION,// 开启方向选择，等待点击边缘点
    DIRECTION_SELECTED, // 边缘点已吸附，等待保存数据
    COMPLETED           // 流程完成，面持久化展示，可隐藏/展示
}

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
                title = { Text(text = if (selectedCaseId == null) "病例列表" else (caseDetail?.caseName ?: "3D 模型预览"), style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    if (selectedCaseId != null) {
                        IconButton(onClick = { selectedCaseId = null; viewModel.fetchCaseList() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer, titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (selectedCaseId == null) {
                CaseListContent(cases = caseList, onCaseClick = { selectedCaseId = it.caseId; viewModel.fetchCaseDetail(it.caseId) })
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
            if (isLoading) Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            errorMessage?.let { Snackbar(modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp), action = { TextButton(onClick = { viewModel.clearError() }) { Text("确定") } }) { Text(it) } }
        }
    }
}

@Composable
fun CaseListContent(cases: List<CaseItem>, onCaseClick: (CaseItem) -> Unit) {
    if (cases.isEmpty()) Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("暂无病例数据", color = Color.Gray) }
    else LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(cases) { case ->
            Card(modifier = Modifier.fillMaxWidth().clickable { onCaseClick(case) }, elevation = CardDefaults.cardElevation(2.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(text = case.caseName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(text = "ID: ${case.caseId}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = case.description, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                }
            }
        }
    }
}

@Composable
fun StatusMessage(message: String) {
    Column(modifier = Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        CircularProgressIndicator(modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = message, color = MaterialTheme.colorScheme.secondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
fun ThreeDViewerContent(
    glbUrls: List<String>,
    organPointsList: List<OrganPoints>,
    modelTypes: List<String>
) {
    val context = LocalContext.current
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)

    val pivotNode = remember { Node(engine) }
    val cameraNode = rememberCameraNode(engine) { position = Position(0f, 0f, 0.3f); lookAt(Position(0f, 0f, 0f)) }
    val cameraManipulator = remember { Manipulator.Builder().orbitHomePosition(0.0f, 0.0f, 0.3f).targetPosition(0.0f, 0.0f, 0.0f).build(Manipulator.Mode.ORBIT) }
    val mainLightNode = rememberMainLightNode(engine) { intensity = 20_000f }

    val glbNodes = remember { mutableStateListOf<ModelNode>() }
    val sphereNodes = remember { mutableStateListOf<SphereNode>() }
    val organNodesMap = remember { mutableStateMapOf<String, MutableList<SphereNode>>() }

    // 材质池化
    var sharedOrangeMaterial: MaterialInstance? by remember { mutableStateOf(null) }
    var sharedRedMaterial: MaterialInstance? by remember { mutableStateOf(null) }
    var sharedBlueMaterial: MaterialInstance? by remember { mutableStateOf(null) }
    var sharedGreenMaterial: MaterialInstance? by remember { mutableStateOf(null) }

    LaunchedEffect(materialLoader) {
        try {
            sharedOrangeMaterial = materialLoader.createColorInstance(Color(0xFFFF9800))
            sharedRedMaterial = materialLoader.createColorInstance(Color.Red)
            sharedBlueMaterial = materialLoader.createColorInstance(Color(0x8800BFFF)) // 半透明蓝
            sharedGreenMaterial = materialLoader.createColorInstance(Color.Green) // 绿色箭头
        } catch (e: Exception) { Log.e("Model3DScreen", "材质加载失败") }
    }

    // 基础交互状态
    var isSelectingPoint by remember { mutableStateOf(false) }
    var selectedPointIndices by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var pendingPointIndex by remember { mutableStateOf<Int?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    // 面交互状态机与节点
    var planeState by remember { mutableStateOf(PlaneCreateState.IDLE) }
    var isPlaneVisible by remember { mutableStateOf(true) } // 控制面是否可见
    var planeContainerNode by remember { mutableStateOf<Node?>(null) }
    var planeCenterNode by remember { mutableStateOf<SphereNode?>(null) }
    var normalRefNode by remember { mutableStateOf<Node?>(null) } 
    var planeDirectionArrowNode by remember { mutableStateOf<Node?>(null) } // 实际可见的绿线
    
    // 专门用于边缘吸附的 36 个隐形节点
    val edgeDummyNodes = remember { mutableStateListOf<Node>() }
    var selectedEdgeNode by remember { mutableStateOf<Node?>(null) } // 选中的边界节点

    // 最终保存的数据显示
    var showSaveResultDialog by remember { mutableStateOf(false) }
    var saveResultText by remember { mutableStateOf("") }

    var selectedModelTypes by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isModelListExpanded by remember { mutableStateOf(true) }
    var viewWidth by remember { mutableStateOf(1f) }
    var viewHeight by remember { mutableStateOf(1f) }

    // 摄像机冻结：只有在 ROTATING_PLANE 状态，彻底禁用滑动视角
    val activeCameraManipulator = if (planeState == PlaneCreateState.ROTATING_PLANE) null else cameraManipulator

    fun updatePivotCenter() {
        var minX = Float.MAX_VALUE; var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE; var maxY = -Float.MAX_VALUE
        var minZ = Float.MAX_VALUE; var maxZ = -Float.MAX_VALUE
        var hasData = false

        glbNodes.forEach { node ->
            val c = node.center; val e = node.extents
            val hX = e.x / 2f; val hY = e.y / 2f; val hZ = e.z / 2f
            hasData = true
            minX = minOf(minX, c.x - hX); maxX = maxOf(maxX, c.x + hX)
            minY = minOf(minY, c.y - hY); maxY = maxOf(maxY, c.y + hY)
            minZ = minOf(minZ, c.z - hZ); maxZ = maxOf(maxZ, c.z + hZ)
        }
        if (!hasData && organPointsList.isNotEmpty()) {
            organPointsList.forEach { organ -> organ.points.forEach { p ->
                if (p.size >= 3) {
                    hasData = true
                    minX = minOf(minX, p[0].toFloat()); maxX = maxOf(maxX, p[0].toFloat())
                    minY = minOf(minY, p[1].toFloat()); maxY = maxOf(maxY, p[1].toFloat())
                    minZ = minOf(minZ, p[2].toFloat()); maxZ = maxOf(maxZ, p[2].toFloat())
                }
            }}
        }
        if (hasData) {
            pivotNode.position = Position(-((minX+maxX)/2f) * 0.0001f, -((minY+maxY)/2f) * 0.0001f, -((minZ+maxZ)/2f) * 0.0001f)
        }
    }

    fun updateSphereNodeColor(index: Int, isSelected: Boolean, isDirection: Boolean = false) {
        if (index in sphereNodes.indices) {
            try {
                // 1. 先判断应该使用哪个颜色的共享材质
                val targetMaterial = when {
                    isDirection -> sharedGreenMaterial
                    isSelected -> sharedRedMaterial
                    else -> sharedOrangeMaterial
                }

                // 2. 使用 ?.let 安全解包：只有当材质加载成功不为 null 时，才赋值给 Node
                targetMaterial?.let {
                    sphereNodes[index].materialInstance = it
                }
            } catch (e: Exception) {
                Log.e("Model3DScreen", "Error updating sphere color")
            }
        }
    }

    fun loadPointCloud(organ: String) {
        val organData = organPointsList.find { it.organ == organ } ?: return
        val organNodes = mutableListOf<SphereNode>()
        for (i in 0 until minOf(organData.points.size, 2000)) {
            val point = organData.points[i]
            if (point.size >= 3) {
                val sphereNode = SphereNode(engine = engine, radius = 0.000166f, center = Position(0f, 0f, 0f)).apply {
                    position = Position(point[0].toFloat() * 0.0001f, point[1].toFloat() * 0.0001f, point[2].toFloat() * 0.0001f)
                }
                try { sharedOrangeMaterial?.let { sphereNode.materialInstance = it } } catch (e: Exception) {}
                sphereNodes.add(sphereNode)
                organNodes.add(sphereNode)
                pivotNode.addChildNode(sphereNode)
            }
        }
        organNodesMap[organ] = organNodes
    }

    fun removePointCloud(organ: String) {
        organNodesMap.remove(organ)?.forEach { node ->
            pivotNode.removeChildNode(node); sphereNodes.remove(node)
        }
    }

    // 清理所有的面节点，回归最初始状态
    fun clearPlaneState() {
        planeState = PlaneCreateState.IDLE
        planeContainerNode?.let { pivotNode.removeChildNode(it) }
        planeContainerNode = null
        planeCenterNode = null
        normalRefNode = null
        planeDirectionArrowNode = null
        selectedEdgeNode = null
        edgeDummyNodes.clear()
        isPlaneVisible = true
    }

    LaunchedEffect(glbUrls) {
        glbNodes.forEach { pivotNode.removeChildNode(it) }
        glbNodes.clear()
        sphereNodes.forEach { pivotNode.removeChildNode(it) }
        sphereNodes.clear()
        selectedModelTypes = emptySet()
        clearPlaneState()

        val totalModels = glbUrls.size
        if (totalModels == 0) { updatePivotCenter(); return@LaunchedEffect }
        var loadedCount = 0

        glbUrls.forEach { url ->
            launch(Dispatchers.IO) {
                try {
                    val bytes = RetrofitClient.downloadFile(url)
                    if (bytes != null) {
                        val buffer = ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder()).put(bytes).apply { flip() }
                        withContext(Dispatchers.Main) {
                            modelLoader.createModelInstance(buffer)?.let {
                                val modelNode = ModelNode(modelInstance = it).apply { scale = Scale(0.0001f, 0.0001f, 0.0001f) }
                                glbNodes.add(modelNode)
                                pivotNode.addChildNode(modelNode)
                            }
                        }
                    }
                } catch (e: Exception) {} finally {
                    withContext(Dispatchers.Main) { loadedCount++; if (loadedCount == totalModels) updatePivotCenter() }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().onSizeChanged { viewWidth = it.width.toFloat(); viewHeight = it.height.toFloat() }) {
        Scene(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            modelLoader = modelLoader,
            materialLoader = materialLoader,
            cameraNode = cameraNode,
            cameraManipulator = activeCameraManipulator, // 面旋转时被冻结
            mainLightNode = mainLightNode,
            environmentLoader = environmentLoader,
            childNodes = listOf(pivotNode),
            onGestureListener = rememberOnGestureListener(
                // 滑动事件拦截：当平面旋转时处理
                onScroll = { _, _, _, distance ->
                    if (planeState == PlaneCreateState.ROTATING_PLANE && planeContainerNode != null) {
                        val rot = planeContainerNode!!.rotation
                        planeContainerNode!!.rotation = Position(rot.x - distance.y * 0.2f, rot.y - distance.x * 0.2f, rot.z)
                    }
                },
                onSingleTapConfirmed = { motionEvent, _ ->
                    val isSelectingDirection = planeState == PlaneCreateState.SELECTING_DIRECTION || planeState == PlaneCreateState.DIRECTION_SELECTED
                    val isSelectingCenter = isSelectingPoint && (planeState == PlaneCreateState.IDLE || planeState == PlaneCreateState.COMPLETED)
                    
                    if ((isSelectingCenter || isSelectingDirection) && viewWidth > 0 && viewHeight > 0) {
                        var closestIndex = -1
                        var closestEdgeNode: Node? = null
                        var minDistance = Float.MAX_VALUE

                        try {
                            val camera = cameraNode.camera ?: return@rememberOnGestureListener
                            val viewMatrixDouble = DoubleArray(16); val projMatrixDouble = DoubleArray(16)
                            camera.getViewMatrix(viewMatrixDouble); camera.getProjectionMatrix(projMatrixDouble)
                            val viewProjMatrix = FloatArray(16)
                            val viewMatrix = FloatArray(16) { viewMatrixDouble[it].toFloat() }
                            val projMatrix = FloatArray(16) { projMatrixDouble[it].toFloat() }
                            android.opengl.Matrix.multiplyMM(viewProjMatrix, 0, projMatrix, 0, viewMatrix, 0)

                            // 1. 如果正在选面方向 -> 遍历 36 个隐形边界点
                            if (isSelectingDirection && edgeDummyNodes.isNotEmpty()) {
                                edgeDummyNodes.forEach { edgeNode ->
                                    val wp = edgeNode.worldPosition
                                    val clipVec = FloatArray(4)
                                    android.opengl.Matrix.multiplyMV(clipVec, 0, viewProjMatrix, 0, floatArrayOf(wp.x, wp.y, wp.z, 1f), 0)
                                    if (clipVec[3] > 0.0001f) {
                                        val screenX = (clipVec[0]/clipVec[3] + 1f)/2f * viewWidth
                                        val screenY = (1f - clipVec[1]/clipVec[3])/2f * viewHeight
                                        val dist = kotlin.math.sqrt(Math.pow((screenX - motionEvent.x).toDouble(), 2.0) + Math.pow((screenY - motionEvent.y).toDouble(), 2.0)).toFloat()
                                        if (dist < minDistance) { minDistance = dist; closestEdgeNode = edgeNode }
                                    }
                                }
                                
                                // 吸附距离 100px
                                if (closestEdgeNode != null && minDistance < 100f) {
                                    selectedEdgeNode = closestEdgeNode
                                    planeState = PlaneCreateState.DIRECTION_SELECTED
                                    
                                    // 移除旧箭头
                                    planeDirectionArrowNode?.let { planeContainerNode?.removeChildNode(it) }
                                    
                                    // 生成连接中心点和该边缘点的新绿线
                                    val localX = closestEdgeNode!!.position.x
                                    val localY = closestEdgeNode!!.position.y
                                    val len = sqrt(localX*localX + localY*localY)
                                    
                                    // 利用 atan2 计算绕 Z 轴的平面旋转角
                                    val angleDeg = Math.toDegrees(kotlin.math.atan2(localY.toDouble(), localX.toDouble())).toFloat()
                                    
                                    val dirPivot = Node(engine)
                                    dirPivot.rotation = Position(0f, 0f, angleDeg)
                                    
                                    // 几何体：长度 len，从中心出发指向边缘
                                    val dirMesh = CubeNode(engine, size = Position(len, 0.0004f, 0.0004f), center = Position(len/2f, 0f, 0f))
                                    sharedGreenMaterial?.let { dirMesh.materialInstance = it }
                                    dirPivot.addChildNode(dirMesh)
                                    
                                    planeContainerNode?.addChildNode(dirPivot)
                                    planeDirectionArrowNode = dirPivot
                                }
                            } 
                            // 2. 如果正在选初始中心点 -> 遍历海量点云
                            else if (isSelectingCenter && sphereNodes.isNotEmpty()) {
                                sphereNodes.forEachIndexed { index, sphereNode ->
                                    val wp = sphereNode.worldPosition
                                    val clipVec = FloatArray(4)
                                    android.opengl.Matrix.multiplyMV(clipVec, 0, viewProjMatrix, 0, floatArrayOf(wp.x, wp.y, wp.z, 1f), 0)
                                    if (clipVec[3] > 0.0001f) {
                                        val screenX = (clipVec[0]/clipVec[3] + 1f)/2f * viewWidth
                                        val screenY = (1f - clipVec[1]/clipVec[3])/2f * viewHeight
                                        val dist = kotlin.math.sqrt(Math.pow((screenX - motionEvent.x).toDouble(), 2.0) + Math.pow((screenY - motionEvent.y).toDouble(), 2.0)).toFloat()
                                        if (dist < minDistance) { minDistance = dist; closestIndex = index }
                                    }
                                }
                                
                                if (closestIndex != -1 && minDistance < 100f) {
                                    // 当点击了新的有效点时，清空之前创建的所有面
                                    if (planeState == PlaneCreateState.COMPLETED) clearPlaneState()
                                    pendingPointIndex = closestIndex
                                    showConfirmDialog = true
                                }
                            }
                        } catch (e: Exception) { Log.e("Model3DScreen", "投影计算失败") }
                    }
                }
            )
        )

        // 核心 UI 控制区域
        Column(
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp).background(Color.Black.copy(alpha = 0.5f), shape = MaterialTheme.shapes.medium).padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 开关选点模式
            Row {
                IconButton(onClick = { isSelectingPoint = !isSelectingPoint }, colors = IconButtonDefaults.iconButtonColors(containerColor = if (isSelectingPoint) MaterialTheme.colorScheme.primary else Color.Transparent)) {
                    Icon(imageVector = Icons.Default.TouchApp, contentDescription = "Select Point", tint = Color.White)
                }
                if (selectedPointIndices.isNotEmpty() && planeState == PlaneCreateState.IDLE) {
                    IconButton(onClick = { selectedPointIndices.forEach { updateSphereNodeColor(it, false) }; selectedPointIndices = emptySet() }) {
                        Icon(imageVector = Icons.Default.Cancel, contentDescription = "Clear", tint = Color.White)
                    }
                }
            }

            // 状态机面板
            when (planeState) {
                PlaneCreateState.IDLE -> {
                    Button(onClick = {
                        if (selectedPointIndices.size == 1) {
                            planeState = PlaneCreateState.ROTATING_PLANE
                            planeCenterNode = sphereNodes[selectedPointIndices.first()]

                            val container = Node(engine)
                            container.position = planeCenterNode!!.position

                            // 生成蓝色底面
                            val halfSize = 0.004f
                            val planeMesh = CubeNode(engine, size = Position(halfSize*2, halfSize*2, 0.00001f), center = Position(0f, 0f, 0f))
                            sharedBlueMaterial?.let { planeMesh.materialInstance = it }
                            container.addChildNode(planeMesh)

                            // 生成粗壮红色法向量
                            val arrowMesh = CubeNode(engine, size = Position(0.0004f, 0.0004f, 0.006f), center = Position(0f, 0f, 0.003f))
                            sharedRedMaterial?.let { arrowMesh.materialInstance = it }
                            container.addChildNode(arrowMesh)

                            // 放置获取法相量的隐形节点
                            val refNode = Node(engine).apply { position = Position(0f, 0f, 1f) }
                            container.addChildNode(refNode)
                            normalRefNode = refNode

                            // 生成36个隐形的边界点，每边分布10个点（含顶点）
                            edgeDummyNodes.clear()
                            val segments = 9
                            for (i in 0..segments) {
                                val t = -halfSize + (2 * halfSize * i) / segments
                                edgeDummyNodes.add(Node(engine).apply { position = Position(t, halfSize, 0f) }) // 上
                                edgeDummyNodes.add(Node(engine).apply { position = Position(t, -halfSize, 0f) })// 下
                                if (i in 1 until segments) {
                                    edgeDummyNodes.add(Node(engine).apply { position = Position(-halfSize, t, 0f) }) // 左
                                    edgeDummyNodes.add(Node(engine).apply { position = Position(halfSize, t, 0f) })  // 右
                                }
                            }
                            edgeDummyNodes.forEach { container.addChildNode(it) }

                            pivotNode.addChildNode(container)
                            planeContainerNode = container
                            isPlaneVisible = true

                        } else Toast.makeText(context, "该功能仅支持选中一个点时使用，请重新选择单个点", Toast.LENGTH_SHORT).show()
                    }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) { Text("创建并选择面") }
                }

                PlaneCreateState.ROTATING_PLANE -> {
                    Text("提示：视角已冻结\n单指滑动屏幕旋转平面", color = Color.Yellow, fontSize = 12.sp)
                    Button(onClick = { planeState = PlaneCreateState.PLANE_ROTATED }) { Text("完成创建并选择面") }
                }

                PlaneCreateState.PLANE_ROTATED -> {
                    Button(onClick = { planeState = PlaneCreateState.SELECTING_DIRECTION }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) { Text("选择面方向") }
                    Button(onClick = { clearPlaneState() }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("重置放弃") }
                }

                PlaneCreateState.SELECTING_DIRECTION -> {
                    Button(onClick = { }, enabled = false, colors = ButtonDefaults.buttonColors(disabledContainerColor = Color.Gray)) { Text("请点击面的边缘处吸附", color = Color.White) }
                    Button(onClick = { clearPlaneState() }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("重置放弃") }
                }

                PlaneCreateState.DIRECTION_SELECTED -> {
                    Button(onClick = {
                        val cNode = planeCenterNode!!
                        val eNode = selectedEdgeNode!!

                        // 绝对坐标与绝对向量计算
                        val cx = cNode.worldPosition.x * 10000f; val cy = cNode.worldPosition.y * 10000f; val cz = cNode.worldPosition.z * 10000f
                        
                        val wNorm = normalRefNode!!.worldPosition
                        val wCent = planeContainerNode!!.worldPosition
                        var nx = wNorm.x - wCent.x; var ny = wNorm.y - wCent.y; var nz = wNorm.z - wCent.z
                        val nLen = sqrt(nx*nx + ny*ny + nz*nz); nx /= nLen; ny /= nLen; nz /= nLen

                        // 因为点在面上，方向直接是边缘点减中心点，无需再次正交投影
                        var px = (eNode.worldPosition.x - wCent.x) * 10000f
                        var py = (eNode.worldPosition.y - wCent.y) * 10000f
                        var pz = (eNode.worldPosition.z - wCent.z) * 10000f
                        val pLen = sqrt(px*px + py*py + pz*pz); if (pLen > 0) { px /= pLen; py /= pLen; pz /= pLen }

                        saveResultText = "已保存数据：\n" +
                                "点坐标：(${String.format("%.2f", cx)}, ${String.format("%.2f", cy)}, ${String.format("%.2f", cz)})\n" +
                                "法向量：(${String.format("%.4f", nx)}, ${String.format("%.4f", ny)}, ${String.format("%.4f", nz)})\n" +
                                "面方向：(${String.format("%.4f", px)}, ${String.format("%.4f", py)}, ${String.format("%.4f", pz)})"

                        showSaveResultDialog = true
                    }) { Text("结束并保存") }
                    Button(onClick = { clearPlaneState() }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("重置放弃") }
                }

                PlaneCreateState.COMPLETED -> {
                    // 面已完成状态，只允许删除
                    Button(onClick = { clearPlaneState() }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                        Text("删除面", color = Color.White)
                    }
                }
            }
        }

        // 新选点弹窗
        if (showConfirmDialog && pendingPointIndex != null) {
            val pointLocal = sphereNodes.getOrNull(pendingPointIndex!!)?.position
            val origX = (pointLocal?.x ?: 0f) * 10000f; val origY = (pointLocal?.y ?: 0f) * 10000f; val origZ = (pointLocal?.z ?: 0f) * 10000f
            val coordText = "真实坐标: (${String.format("%.2f", origX)}, ${String.format("%.2f", origY)}, ${String.format("%.2f", origZ)})"

            AlertDialog(
                onDismissRequest = { showConfirmDialog = false; pendingPointIndex = null },
                title = { Text("确认选择") },
                text = { Column { Text("您确定要选择这个点吗？"); Spacer(modifier = Modifier.height(8.dp)); Text(text = coordText, color = MaterialTheme.colorScheme.primary) } },
                confirmButton = {
                    TextButton(onClick = {
                        pendingPointIndex?.let { index ->
                            val newSelectedIndices = selectedPointIndices.toMutableSet()
                            if (newSelectedIndices.contains(index)) {
                                newSelectedIndices.remove(index); updateSphereNodeColor(index, false)
                            } else {
                                newSelectedIndices.add(index); updateSphereNodeColor(index, true)
                            }
                            selectedPointIndices = newSelectedIndices
                        }
                        showConfirmDialog = false; pendingPointIndex = null
                    }) { Text("确定") }
                },
                dismissButton = { TextButton(onClick = { showConfirmDialog = false; pendingPointIndex = null }) { Text("取消") } }
            )
        }

        // 数据保存成功弹窗，弹窗结束后进入持久化 COMPLETED 状态
        if (showSaveResultDialog) {
            AlertDialog(
                onDismissRequest = { showSaveResultDialog = false; planeState = PlaneCreateState.COMPLETED },
                title = { Text("保存成功") },
                text = { Text(saveResultText) },
                confirmButton = { TextButton(onClick = { showSaveResultDialog = false; planeState = PlaneCreateState.COMPLETED }) { Text("确定") } }
            )
        }

        // 底部已加载模型 (保持不变)
        Card(modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(8.dp), onClick = { isModelListExpanded = !isModelListExpanded }) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "已加载模型 (${modelTypes.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A))
                    Icon(imageVector = if (isModelListExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp, contentDescription = null, tint = Color.Gray)
                }
                if (isModelListExpanded) {
                    Spacer(modifier = Modifier.height(8.dp))
                    if (modelTypes.isEmpty()) Text(text = "暂无模型", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    else {
                        modelTypes.forEach { modelType ->
                            val isSelected = selectedModelTypes.contains(modelType)
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    if (isSelected) { selectedModelTypes = selectedModelTypes - modelType; removePointCloud(modelType) } 
                                    else { selectedModelTypes = selectedModelTypes + modelType; loadPointCloud(modelType) }
                                }.padding(vertical = 8.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, contentDescription = null, tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(text = modelType, style = MaterialTheme.typography.bodyLarge, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF1A1A1A), fontSize = 18.sp)
                            }
                            HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.3f))
                        }
                    }
                }
            }
        }

        // 右上角调试框
        Column(modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).background(Color.Black.copy(alpha = 0.5f), shape = MaterialTheme.shapes.medium).padding(8.dp)) {
            Text("Total Nodes: ${glbNodes.size + sphereNodes.size}", color = Color.White)
            Text("GLBs: ${glbNodes.size}", color = Color.White)
            Text("Points: ${sphereNodes.size}", color = Color(0xFF00FF00))
            Text("Selected: ${selectedPointIndices.size}", color = Color.Yellow)
            if (planeState != PlaneCreateState.IDLE) Text("Mode: ${planeState.name}", color = Color.Cyan, fontSize = 10.sp)
        }
    }
}