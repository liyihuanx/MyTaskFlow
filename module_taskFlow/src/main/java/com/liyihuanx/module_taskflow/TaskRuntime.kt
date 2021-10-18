package com.liyihuanx.module_taskflow

import android.text.TextUtils
import android.util.Log
import java.util.*
import kotlin.Comparator
import kotlin.collections.HashMap
import kotlin.collections.LinkedHashSet

/**
 * @author created by liyihuanx
 * @date 2021/10/15
 * @description: 类的描述
 */
internal object TaskRuntime {

    //通过addBlockTask (String name) 指定启动阶段需要阻塞完成的任务，只有当blockTasksId当中的任务都执行完了
    //才会释放application的阻塞，才会拉起launchActivity
    val blockTasksId: MutableList<String> = mutableListOf()

    //如果blockTasksId集合中的任务还没有完成，那么在主线程中执行的任务会被添加到wai tingTasks集合里面去
    //目的是为了优先保证阻塞任务的优先完成，尽可能早的拉起launchActivity
    val waitingTasks: MutableList<Task> = mutableListOf()

    //记录下启动阶段所有任务的运行时信息key是taskId
    val taskRuntimeInfos: MutableMap<String, TaskRuntimeInfo> = HashMap()

    val taskComparator = Comparator<Task> { task1, task2 -> Util.compareTask(task1, task2) }


    @JvmStatic
    fun addBlockTask(id: String) {
        if (!TextUtils.isEmpty(id)) {
            blockTasksId.add(id)
        }
    }

    @JvmStatic
    fun addBlockTasks(vararg ids: String) {
        ids.takeIf { it.isNotEmpty() }?.forEach {
            addBlockTask(it)
        }
    }

    @JvmStatic
    fun removeBlockTask(id: String) {
        blockTasksId.remove(id)
    }

    @JvmStatic
    fun hasBlockTasks(): Boolean {
        return blockTasksId.iterator().hasNext()
    }

    @JvmStatic
    fun hasWaitingTasks(): Boolean {
        return waitingTasks.iterator().hasNext()
    }


    @JvmStatic
    fun setThreadName(task: Task, threadName: String?) {
        val taskRuntimeInfo = getTaskRuntimeInfo(task.id)
        taskRuntimeInfo?.threadName = threadName
    }

    @JvmStatic
    fun setStateInfo(task: Task) {
        val taskRuntimeInfo = getTaskRuntimeInfo(task.id)
        taskRuntimeInfo?.setStateTime(task.state, System.currentTimeMillis())
    }

    @JvmStatic
    fun getTaskRuntimeInfo(id: String): TaskRuntimeInfo? {
        return taskRuntimeInfos[id]
    }

    fun executeTask(task: Task) {
        if (task.isAsyncTask) {
            ExecutorHelper.execute(runnable = task)
        } else {
            //else里面的都是在主线程执行的
            //延迟任务，但是如果这个延迟任务它存在着后置任务A(延迟任务)-->B--->C (Block task)
            if (task.delayMills > 0 && !hasBlockBehindTask(task)) {
                // 执行这里就说明后置任务没有阻塞任务，可以直接做延迟
                MainHandler.postDelay(task.delayMills, task)
                return
            }
            // 如果阻塞任务列表接下来没有阻塞任务就执行，有阻塞任务就加到等待队列中
            return if (!hasBlockTasks()) {
                task.run()
            } else {
                addWaitingTask(task)
            }
        }
    }

    //把一个主线程上需要执行的任务，但又不影响launchActivity的启动，添加到等待队列
    private fun addWaitingTask(task: Task) {
        if (!waitingTasks.contains(task)) {
            waitingTasks.add(task)
        }
    }

    /**
     * 检测一个延迟任务是否存在着后置的阻塞任务(就是等他们都执行完了，
     * 才会释放application的阻塞，才会拉起launchActivity)
     */
    private fun hasBlockBehindTask(task: Task): Boolean {
        // 如果是startTask 跳过
        if (task is Project.CriticalTask) {
            return false
        }
        // 那到当前task的后面的任务列表
        val behindTasks = task.behindTasks
        for (behindTask in behindTasks) {
            //需要判断一个task是不是阻塞任务，blockTaskIds
            val behindTaskInfo = getTaskRuntimeInfo(behindTask.id)
            return if (behindTaskInfo != null && behindTaskInfo.isBlockTask) {
                true
            } else {
                // 有可能当前任务中，后置任务还有后置任务是阻塞任务，所以做递归
                hasBlockBehindTask(behindTask)
            }
        }
        return false
    }

    //校验依赖树中是否存在环形依赖校验，依赖树中是否存 在taskId相同的任务初始化task 对应taskRuntimeInfo
    //遍历依赖树完成启动前的检查和初始化
    @JvmStatic
    fun traversalDependencyTreeAndInit(task: Task) {
        val traversalVisitor = linkedSetOf<Task>()
        traversalVisitor.add(task)
        innerTraversalDependencyTreeAndInit(task, traversalVisitor)

        val iterator = blockTasksId.iterator()
        while (iterator.hasNext()) {
            val taskId = iterator.next()
            //检查这个阻塞任务是否存在依赖树中
            if (!taskRuntimeInfos.containsKey(taskId)) {
                throw java.lang.RuntimeException("block task ${task.id} not in dependency tree.")
            } else {
                val task = getTaskRuntimeInfo(taskId)?.task
                traversalDependencyPriority(task)
            }
        }
    }

    // 优先级
    private fun traversalDependencyPriority(task: Task?) {
        if (task == null) return

    }

    private fun innerTraversalDependencyTreeAndInit(
        task: Task,
        traversalVisitor: LinkedHashSet<Task>
    ) {
        var taskRuntimeInfo = getTaskRuntimeInfo(task.id)
        if (taskRuntimeInfo == null) {
            taskRuntimeInfo = TaskRuntimeInfo(task)
            if (blockTasksId.contains(task.id)) {
                taskRuntimeInfo.isBlockTask = true
            }
            taskRuntimeInfos[task.id] = taskRuntimeInfo
        } else {
            if (!taskRuntimeInfo.isSameTask(task)) {
                throw RuntimeException("not allow to contain the same id ${task.id}")
            }
        }


        //校验环形依赖
        for (behindTask in task.behindTasks) {
            if (!traversalVisitor.contains(behindTask)) {
                traversalVisitor.add(behindTask)
            } else {
                throw RuntimeException("not allow loopback dependency ,task id =${task.id}")
            }
            //start --> taskI -->task2-->task3-->task4-->task5-->end
            //对task3 后面的依赖任务路径上的task做环形依赖检查初始化runtimeInfo信息
            if (behindTask.behindTasks.isEmpty()) {
                //behindTask =end
                val iterator = traversalVisitor.iterator()
                val builder: StringBuilder = StringBuilder()
                while (iterator.hasNext()) {
                    builder.append(iterator.next().id)
                    builder.append(" --> ")
                }
                Log.d("TaskFlow", "task-line: ${builder.toString()}")
            }
            innerTraversalDependencyTreeAndInit(behindTask, traversalVisitor)
            traversalVisitor.remove(behindTask)
        }
    }

    fun runWaitingTasks() {
        if (hasWaitingTasks()) {
            if (waitingTasks.size > 1) {
                Collections.sort(waitingTasks, taskComparator)
            }
            if (hasBlockTasks()) {
                val head = waitingTasks.removeAt(0)
                head.run()
            } else {
                for (waitingTask in waitingTasks) {
                    MainHandler.postDelay(waitingTask.delayMills, waitingTask)
                }
                waitingTasks.clear()
            }
        }
    }
}