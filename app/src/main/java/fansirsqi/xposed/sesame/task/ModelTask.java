package fansirsqi.xposed.sesame.task;

import android.annotation.SuppressLint;
import android.os.Build;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
// import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import fansirsqi.xposed.sesame.model.BaseModel;
import fansirsqi.xposed.sesame.model.Model;
import fansirsqi.xposed.sesame.model.ModelFields;
import fansirsqi.xposed.sesame.model.ModelType;
import fansirsqi.xposed.sesame.util.GlobalThreadPools;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.Notify;
import fansirsqi.xposed.sesame.util.StringUtil;
import lombok.Getter;
import lombok.Setter;

/**
 * 抽象任务模型类
 * 
 * 这是Sesame-TK框架中的核心任务执行类，提供了以下功能：
 * 1. 任务生命周期管理（启动、停止、暂停）
 * 2. 子任务管理（添加、移除、执行）
 * 3. 任务执行统计和监控
 * 4. 支持顺序和并行两种执行模式
 * 5. 线程池管理和任务调度
 * 6. 中断处理和错误恢复
 * 
 * 主要组件：
 * - MAIN_TASK_MAP: 跟踪正在运行的主任务
 * - MAIN_THREAD_POOL: 主任务线程池
 * - childTaskMap: 子任务映射表
 * - childTaskExecutor: 子任务执行器
 * - run_cents: 任务运行次数计数器
 * 
 * 使用方式：
 * 继承此类并实现抽象方法：getName(), getFields(), check(), run()
 * 
 * @author Sesame-TK Team
 */
public abstract class ModelTask extends Model {

    /** 主任务映射表，用于跟踪正在运行的任务及其对应的线程 */
    private static final Map<ModelTask, Thread> MAIN_TASK_MAP = new ConcurrentHashMap<>();

    /** 主任务线程池，用于执行异步任务 */
    private static final ThreadPoolExecutor MAIN_THREAD_POOL =
            new ThreadPoolExecutor(getModelArray().length, Integer.MAX_VALUE,
                    30L, TimeUnit.SECONDS,
                    new SynchronousQueue<>(),
                    new ThreadPoolExecutor.CallerRunsPolicy());
    /** 日志标签 */
    private static final String TAG = "ModelTask";

    /** 子任务映射表，存储当前任务的所有子任务 */
    private final Map<String, ChildModelTask> childTaskMap = new ConcurrentHashMap<>();

    /** 子任务执行器，负责管理和执行子任务 */
    private ChildTaskExecutor childTaskExecutor;

    /** 任务运行次数计数器 */
    private int run_cents = 0;

    @Getter
    private final Runnable mainRunnable = new Runnable() {
        private final ModelTask task = ModelTask.this;

        @Override
        public void run() {
            // 防止重复执行
            if (MAIN_TASK_MAP.get(task) != null) {
                return;
            }
            // 记录当前任务和线程的映射关系
            MAIN_TASK_MAP.put(task, Thread.currentThread());
            try {
                // 更新状态显示
                Notify.setStatusTextExec(task.getName());
                // 执行具体任务逻辑
                task.run();
            } catch (Exception e) {
                // 记录异常信息
                Log.printStackTrace(e);
            } finally {
                // 清理任务映射和状态
                MAIN_TASK_MAP.remove(task);
                Notify.updateNextExecText(-1);
            }
        }
    };

    /** 默认构造函数 */
    public ModelTask() {
    }

    /** 增加任务运行次数 */
    public void addRunCents() {
        run_cents += 1;
    }

    /** 获取任务运行次数 */
    public int getRunCents() {
        return run_cents;
    }

    /**
     * 准备任务执行环境
     */
    @Override
    public final void prepare() {
        childTaskExecutor = newTimedTaskExecutor();
    }

    /**
     * 获取任务ID
     *
     * @return 任务ID
     */
    /** 获取任务ID，默认使用toString()方法 */
    public String getId() {
        return toString();
    }

