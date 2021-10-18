package com.liyihuanx.module_taskflow

import androidx.core.os.TraceCompat
import com.liyihuanx.module_taskflow.TaskRuntime.executeTask
import java.util.*
import kotlin.Comparator
import kotlin.collections.ArrayList

/**
 * @author created by liyihuanx
 * @date 2021/10/13
 * @description: 类的描述
 */
abstract class Task @JvmOverloads constructor(
    val id: String,
    val isAsyncTask: Boolean = false,
    val delayMills: Long = 0,
    var priority: Int = 0
) : Runnable, Comparable<Task> {
    // 任务执行时间.
    var executeTime: Long = 0
        protected set

    // 任务的状态
    var state: Int = TaskState.IDLE
        protected set

    // 当前task依赖了那些前置任务,只有当dependTasks集合中的所有任务执行完，当前才可以执行
    private val dependTasks: MutableList<Task> = ArrayList()

    // 后置任务
    val behindTasks: MutableList<Task> = ArrayList()

    // 用于运行时log统计输出,输出当前task依赖了 那些前置任务，这些前置任务的名称我们将它存储在这里
    val dependTasksName: MutableList<String> = ArrayList()

    // 任务执行时，监听状态
    private val taskListeners: MutableList<TaskListener> = ArrayList()

    // 用于输出task运行时的日志
    private var taskRuntimeListener: TaskRuntimeListener? = TaskRuntimeListener()


    fun addTaskListener(taskListener: TaskListener) {
        if (!taskListeners.contains(taskListener)) {
            taskListeners.add(taskListener)
        }
    }

    open fun start() {
        if (state != TaskState.IDLE) {
            throw RuntimeException("cannot run task $id again")
        }
        toStart()
        executeTime = System.currentTimeMillis()
        executeTask(this)
    }


    // 在executeTask方法中调用
    override fun run() {
        // 改变任务的状态--onstart onrunning onfinshed --通知后置任务去开始执行
        TraceCompat.beginSection(id)
        toRunning()

        //真正的执行初始化任务的代码的方法
        run(id)

        toFinish()

        notifyBehindTasks()
        recycle()

        TraceCompat.endSection()

    }

    private fun recycle() {
        dependTasks.clear()
        behindTasks.clear()
        taskListeners.clear()
        taskRuntimeListener = null
    }

    /**
     * 通知后置任务去尝试执行
     * 任务流：start -> block -> async -> end
     * 实际结构：
     * start#behindTasks
     *      -》block#behindTasks
     *              -》async#behindTasks
     *                      -》end
     * dependTask就是反过来的
     */
    private fun notifyBehindTasks() {
        if (behindTasks.isNotEmpty()) {
            if (behindTasks.size > 1) {
                Collections.sort(behindTasks, TaskRuntime.taskComparator)
            }
            // 遍历behindTask后置任务，告诉他们你的一个前置依赖任务已经执行完成了
            behindTasks.forEach {
                it.dependTaskFinished(this)
            }
        }
    }

    /**
     *  传入的是当前执行完毕的task的后置列表每个task
     *  有可能：A -> B
     *  同时 ：C -> B
     *  那么B就有两个前置任务，那么当B要执行时，得先判断A,C执行完了没有
     *  假设A执行完毕，则当前传入的task就是B
     */
    private fun dependTaskFinished(task: Task) {
        if (dependTasks.isEmpty()) {
            return
        }
        // A的dependTasks，移除掉他的前置任务
        dependTasks.remove(task)
        if (dependTasks.isEmpty()) {
            start()
        }
    }

    //给当前task添加一个前置的依赖任务
    open fun dependOn(dependTask: Task) {
        var task = dependTask
        if (dependTask != this) {
            if (dependTask is Project) {
                task = dependTask.endTask
            }
            dependTasks.add(task)
            dependTasksName.add(task.id)
            //当前task依赖了dependTask , 那么我们还需要吧dependTask-里面的behindTask添加进去当前的task
            if (!task.behindTasks.contains(this)) {
                task.behindTasks.add(this)
            }
        }
    }

    //给当前task移除一个前置依赖任务
    open fun removeDependence(dependTask: Task) {
        var task = dependTask
        if (dependTask != this) {
            if (dependTask is Project) {
                task = dependTask.endTask
            }
            dependTasks.remove(task)
            dependTasksName.remove(task.id)
            //把当前task从dependTask的 后置依赖任务集合behindTasks中移除
            //达到接触两个任务依赖关系的目的
            if (task.behindTasks.contains(this)) {
                task.behindTasks.remove(this)
            }
        }
    }

    //给当前任务添加后置依赖项
    //他和dependOn是相反的
    open fun behind(behindTask: Task) {
        var task = behindTask
        if (behindTask != this) {
            if (behindTask is Project) {
                task = behindTask.startTask
            }
            //这个是把behindTask添加到当前task的后面
            behindTasks.add(task)
            //把当前task添加到behindTask 的前面
            behindTask.dependOn(this)
        }
    }

    //给当前task移除- -个后置的任务
    open fun removeBehind(behindTask: Task) {
        var task = behindTask
        if (behindTask != this) {
            if (behindTask is Project) {
                task = behindTask.startTask
            }
            behindTasks.remove(task)
            behindTask.removeDependence(this)
        }
    }


    private fun toStart() {
        state = TaskState.START
        TaskRuntime.setStateInfo(this)

        taskListeners.forEach {
            it.onStart(this)
        }
        taskRuntimeListener?.onStart(this)
    }

    private fun toFinish() {
        state = TaskState.FINISHED
        TaskRuntime.setStateInfo(this)
        TaskRuntime.removeBlockTask(this.id)
        taskListeners.forEach {
            it.onFinished(this)
        }
        taskRuntimeListener?.onFinished(this)
    }

    private fun toRunning() {
        state = TaskState.RUNNING
        TaskRuntime.setStateInfo(this)
        TaskRuntime.setThreadName(this, Thread.currentThread().name)
        taskListeners.forEach {
            it.onRunning(this)
        }
        taskRuntimeListener?.onRunning(this)

    }

    abstract fun run(id: String)

    override fun compareTo(other: Task): Int {
        return Util.compareTask(this, other)
    }
}
