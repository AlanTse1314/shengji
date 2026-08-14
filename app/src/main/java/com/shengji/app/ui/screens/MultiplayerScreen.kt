package com.shengji.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.sp
import com.shengji.app.net.*
import com.shengji.app.ui.utils.rememberScreenSize
import kotlinx.coroutines.delay

/**
 * 联机大厅 - 老人友好设计
 *
 * 极致简化：
 * - 大按钮、大字体
 * - 自动搜索，无需手动输入
 * - 点击房间即可加入
 * - 零配置，开箱即用
 */
@Composable
fun MultiplayerScreen(
    onBack: () -> Unit,
    onCreateRoom: () -> Unit,
    onJoinRoom: (String) -> Unit
) {
    val context = LocalContext.current
    val screenSize = rememberScreenSize()
    var hasPermission by remember { mutableStateOf(NetworkPermissionHelper.hasAllPermissions(context)) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var discoveredRooms by remember { mutableStateOf(listOf<DiscoveredRoom>()) }
    var isSearching by remember { mutableStateOf(false) }

    // 权限请求
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasPermission = results.values.all { it }
        if (!hasPermission) {
            showPermissionDialog = true
        }
    }

    // UDP 发现
    val udpDiscovery = remember { UdpDiscovery(context) }

    // 有权限后自动启动搜索
    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            val deviceName = android.os.Build.MODEL ?: "手机"
            udpDiscovery.onRoomsChanged = { rooms ->
                discoveredRooms = rooms
            }
            udpDiscovery.start(deviceName)
            isSearching = true
        }
    }

    DisposableEffect(Unit) {
        onDispose { udpDiscovery.release() }
    }

    // 大字体定义
    val titleSize = (screenSize.titleTextSize.value * 1.3f).sp
    val buttonSize = (screenSize.buttonTextSize.value * 1.2f).sp
    val labelSize = (screenSize.statusTextSize.value * 1.2f).sp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // ═══ 顶部返回按钮 ═══
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onBack,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    "← 返回",
                    fontSize = buttonSize,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // ═══ 主内容 ═══
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 标题
            Text(
                text = "📡 联机对战",
                fontSize = titleSize,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "和朋友一起玩",
                fontSize = labelSize,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (!hasPermission) {
                // ═══ 无权限提示 ═══
                NeedPermissionCard(
                    screenSize = screenSize,
                    onRequestPermission = {
                        permissionLauncher.launch(
                            NetworkPermissionHelper.getRequiredPermissions().toTypedArray()
                        )
                    }
                )
            } else {
                // ═══ 创建房间按钮 ═══
                BigButton(
                    text = "🏠 创建房间",
                    subtitle = "我来当房主",
                    fontSize = buttonSize,
                    screenSize = screenSize,
                    onClick = onCreateRoom
                )

                Spacer(modifier = Modifier.height(20.dp))

                // ═══ 分隔线 ═══
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Divider(modifier = Modifier.weight(1f))
                    Text(
                        text = "  或者加入别人的房间  ",
                        fontSize = labelSize,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Divider(modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ═══ 搜索状态 ═══
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    BlinkingDot()
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isSearching) "正在搜索附近房间..." else "未在搜索",
                        fontSize = labelSize,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ═══ 房间列表 ═══
                if (discoveredRooms.isEmpty()) {
                    // 没找到房间
                    NoRoomFoundCard(screenSize = screenSize)
                } else {
                    // 显示找到的房间
                    Text(
                        text = "找到 ${discoveredRooms.size} 个房间：",
                        fontSize = labelSize,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    discoveredRooms.forEach { room ->
                        RoomCard(
                            room = room,
                            fontSize = buttonSize,
                            onClick = { onJoinRoom(room.hostAddress) }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }

    // 权限拒绝弹窗
    if (showPermissionDialog) {
        PermissionDialog(
            screenSize = screenSize,
            onConfirm = {
                showPermissionDialog = false
                NetworkPermissionHelper.openAppSettings(context)
            },
            onDismiss = { showPermissionDialog = false }
        )
    }
}

// ═══════════════════════════════════════════
// 子组件
// ═══════════════════════════════════════════

/**
 * 大按钮 - 老人友好
 */
@Composable
private fun BigButton(
    text: String,
    subtitle: String,
    fontSize: androidx.compose.ui.unit.TextUnit,
    screenSize: com.shengji.app.ui.utils.ScreenSize,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(screenSize.buttonHeightLarge * 1.3f),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = text,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                fontSize = (fontSize.value * 0.75f).sp,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
            )
        }
    }
}

/**
 * 需要权限提示卡片
 */
@Composable
private fun NeedPermissionCard(
    screenSize: com.shengji.app.ui.utils.ScreenSize,
    onRequestPermission: () -> Unit
) {
    val buttonSize = (screenSize.buttonTextSize.value * 1.2f).sp

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "📶",
                fontSize = 48.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "需要网络权限",
                fontSize = buttonSize,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "允许后才能和朋友联机",
                fontSize = (buttonSize.value * 0.8f).sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onRequestPermission,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(screenSize.buttonHeightLarge),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "打开权限",
                    fontSize = buttonSize,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * 没找到房间提示
 */
@Composable
private fun NoRoomFoundCard(screenSize: com.shengji.app.ui.utils.ScreenSize) {
    val labelSize = (screenSize.statusTextSize.value * 1.2f).sp

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🔍",
                fontSize = 40.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "暂时没找到房间",
                fontSize = labelSize,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "请确保对方已创建房间\n并且在同一 WiFi 下",
                fontSize = (labelSize.value * 0.9f).sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * 房间卡片 - 大字体，点击即加入
 */
@Composable
private fun RoomCard(
    room: DiscoveredRoom,
    fontSize: androidx.compose.ui.unit.TextUnit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 设备图标
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "📱",
                    fontSize = 28.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 房间信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = room.deviceName,
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when (room.roomStatus) {
                        "waiting" -> "等待中 · ${room.playerCount}/${room.maxPlayers} 人"
                        "playing" -> "游戏中"
                        "full" -> "房间已满"
                        else -> room.roomStatus
                    },
                    fontSize = (fontSize.value * 0.8f).sp,
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
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "加入",
                        fontSize = (fontSize.value * 0.9f).sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * 权限弹窗 - 大白话
 */
@Composable
private fun PermissionDialog(
    screenSize: com.shengji.app.ui.utils.ScreenSize,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val buttonSize = (screenSize.buttonTextSize.value * 1.1f).sp

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "需要权限",
                fontSize = buttonSize,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "需要允许「网络」和「附近设备」权限\n才能和朋友联机对战\n\n请点击下方按钮去设置里打开",
                fontSize = (buttonSize.value * 0.9f).sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "去设置打开",
                    fontSize = buttonSize
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "暂不联机",
                    fontSize = buttonSize
                )
            }
        }
    )
}

/**
 * 闪烁圆点
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
            .size(12.dp)
            .clip(CircleShape)
            .background(
                if (visible) Color(0xFF4CAF50) else Color(0xFF4CAF50).copy(alpha = 0.3f)
            )
    )
}