    /**
     * 获取任务类型
     *
     * @return 任务类型为TASK
     */
    /** 获取模型类型，固定返回TASK */
    public ModelType getType() {
        return ModelType.TASK;
    }

    /**
     * 获取任务名称
     *
     * @return 任务名称
     */
    /** 获取任务名称，子类必须实现 */
    public abstract String getName();

    /**
     * 获取任务的字段
     *
     * @return 任务字段
     */
    /** 获取任务字段配置，子类必须实现 */
    public abstract ModelFields getFields();

    /**
     * 检查任务是否可执行
     *
     * @return Boolean值，表示是否通过检查
     */
    /** 检查任务是否可以执行，子类必须实现 */
    public abstract Boolean check();

    /**
     * 是否为同步任务
     *
     * @return Boolean值，表示是否为同步任务
     */
    /** 判断是否为同步任务，默认返回true */
    public Boolean isSync() {
        return true;
    }

    /**
     * 执行任务
     */
    /** 执行任务的具体逻辑，子类必须实现 */
    public abstract void run();

    /**
     * 检查任务是否包含指定的子任务
     *
     * @param childId 子任务ID
     * @return 是否包含该子任务
     */
    /** 检查是否存在指定ID的子任务 */
    public Boolean hasChildTask(String childId) {
        return childTaskMap.containsKey(childId);
    }

    /**
     * 获取指定ID的子任务
     *
     * @param childId 子任务ID
     * @return 子任务对象
     */
    // public ChildModelTask getChildTask(String childId) {
    //     return childTaskMap.get(childId);
    // }

