package com.shengji.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shengji.app.net.*
import kotlinx.coroutines.delay

/**
 * 联机大厅页面（老人向设计）
 *
 * - 进入页面自动开始发现，无需手动扫描
 * - 大字体、高对比度
 * - 权限拒绝时大白话引导
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiplayerScreen(
    onBack: () -> Unit,
    onCreateRoom: () -> Unit,
    onJoinRoom: (String) -> Unit  // 传入房主 IP
) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(NetworkPermissionHelper.hasAllPermissions(context)) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var deniedPermission by remember { mutableStateOf("") }
    var discoveredRooms by remember { mutableStateOf(listOf<DiscoveredRoom>()) }
    var isDiscovering by remember { mutableStateOf(false) }

    // 权限请求
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasPermission = results.values.all { it }
        if (!hasPermission) {
            // 找到被拒绝的权限
            val denied = results.entries.firstOrNull { !it.value }?.key ?: ""
            deniedPermission = denied
            showPermissionDialog = true
        }
    }

    // UDP 发现实例
    val udpDiscovery = remember { UdpDiscovery(context) }

    // 有权限后自动启动发现
    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            val deviceName = android.os.Build.MODEL ?: "未知设备"
            udpDiscovery.onRoomsChanged = { rooms ->
                discoveredRooms = rooms
            }
            udpDiscovery.start(deviceName)
            isDiscovering = true
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            udpDiscovery.release()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ═══ 标题栏 ═══
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .statusBarsPadding() // 避开手机状态栏
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text("← 返回", style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "📡 联机大厅",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        if (!hasPermission) {
            // ═══ 无权限提示 ═══
            PermissionRequiredCard(
                onRequestPermission = {
                    permissionLauncher.launch(
                        NetworkPermissionHelper.getRequiredPermissions().toTypedArray()
                    )
                }
            )
        } else {
            // ═══ 已有权限，显示房间列表 ═══
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // 发现状态
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 闪烁圆点表示正在搜索
                    BlinkingDot()
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isDiscovering) "正在搜索附近房间..." else "未在搜索",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "找到 ${discoveredRooms.size} 个房间",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 创建房间按钮
                Button(
                    onClick = onCreateRoom,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("🏠 创建房间（当房主）", style = MaterialTheme.typography.titleMedium)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 房间列表
                if (discoveredRooms.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "🔍",
                                style = MaterialTheme.typography.displayMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "正在搜索附近的房间...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "请确保对方已创建房间\n且在同一 WiFi 下",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    Text(
                        text = "可加入的房间：",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(discoveredRooms) { room ->
                            RoomCard(
                                room = room,
                                onClick = { onJoinRoom(room.hostAddress) }
                            )
                        }
                    }
                }
            }
        }
    }

    // 权限拒绝弹窗
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = {
                Text(
                    "需要权限才能联机",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = NetworkPermissionHelper.getPermissionRationale(deniedPermission),
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionDialog = false
                        NetworkPermissionHelper.openAppSettings(context)
                    },
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("去设置打开", style = MaterialTheme.typography.titleSmall)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("暂不联机")
                }
            }
        )
    }
}

// ═══ 子组件 ═══

@Composable
private fun PermissionRequiredCard(onRequestPermission: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "📶",
                    style = MaterialTheme.typography.displayLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "联机需要网络权限",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "需要允许「WiFi」和「附近设备」权限\n才能找到同一网络下的其他玩家",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onRequestPermission,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("打开权限", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun RoomCard(room: DiscoveredRoom, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 设备图标
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "📱",
                    style = MaterialTheme.typography.headlineSmall
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 房间信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = room.deviceName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = when (room.roomStatus) {
                        "waiting" -> "等待中 · ${room.playerCount}/${room.maxPlayers} 人"
                        "playing" -> "游戏中"
                        "full" -> "房间已满"
                        else -> room.roomStatus
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (room.roomStatus == "waiting")
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 加入按钮
            if (room.roomStatus == "waiting") {
                Button(
                    onClick = onClick,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("加入")
                }
            }
        }
    }
}

/**
 * 闪烁圆点（表示正在搜索）
 */
@Composable
private fun BlinkingDot() {
    var visible by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            visible = !visible
        }
    }

    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(
                if (visible) Color(0xFF4CAF50) else Color(0xFF4CAF50).copy(alpha = 0.3f)
            )
    )
}
