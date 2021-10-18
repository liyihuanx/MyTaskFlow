package com.liyihuanx.module_taskflow

import androidx.annotation.IntDef

/**
 * @author created by liyihuanx
 * @date 2021/10/13
 * @description: 类的描述
 */
@Retention(AnnotationRetention.SOURCE)
@IntDef(TaskState.IDLE, TaskState.RUNNING, TaskState.FINISHED, TaskState.START)
annotation class TaskState {
    companion object {
        const val IDLE = 0 //静止
        const val START = 1 //启动,可能需要等待调度，
        const val RUNNING = 2 //运行
        const val FINISHED = 3 //运行结束
    }
}