    /**
     * 添加子任务
     * * 支持Android N及以上版本的并发安全操作
     * @param childTask 要添加的子任务
     */
    public void addChildTask(ChildModelTask childTask) {
        String childId = childTask.getId();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            childTaskMap.compute(childId, (key, value) -> {
                if (value != null) {
                    value.cancel();
                }
                childTask.setModelTask(this);
                if (childTaskExecutor.addChildTask(childTask)) {
                    return childTask;
                }
                return null;
            });
        } else {
            synchronized (childTaskMap) {
                ChildModelTask oldTask = childTaskMap.get(childId);
                if (oldTask != null) {
                    oldTask.cancel();
                }
                childTask.setModelTask(this);
                if (childTaskExecutor.addChildTask(childTask)) {
                    childTaskMap.put(childId, childTask);
                }
            }
        }
    }

    /**
     * 移除指定ID的子任务
     *
     * @param childId 子任务ID
     */
    public void removeChildTask(String childId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            childTaskMap.compute(childId, (key, value) -> {
                if (value != null) {
                    childTaskExecutor.removeChildTask(value);
                }
                return null;
            });
        } else {
            synchronized (childTaskMap) {
                ChildModelTask childTask = childTaskMap.get(childId);
                if (childTask != null) {
                    childTaskExecutor.removeChildTask(childTask);
                }
                childTaskMap.remove(childId);
            }
        }
    }

    /**
     * 获取当前任务的子任务数量
     *
     * @return 子任务数量
     */
    // public Integer countChildTask() {
    //     return childTaskMap.size();
    // }

    /**
     * 启动任务
     *
     * @return 是否成功启动任务
     */
    // public Boolean startTask() {
    //     return startTask(false);
    // }

    /**
     * 启动任务
     *
     * @param force 是否强制启动
     * @return 是否成功启动任务
     */
    public synchronized Boolean startTask(Boolean force) {
        if (MAIN_TASK_MAP.containsKey(this)) {
            if (!force) {
                return false;
            }
            stopTask();
        }
        try {
            if (isEnable() && check()) {
                if (isSync()) {
                    // Log.record("run task sync");
                    mainRunnable.run();
                } else {
                    Log.record("add task to thread pool");
                    MAIN_THREAD_POOL.execute(mainRunnable);
                }
                return true;
            }
        } catch (Exception e) {
            Log.printStackTrace(e);
        }
        return false;
    }

    /**
     * 停止当前任务及其所有子任务
     */
    public synchronized void stopTask() {
        for (ChildModelTask childModelTask : childTaskMap.values()) {
            try {
                childModelTask.cancel();
            } catch (Exception e) {
                Log.printStackTrace(e);
            }
        }

        if (childTaskExecutor != null) {
            childTaskExecutor.clearAllChildTask();
        }
        childTaskMap.clear();
        MAIN_THREAD_POOL.remove(mainRunnable);
        MAIN_TASK_MAP.remove(this);
    }

    /**
     * 启动所有任务
     */
    // public static void startAllTask() {
    //     startAllTask(false);
    // }

    /**
     * 启动所有任务
     *
     * @param force 是否强制启动
     */
    /**
     * 任务执行模式
     */
    public enum TaskExecutionMode {
        SEQUENTIAL,  // 顺序执行
        PARALLEL     // 并行执行
    }

    /**
     * 任务执行统计类
     */
    public static class TaskExecutionStats {
        private final long startTime;
        private long endTime;
        private final Map<String, Long> taskExecutionTimes = new ConcurrentHashMap<>();
        private final AtomicInteger successCount = new AtomicInteger(0);
        private final AtomicInteger failureCount = new AtomicInteger(0);
        private final AtomicInteger skippedCount = new AtomicInteger(0);

        public TaskExecutionStats() {
            this.startTime = System.currentTimeMillis();
        }
        
        public void recordTaskStart(String taskName) {
            taskExecutionTimes.put(taskName, System.currentTimeMillis());
        }
        
        public void recordTaskEnd(String taskName, boolean success) {
            Long startTime = taskExecutionTimes.get(taskName);
            if (startTime != null) {
                long executionTime = System.currentTimeMillis() - startTime;
                if (success) {
                    successCount.incrementAndGet();
                    Log.debug("任务[" + taskName + "]执行成功，耗时: " + executionTime + "ms");
                } else {
                    failureCount.incrementAndGet();
                    Log.error("任务[" + taskName + "]执行失败，耗时: " + executionTime + "ms");
                }
            }
        }
        
        public void recordSkipped(String taskName) {
            skippedCount.incrementAndGet();
            Log.debug("任务[" + taskName + "]被跳过");
        }
        
        public void complete() {
            this.endTime = System.currentTimeMillis();
        }
        
        @SuppressLint("DefaultLocale")
        public String getSummary() {
            long totalTime = endTime - startTime;
            return String.format("任务执行统计 - 总耗时: %dms, 成功: %d, 失败: %d, 跳过: %d",
                    totalTime, successCount.get(), failureCount.get(), skippedCount.get());
        }
    }

    /**
     * 并行任务执行器
     */
    public static class ParallelTaskExecutor {
        // 创建可配置的线程池
        private static final ThreadPoolExecutor PARALLEL_EXECUTOR = new ThreadPoolExecutor(
                // 核心线程数 - 可根据CPU核心数动态设置
                Math.max(2, Runtime.getRuntime().availableProcessors()),
                // 最大线程数
                Math.max(4, Runtime.getRuntime().availableProcessors() * 2),
                // 空闲线程存活时间
                60L, TimeUnit.SECONDS,
                // 工作队列
                new LinkedBlockingQueue<>(100),
                // 线程工厂
                r -> {
                    Thread t = new Thread(r, "parallel-task-thread");
                    t.setDaemon(true); // 设为守护线程
                    return t;
                },
                // 拒绝策略
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        
        /**
         * 并行执行任务列表
         * @param tasks 待执行的任务列表
         * @param force 是否强制执行
         * @return 成功执行的任务数量
         */
        /*
         public static int executeTasksInParallel(List<ModelTask> tasks, boolean force) {
            if (tasks == null || tasks.isEmpty()) {
                return 0;
            }
            
            // 使用CountDownLatch等待所有任务完成
            CountDownLatch latch = new CountDownLatch(tasks.size());
            AtomicInteger successCount = new AtomicInteger(0);
            
            // 提交所有任务到线程池
            for (ModelTask task : tasks) {
                PARALLEL_EXECUTOR.execute(() -> {
                    try {
                        task.addRunCnts();
                        if (task.isEnable() && task.check() && task.startTask(force)) {
                            successCount.incrementAndGet();
                            Log.record("并行执行任务[" + task.getName() + "]成功");
                        }
                    } catch (Exception e) {
                        Log.error("并行执行任务[" + task.getName() + "]失败: " + e.getMessage());
                        Log.printStackTrace(e);
                    } finally {
                        latch.countDown();
                    }
                });
            }
            
            try {
                // 等待所有任务完成，设置最大等待时间
                boolean allCompleted = latch.await(5, TimeUnit.MINUTES);
                if (!allCompleted) {
                    Log.record(TAG,"部分任务执行超时，继续执行下一轮");
                }
            } catch (InterruptedException e) {
                Log.error(TAG,"等待任务完成被中断: " + e.getMessage());
                Thread.currentThread().interrupt();
            }
            
            return successCount.get();
        }
        */
        
        /**
         * 关闭执行器
         */
        /*
        public static void shutdown() {
            PARALLEL_EXECUTOR.shutdown();
            try {
                if (!PARALLEL_EXECUTOR.awaitTermination(30, TimeUnit.SECONDS)) {
                    PARALLEL_EXECUTOR.shutdownNow();
                }
            } catch (InterruptedException e) {
                PARALLEL_EXECUTOR.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        */
    }

    /**
     * 按优先级分组收集任务
     */
    private static Map<Integer, List<ModelTask>> collectTasksByPriorityGroups() {
        Map<Integer, List<ModelTask>> priorityGroups = new HashMap<>();
        
        for (Model model : getModelArray()) {
            if (model != null && ModelType.TASK == model.getType()) {
                ModelTask task = (ModelTask) model;
                int priority = model.getPriority();
                
                priorityGroups.computeIfAbsent(priority, k -> new ArrayList<>()).add(task);
            }
        }
        
        return priorityGroups;
    }

    /**
     * 启动所有任务 - 顺序执行版本（原始实现）
     */
    public static void startAllTaskSequential(Boolean force) {
        Notify.setStatusTextExec();
        TaskExecutionStats stats = new TaskExecutionStats();
        
        try {
            for (int run_cnt = 1; run_cnt <= 2; run_cnt++) {
                Log.record(TAG,"第" + run_cnt + "轮开始 (顺序执行)");
                for (Model model : getModelArray()) {
                    if (model != null && ModelType.TASK == model.getType()) {
                        ModelTask task = (ModelTask) model;
                        String taskName = task.getName();
                        task.addRunCents();
                        int model_priority = model.getPriority();

                        if (run_cnt < model_priority) {
                            stats.recordSkipped(taskName);
                            Log.record(TAG,"模块[" + taskName + "]优先级:" + model_priority + " 第" + run_cnt + "轮跳过");
                            continue;
                        }
                        try {
                            stats.recordTaskStart(taskName);
                            boolean success = task.startTask(force);
                            stats.recordTaskEnd(taskName, success);
                            if (success) {
                                GlobalThreadPools.sleep(10);
                            }
                        } catch (Exception e) {
                            Log.error(TAG,"执行任务[" + taskName + "]时发生错误: " + e.getMessage());
                            Log.printStackTrace(e);
                            stats.recordTaskEnd(taskName, false);
                        }
                    }
                }
                Log.record(TAG,"第" + run_cnt + "轮结束");
            }
        } catch (Exception e) {
            Log.error(TAG,"顺序启动任务时发生错误: " + e.getMessage());
            Log.printStackTrace(e);
        } finally {
            stats.complete();
            Log.record(stats.getSummary());
        }
    }

    /**
     * 启动所有任务 - 并行执行版本
     */
    public static void startAllTaskParallel(Boolean force) {
        Notify.setStatusTextExec();
        TaskExecutionStats stats = new TaskExecutionStats();
        
        try {
            // 按优先级分组收集任务
            Map<Integer, List<ModelTask>> priorityGroups = collectTasksByPriorityGroups();
            
            // 按优先级顺序执行任务组（优先级间串行，同优先级内并行）
            for (int run_cnt = 1; run_cnt <= 2; run_cnt++) {
                Log.record(TAG,"第" + run_cnt + "轮开始 (并行执行)");
                
                List<ModelTask> currentRoundTasks = priorityGroups.getOrDefault(run_cnt, Collections.emptyList());
                assert currentRoundTasks != null;
                if (currentRoundTasks.isEmpty()) {
                    Log.record(TAG,"第" + run_cnt + "轮没有匹配的任务");
                    continue;
                }
                
                CountDownLatch latch = new CountDownLatch(currentRoundTasks.size());
                
                for (ModelTask task : currentRoundTasks) {
                    int finalRun_cnt = run_cnt;
                    ParallelTaskExecutor.PARALLEL_EXECUTOR.execute(() -> {
                        String taskName = task.getName();
                        try {
                            task.addRunCents();
                            int taskPriority = task.getPriority();
                            
                            if (finalRun_cnt < taskPriority) {
                                stats.recordSkipped(taskName);
                                Log.record(TAG,"模块[" + taskName + "]优先级:" + taskPriority + " 第" + finalRun_cnt + "轮跳过");
                            } else {
                                stats.recordTaskStart(taskName);
                                boolean success = task.startTask(force);
                                stats.recordTaskEnd(taskName, success);
                            }
                        } catch (Exception e) {
                            Log.error(TAG,"执行任务[" + taskName + "]时发生错误: " + e.getMessage());
                            Log.printStackTrace(e);
                            stats.recordTaskEnd(taskName, false);
                        } finally {
                            latch.countDown();
                        }
                    });
                }
                
                try {
                    Thread currentThread = Thread.currentThread();
                    String threadName = currentThread.getName();
                    long threadId = currentThread.threadId();
                    Log.error(TAG, "开始等待第" + run_cnt + "轮任务完成，线程信息: [ID=" + threadId + ", Name=" + threadName + "]");
                    boolean completed = latch.await(10, TimeUnit.MINUTES);
                    if (!completed) {
                        Log.error(TAG, "等待任务超过10分钟，部分任务可能未完成，线程信息: [ID=" + threadId + ", Name=" + threadName + "]");
                    } else {
                        Log.debug(TAG, "第" + run_cnt + "轮所有任务已完成，线程信息: [ID=" + threadId + ", Name=" + threadName + "]");
                    }
                } catch (InterruptedException e) {
                    Thread currentThread = Thread.currentThread();
                    Log.error(TAG, "第" + run_cnt + "轮任务等待被中断，但继续等待: " + e.getMessage() );
                    try {
                        // 重新等待，缩短超时时间到60秒
                        boolean completed = latch.await(60, TimeUnit.SECONDS);
                        if (!completed) {
                            Log.error(TAG, "重新等待后仍超时，线程信息: [ID=" + currentThread.threadId() + ", Name=" + currentThread.getName() + "]");
                        } else {
                            Log.debug(TAG, "重新等待后任务完成，线程信息: [ID=" + currentThread.threadId() + ", Name=" + currentThread.getName() + "]");
                        }
                        Thread.currentThread().interrupt();
                    } catch (InterruptedException e2) {
                        Log.error(TAG, "重新等待时再次被中断，放弃等待");
                    }
                }
                
                Log.record(TAG,"第" + run_cnt + "轮结束");
            }
            } catch (Exception e) {
            Log.error("并行启动任务时发生错误: " + e.getMessage());
            Log.printStackTrace(e);
        } finally {
            stats.complete();
            Log.record(stats.getSummary());
        }
    }

    /**
     * 启动所有任务，支持选择执行模式
     */
    public static void startAllTask(Boolean force, TaskExecutionMode mode) {
        if (mode == TaskExecutionMode.PARALLEL) {
            startAllTaskParallel(force);
        } else {
            startAllTaskSequential(force);
        }
    }

    /**
     * 启动所有任务（默认使用顺序执行模式，保持向后兼容）
     */
    public static void startAllTask(Boolean force) {
        // 默认使用顺序执行模式，可以通过配置更改
        startAllTask(force, TaskExecutionMode.SEQUENTIAL);
    }

    /**
     * 停止所有任务
     */
    public static void stopAllTask() {
        for (Model model : getModelArray()) {
            if (model != null) {
                try {
                    if (ModelType.TASK == model.getType()) {
                        ((ModelTask) model).stopTask();
                    }
                } catch (Exception e) {
                    Log.printStackTrace(e);
                }
            }
        }
    }

    /**
     * 创建一个新的子任务执行器
     *
     * @return 子任务执行器
     */
    private ChildTaskExecutor newTimedTaskExecutor() {
        ChildTaskExecutor childTaskExecutor;
        Integer timedTaskModel = BaseModel.getTimedTaskModel().getValue();
        if (timedTaskModel == BaseModel.TimedTaskModel.SYSTEM) {
            childTaskExecutor = new SystemChildTaskExecutor();
        } else if (timedTaskModel == BaseModel.TimedTaskModel.PROGRAM) {
            childTaskExecutor = new ProgramChildTaskExecutor();
        } else {
            throw new RuntimeException("not found childTaskExecutor");
        }
        return childTaskExecutor;
    }

    @Getter
    public static class ChildModelTask implements Runnable {
        @Setter
        private ModelTask modelTask;
        private final String id;
        private final String group;
        private final Runnable runnable;
        private final Long execTime;
        private CancelTask cancelTask;
        private Boolean isCancel = false;

        protected ChildModelTask(String id, long execTime) {
            this(id, null, null, execTime);
        }

        public ChildModelTask(String id, Runnable runnable) {
            this(id, null, runnable, 0L);
        }

        // public ChildModelTask(String id, String group, Runnable runnable) {
        //     this(id, group, runnable, 0L);
        // }

        public ChildModelTask(String id, String group, Runnable runnable, Long execTime) {
            if (StringUtil.isEmpty(id)) {
                id = toString();
            }
            if (StringUtil.isEmpty(group)) {
                group = "DEFAULT";
            }
            if (runnable == null) {
                runnable = setRunnable();
            }
            this.id = id;
            this.group = group;
            this.runnable = runnable;
            this.execTime = execTime;
        }

        /**
         * 设置子任务的运行逻辑
         *
         * @return 子任务的运行逻辑
         */
        public Runnable setRunnable() {
            return null;
        }

        /**
         * 执行子任务
         */
        public final void run() {
            getRunnable().run();
        }

        /**
         * 设置取消任务的逻辑
         *
         * @param cancelTask 取消任务的逻辑
         */
        protected void setCancelTask(CancelTask cancelTask) {
            this.cancelTask = cancelTask;
        }

        /**
         * 取消子任务
         */
        public final void cancel() {
            if (getCancelTask() != null) {
                try {
                    getCancelTask().cancel();
                    setCancel(true);
                } catch (Exception e) {
                    Log.printStackTrace(e);
                }
            }
        }

        public Boolean getCancel() {
            return isCancel;
        }

        public void setCancel(Boolean cancel) {
            isCancel = cancel;
        }
    }

    public interface CancelTask {
        void cancel();
    }
}
