package com.liyihuanx.module_taskflow

import android.util.SparseArray

/**
 * @author created by liyihuanx
 * @date 2021/10/15
 * @description: 类的描述
 */
class TaskRuntimeInfo(val task: Task) {
    val stateTime = SparseArray<Long>()
    var isBlockTask = false
    var threadName: String? = null

    fun setStateTime(@TaskState state: Int, time: Long) {
        stateTime.put(state, time)
    }

    fun isSameTask(task: Task?): Boolean {
        return task != null && this.task === task
    }

    override fun toString(): String {
        return "TaskRuntimeInfo(task=$task, stateTime=$stateTime, isBlockTask=$isBlockTask, threadName=$threadName)"
    }


}