package com.liyihuanx.module_taskflow

import android.os.Looper
import androidx.annotation.MainThread

/**
 * @author created by liyihuanx
 * @date 2021/10/18
 * @description: 类的描述
 */
object TaskFlowManager {
    fun addBlockTask(taskId: String): TaskFlowManager {
        TaskRuntime.addBlockTask(taskId)
        return this
    }

    fun addBlockTasks(vararg taskIds: String): TaskFlowManager {
        TaskRuntime.addBlockTasks(*taskIds)
        return this
    }


    //project任务组，也有可能是独立的-个task
    @MainThread
    fun start(task: Task) {
        assert(Thread.currentThread() == Looper.getMainLooper().thread) {
            "start method must be invoke on MainThread"
        }

        val startTask = if (task is Project) task.startTask else task
        TaskRuntime.traversalDependencyTreeAndInit(startTask)
        startTask.start()

        while (TaskRuntime.hasBlockTasks()) {
            try {
                Thread.sleep(10)
            } catch (ex: Exception) {

            }
            //主线程唤醒之后，存在着等待队列的任务
            //那么让等待队列中的任务执行
            while (TaskRuntime.hasBlockTasks()) {
                TaskRuntime.runWaitingTasks()
            }

        }
    }
}