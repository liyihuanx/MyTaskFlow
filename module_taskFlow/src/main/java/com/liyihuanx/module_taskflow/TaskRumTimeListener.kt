package com.liyihuanx.module_taskflow

import android.util.Log
import com.liyihuanx.module_taskflow.TaskRuntime.getTaskRuntimeInfo

/**
 * @author created by liyihuanx
 * @date 2021/10/13
 * @description: 类的描述
 */
class TaskRuntimeListener : TaskListener {
    override fun onStart(task: Task) {

    }

    override fun onRunning(task: Task) {

    }

    override fun onFinished(task: Task) {
        logTaskRuntimeInfo(task)
    }

    private fun logTaskRuntimeInfo(task: Task) {
        val taskRuntimeInfo = getTaskRuntimeInfo(task.id) ?: return
        val startTime = taskRuntimeInfo.stateTime[TaskState.START]
        val runningTime = taskRuntimeInfo.stateTime[TaskState.RUNNING]
        val finishedTime = taskRuntimeInfo.stateTime[TaskState.FINISHED]
        val builder = StringBuilder()
        builder.append(WRAPPER)
        builder.append(TAG)
        builder.append(WRAPPER)
        builder.append(WRAPPER)
        builder.append(HALF_LINE)
        builder.append(if (task is Project) " Project" else "task [${task.id}] " + FINISHED_METHOD)
        builder.append(HALF_LINE)
        builder.append(WRAPPER)


        addTaskInfoLineInfo(builder, DEPENDENCIES, getTaskDependenciesInfo(task))
        addTaskInfoLineInfo(builder, IS_BLOCK_TASK, taskRuntimeInfo.isBlockTask.toString())
        addTaskInfoLineInfo(builder, THREAD_NAME, taskRuntimeInfo.threadName!!)
        addTaskInfoLineInfo(builder, START_TIME, startTime.toString() + "ms")
        addTaskInfoLineInfo(builder, WAITING_TIME, (runningTime - startTime).toString() + "ms")
        addTaskInfoLineInfo(builder, TASK_CONSUME, (finishedTime - runningTime).toString() + "ms")
        addTaskInfoLineInfo(builder, FINISHED_TIME, finishedTime.toString() + "ms")
        builder.append(HALF_LINE + HALF_LINE + HALF_LINE + HALF_LINE)
        builder.append(WRAPPER)
        Log.d(TAG, builder.toString())
    }

    private fun addTaskInfoLineInfo(
        builder: StringBuilder,
        key: String,
        value: String
    ) {
        builder.append("| $key: $value \n")
    }

    private fun getTaskDependenciesInfo(task: Task): String {
        val builder = StringBuilder()
        for (s in task.dependTasksName) {
            builder.append("$s ")
        }
        return builder.toString()
    }

    companion object {
        const val TAG: String = "TaskFlow"
        const val START_METHOD = " onStart "
        const val RUNNING_METHOD = " onRunning "
        const val FINISHED_METHOD = " onFinished "

        const val DEPENDENCIES = "依赖任务"
        const val THREAD_NAME = "线程名称"
        const val START_TIME = "开始执行时刻"
        const val WAITING_TIME = "等待执行耗时"
        const val TASK_CONSUME = "任务执行耗时"
        const val IS_BLOCK_TASK = "是否是阻塞任务"
        const val FINISHED_TIME = "任务结束时刻"
        const val WRAPPER = "\n"
        const val HALF_LINE = "=================="

    }

}
