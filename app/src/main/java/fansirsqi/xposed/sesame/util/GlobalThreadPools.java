package fansirsqi.xposed.sesame.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 全局线程池管理类
 */
public class GlobalThreadPools {
    private static final String TAG = "GlobalThreadPools";

    // 创建一个可缓存的线程池
    private static final ExecutorService THREAD_POOL = new ThreadPoolExecutor(
            5,                       // 核心线程数
            20,                      // 最大线程数
            60L,                     // 空闲线程存活时间
            TimeUnit.SECONDS,        // 时间单位
            new LinkedBlockingQueue<>(100), // 工作队列
            new ThreadFactory() {    // 线程工厂
                private final AtomicInteger threadNumber = new AtomicInteger(1);
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "SesamePool-" + threadNumber.getAndIncrement());
                    if (t.isDaemon()) {
                        t.setDaemon(false);
                    }
                    if (t.getPriority() != Thread.NORM_PRIORITY) {
                        t.setPriority(Thread.NORM_PRIORITY);
                    }
                    return t;
                }
            }
    );

    /**
     * 在线程池中执行任务
     *
     * @param command 要执行的任务
     */
    public static void execute(Runnable command) {
        try {
            THREAD_POOL.execute(command);
        } catch (Exception e) {
            Log.error(TAG, "执行任务异常: " + e.getMessage());
            Log.printStackTrace(e);
            // 如果线程池执行失败，尝试在当前线程执行
            try {
                command.run();
            } catch (Exception ex) {
                Log.error(TAG, "在当前线程执行任务异常: " + ex.getMessage());
                Log.printStackTrace(ex);
            }
        }
    }

    /**
     * 使当前线程暂停指定的毫秒数。
     *
     * @param millis 毫秒数。
     */
    public static void sleep(long millis) {
        try {
//            Log.record("延时:"+millis+" ms");
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Log.error(TAG, "Thread sleep interrupted " + e.getMessage());
            Thread.currentThread().interrupt();
        } catch (Exception e1) {
            Log.printStackTrace(e1);
//            Thread.currentThread().interrupt();
            Log.error(TAG, "Thread sleep interrupted " + e1.getMessage());
        } catch (Throwable t) {
            Log.printStackTrace(t);
//            Thread.currentThread().interrupt();
            Log.error(TAG, "Thread sleep interrupted " + t.getMessage());
        }
    }

    public static void shutdownAndAwaitTermination(ExecutorService pool, long timeout, String poolName) {
        if (pool != null && !pool.isShutdown()) {
            pool.shutdown();
            try {
                if (!pool.awaitTermination(1, TimeUnit.SECONDS)) {
                    pool.shutdownNow();
                    if (!pool.awaitTermination(timeout, TimeUnit.SECONDS)) {
                        Log.runtime(TAG, "thread " + poolName + " can't close");
                    }
                }
            } catch (InterruptedException ie) {
                pool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    /**
     * 关闭线程池
     */
    public static void shutdown() {
        shutdownAndAwaitTermination(THREAD_POOL, 5, "THREAD_POOL");
    }
}