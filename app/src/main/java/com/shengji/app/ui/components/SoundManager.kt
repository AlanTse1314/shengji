package com.shengji.app.ui.components

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

/**
 * 游戏音效管理器
 *
 * 使用 SoundPool 播放本地音效资源，延迟低。
 * 音效文件放在 res/raw/ 目录下。
 */
class SoundManager(context: Context) {

    private val soundPool: SoundPool
    private var playCardId: Int = 0
    private var winId: Int = 0
    private var loseId: Int = 0
    private var invalidId: Int = 0
    private var loaded = false

    init {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(attrs)
            .build()

        soundPool.setOnLoadCompleteListener { _, _, _ -> loaded = true }

        // 加载音效资源（如果存在）
        try {
            playCardId = loadSound(context, "play_card")
            winId = loadSound(context, "win")
            loseId = loadSound(context, "lose")
            invalidId = loadSound(context, "invalid")
        } catch (_: Exception) {
            // 音效资源不存在时静默失败
        }
    }

    private fun loadSound(context: Context, name: String): Int {
        val resId = context.resources.getIdentifier(name, "raw", context.packageName)
        return if (resId != 0) soundPool.load(context, resId, 1) else 0
    }

    fun playCard() { if (loaded && playCardId != 0) soundPool.play(playCardId, 1f, 1f, 1, 0, 1f) }
    fun win() { if (loaded && winId != 0) soundPool.play(winId, 1f, 1f, 1, 0, 1f) }
    fun lose() { if (loaded && loseId != 0) soundPool.play(loseId, 1f, 1f, 1, 0, 1f) }
    fun invalid() { if (loaded && invalidId != 0) soundPool.play(invalidId, 0.5f, 0.5f, 1, 0, 1f) }

    fun release() {
        soundPool.release()
    }
}
