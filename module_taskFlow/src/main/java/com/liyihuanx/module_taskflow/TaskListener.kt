package com.liyihuanx.module_taskflow

/**
 * @author created by liyihuanx
 * @date 2021/10/13
 * @description: 类的描述
 */
interface TaskListener {
    fun onStart(task: Task)
    fun onRunning(task: Task)
    fun onFinished(task: Task)
}
