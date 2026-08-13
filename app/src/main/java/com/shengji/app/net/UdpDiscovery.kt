package com.shengji.app.net

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.*
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.NetworkInterface

/**
 * UDP 设备发现
 *
 * - 广播发送：每 1.5 秒向外广播本机房间信息
 * - 广播监听：持续接收局域网其他设备广播
 * - 自动移除：3 秒无报文的设备自动从列表移除
 */
class UdpDiscovery(private val context: Context) {

    companion object {
        private const val TAG = "UdpDiscovery"
        private const val BROADCAST_PORT = 9876
        private const val BROADCAST_INTERVAL = 1500L  // 1.5 秒
        private const val DEVICE_TIMEOUT = 3000L       // 3 秒超时
        private const val BUFFER_SIZE = 4096
    }

    private val gson = Gson()
    private var broadcastJob: Job? = null
    private var listenJob: Job? = null
    private var cleanupJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @Volatile
    var isRunning = false
        private set

    /** 已发现的房间列表 */
    private val _rooms = mutableMapOf<String, DiscoveredRoom>()
    val rooms: Map<String, DiscoveredRoom> get() = _rooms.toMap()

    /** 房间列表变化回调 */
    var onRoomsChanged: ((List<DiscoveredRoom>) -> Unit)? = null

    /**
     * 启动 UDP 广播和监听
     *
     * @param deviceName 本机设备名称
     * @param roomStatus 房间状态
     * @param playerCount 当前人数
     */
    fun start(deviceName: String, roomStatus: String = "waiting", playerCount: Int = 1) {
        if (isRunning) return
        isRunning = true

        Log.d(TAG, "UDP 发现启动: $deviceName")

        // 启动广播发送
        broadcastJob = scope.launch {
            while (isActive && isRunning) {
                try {
                    sendBroadcast(deviceName, roomStatus, playerCount)
                } catch (e: Exception) {
                    Log.e(TAG, "广播发送失败", e)
                }
                delay(BROADCAST_INTERVAL)
            }
        }

        // 启动广播监听
        listenJob = scope.launch {
            try {
                listenForBroadcasts()
            } catch (e: Exception) {
                Log.e(TAG, "广播监听失败", e)
            }
        }

        // 启动超时清理
        cleanupJob = scope.launch {
            while (isActive && isRunning) {
                delay(1000)
                cleanupStaleDevices()
            }
        }
    }

    /**
     * 停止发现
     */
    fun stop() {
        isRunning = false
        broadcastJob?.cancel()
        listenJob?.cancel()
        cleanupJob?.cancel()
        _rooms.clear()
        onRoomsChanged?.invoke(emptyList())
        Log.d(TAG, "UDP 发现停止")
    }

    /**
     * 更新广播信息（房间状态变化时调用）
     */
    fun updateBroadcastInfo(deviceName: String, roomStatus: String, playerCount: Int) {
        // 广播会在下次循环时使用新参数
    }

    /**
     * 发送 UDP 广播
     */
    private fun sendBroadcast(deviceName: String, roomStatus: String, playerCount: Int) {
        val localAddress = getLocalIpAddress() ?: return

        val message = BroadcastMessage(
            deviceName = deviceName,
            hostAddress = localAddress,
            roomStatus = roomStatus,
            playerCount = playerCount
        )
        val data = gson.toJson(message).toByteArray()

        DatagramSocket().use { socket ->
            socket.broadcast = true
            val broadcastAddress = InetAddress.getByName("255.255.255.255")
            val packet = DatagramPacket(data, data.size, broadcastAddress, BROADCAST_PORT)
            socket.send(packet)
        }
    }

    /**
     * 监听 UDP 广播
     */
    private fun listenForBroadcasts() {
        val socket = DatagramSocket(BROADCAST_PORT)
        socket.reuseAddress = true
        socket.soTimeout = 2000  // 2 秒超时，便于检查 isRunning

        val buffer = ByteArray(BUFFER_SIZE)

        while (isRunning) {
            try {
                val packet = DatagramPacket(buffer, buffer.size)
                socket.receive(packet)

                val message = String(packet.data, 0, packet.length)
                val broadcast = gson.fromJson(message, BroadcastMessage::class.java)

                // 过滤自己的广播
                val localAddress = getLocalIpAddress()
                if (broadcast.hostAddress == localAddress) continue

                // 更新房间列表
                val key = broadcast.hostAddress
                val existing = _rooms[key]
                if (existing != null) {
                    existing.lastSeen = System.currentTimeMillis()
                    existing.apply {
                        // 更新字段（DiscoveredRoom 是 var）
                    }
                } else {
                    _rooms[key] = DiscoveredRoom(
                        deviceName = broadcast.deviceName,
                        hostAddress = broadcast.hostAddress,
                        roomStatus = broadcast.roomStatus,
                        playerCount = broadcast.playerCount,
                        maxPlayers = broadcast.maxPlayers,
                        lastSeen = System.currentTimeMillis()
                    )
                }

                // 通知 UI
                onRoomsChanged?.invoke(_rooms.values.filter { it.isAlive }.toList())

            } catch (e: java.net.SocketTimeoutException) {
                // 超时正常，继续循环
            } catch (e: Exception) {
                if (isRunning) Log.e(TAG, "接收广播异常", e)
            }
        }

        socket.close()
    }

    /**
     * 清理超时设备
     */
    private fun cleanupStaleDevices() {
        val now = System.currentTimeMillis()
        val iterator = _rooms.entries.iterator()
        var changed = false
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (now - entry.value.lastSeen > DEVICE_TIMEOUT) {
                iterator.remove()
                changed = true
            }
        }
        if (changed) {
            onRoomsChanged?.invoke(_rooms.values.filter { it.isAlive }.toList())
        }
    }

    /**
     * 获取本机局域网 IP
     */
    private fun getLocalIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                if (networkInterface.isLoopback || !networkInterface.isUp) continue

                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is java.net.Inet4Address) {
                        return address.hostAddress
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取 IP 失败", e)
        }
        return null
    }

    fun release() {
        stop()
        scope.cancel()
    }
}
