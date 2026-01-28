package com.lyj.bi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;

@Configuration
public class ThreadPoolExecutorConfig {


    @Bean
    public ThreadPoolExecutor threadPoolExecutor() {

        //1. 线程工厂
        ThreadFactory threadFactory = new ThreadFactory() {

            //线程编号
            private int count = 1;

            //创建新线程
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("线程："+count);
                count++;
                return thread;
            }
        };

        //2. 自定义线程池
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(2,4,100, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(4),threadFactory);
        return threadPoolExecutor;
    }
}
