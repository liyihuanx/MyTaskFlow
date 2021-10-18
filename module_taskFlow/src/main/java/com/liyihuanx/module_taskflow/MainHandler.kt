package com.liyihuanx.module_taskflow

import android.os.Handler
import android.os.Looper
import android.os.Message

/**
 * @author created by liyihuanx
 * @date 2021/10/15
 * @description: 类的描述
 */
object MainHandler {
    private val handler = Handler(Looper.getMainLooper())
    fun post(runnable: Runnable) {
        handler.post(runnable)
    }

    fun postDelay(delayMills: Long, runnable: Runnable) {
        handler.postDelayed(runnable, delayMills)
    }

    fun sendAtFrontOfQueue(runnable: Runnable) {
        val msg = Message.obtain(handler, runnable)
        handler.sendMessageAtFrontOfQueue(msg)
    }

    fun remove(runnable: Runnable) {
        handler.removeCallbacks(runnable)
    }

}