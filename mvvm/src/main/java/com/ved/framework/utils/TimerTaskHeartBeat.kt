package com.ved.framework.utils

import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class TimerTaskHeartBeat{
    private var timerTask: TimerTask? = null
    private var heartbeatTimer: ScheduledExecutorService? = null

    fun startTimer(period: Float,callBack: () -> Unit) {
        try {
            if (heartbeatTimer == null || !(heartbeatTimer?.isShutdown == false && heartbeatTimer?.isTerminated == false)){
                heartbeatTimer = Executors.newScheduledThreadPool(5)
            }
            if (timerTask == null) {
                timerTask = object : TimerTask() {
                    override fun run() {
                        try {
                            callBack.invoke()
                        } catch (e: Exception) {
                            KLog.e(e.message)
                        }
                    }
                }
            }
            heartbeatTimer?.scheduleAtFixedRate(timerTask, 0, (period * 1000).toLong(), TimeUnit.MILLISECONDS)
        } catch (e: Exception) {
            stopTimer()
        }
    }

    fun stopTimer() {
        if (timerTask != null) {
            timerTask?.cancel()
            timerTask = null
        }
        if (heartbeatTimer != null) {
            heartbeatTimer?.shutdown()
            try {
                // 等待线程池终止
                if (heartbeatTimer?.awaitTermination(1, TimeUnit.SECONDS) == false) {
                    heartbeatTimer?.shutdownNow() // 强制终止
                }
            } catch (e: InterruptedException) {
                heartbeatTimer?.shutdownNow()
            }
            heartbeatTimer = null
        }
    }
}