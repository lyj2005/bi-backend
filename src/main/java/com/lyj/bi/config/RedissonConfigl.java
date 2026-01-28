package com.lyj.bi.config;//package com.lyj.bi.config;
//
//import org.redisson.Redisson;
//import org.redisson.api.RedissonClient;
//import org.redisson.config.Config;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.Primary;
//
///**
// * 线上连接redis
// */
//@Configuration
//public class RedissonConfigl {
//
//    @Bean
//    @Primary  // 确保这个bean优先使用
//    public RedissonClient redissonClient() {
//        System.out.println("=========================================");
//        System.out.println("🎯 强制硬编码 Redisson 配置生效！");
//        System.out.println("=========================================");
//
//        // 完全硬编码配置
//        Config config = new Config();
//
//        // 强制使用远程 Redis
//        String redisAddress = "redis://43.138.168.102:6379";
//        String redisPassword = "250329";
//        int redisDatabase = 1;
//
//        System.out.println("🚀 硬编码连接到: " + redisAddress);
//        System.out.println("🔑 使用数据库: " + redisDatabase);
//
//        config.useSingleServer()
//                .setAddress(redisAddress)
//                .setPassword(redisPassword)
//                .setDatabase(redisDatabase)
//                .setConnectTimeout(10000)
//                .setTimeout(3000)
//                .setRetryAttempts(3)
//                .setRetryInterval(1500);
//
//        RedissonClient client = Redisson.create(config);
//        System.out.println("✅ Redisson 客户端创建成功！");
//        return client;
//    }
//}