package com.shengji.app.net

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

/**
 * 网络权限管理器
 *
 * 老人向设计：大白话提示，权限拒绝时引导跳转系统设置。
 */
object NetworkPermissionHelper {

    /**
     * 获取需要申请的权限列表
     */
    fun getRequiredPermissions(): List<String> {
        val permissions = mutableListOf<String>()

        // 基础网络权限
        permissions.add(Manifest.permission.INTERNET)
        permissions.add(Manifest.permission.ACCESS_WIFI_STATE)
        permissions.add(Manifest.permission.ACCESS_NETWORK_STATE)
        permissions.add(Manifest.permission.CHANGE_WIFI_MULTICAST_STATE)

        // Android 13+ 附近设备权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add("android.permission.NEARBY_WIFI_DEVICES")
        }

        return permissions
    }

    /**
     * 检查是否所有权限都已授予
     */
    fun hasAllPermissions(context: Context): Boolean {
        return getRequiredPermissions().all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * 获取被拒绝的权限
     */
    fun getDeniedPermissions(context: Context): List<String> {
        return getRequiredPermissions().filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * 获取权限拒绝时的大白话说明
     */
    fun getPermissionRationale(permission: String): String {
        return when {
            permission.contains("NEARBY_WIFI_DEVICES") ->
                "需要「附近设备」权限才能找到同一 WiFi 下的其他玩家。\n\n请点击下方按钮去设置里打开。"
            permission.contains("WIFI") ->
                "需要「WiFi」权限才能进行局域网联机。\n\n请点击下方按钮去设置里打开。"
            permission.contains("NETWORK") ->
                "需要「网络」权限才能联机对战。\n\n请点击下方按钮去设置里打开。"
            else ->
                "需要相关权限才能联机对战。\n\n请点击下方按钮去设置里打开。"
        }
    }

    /**
     * 打开应用权限设置页面
     */
    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
