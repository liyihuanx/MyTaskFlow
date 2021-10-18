package com.liyihuanx.module_taskflow

/**
 * @author created by liyihuanx
 * @date 2021/10/15
 * @description: 类的描述
 */
interface ITaskCreator {
    fun createTask(taskName: String): Task
}