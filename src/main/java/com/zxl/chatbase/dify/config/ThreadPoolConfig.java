package com.zxl.chatbase.dify.config;

import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;


public class ThreadPoolConfig {

    /**
     * 创建自定义线程池
     * @return 配置好的线程池
     */
    public static ThreadPoolExecutor createCustomThreadPool() {
        // 核心线程数
        int corePoolSize = 5;
        // 最大线程数
        int maximumPoolSize = 10;
        // 空闲线程存活时间
        long keepAliveTime = 60;
        // 时间单位
        TimeUnit unit = TimeUnit.SECONDS;
        // 任务队列（有界队列，容量为100）
        BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(100);
        // 线程工厂（自定义名称）
        ThreadFactory threadFactory = new CustomThreadFactory("business-thread");
        // 拒绝策略（调用者运行）
        RejectedExecutionHandler handler = new ThreadPoolExecutor.CallerRunsPolicy();

        return new ThreadPoolExecutor(
                corePoolSize,
                maximumPoolSize,
                keepAliveTime,
                unit,
                workQueue,
                threadFactory,
                handler
        );
    }

    /**
     * 自定义线程工厂
     */
    static class CustomThreadFactory implements ThreadFactory {
        private final String namePrefix;
        private int threadCount = 1;

        public CustomThreadFactory(String namePrefix) {
            this.namePrefix = namePrefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setName(namePrefix + "-" + threadCount++);
            thread.setDaemon(false); // 设置为非守护线程
            return thread;
        }
    }


}