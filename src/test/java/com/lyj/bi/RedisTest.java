package com.lyj.bi;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;


/**
 * 测试线上redis是否连接成功
 */
@SpringBootTest(properties = "spring.profiles.active=prod")
public class RedisTest {

    @Autowired
    private RedissonClient redissonClient;

    @Test
    public void testOnlineRedisConnection() {
        // 1. 写入测试数据到线上 Redis
        redissonClient.getBucket("test_online_redis").set("hello_online_redis21");

        // 2. 从线上 Redis 读取数据
        String value = (String) redissonClient.getBucket("test_online_redis").get();
        System.out.println("📌 从线上 Redis 读取到数据：" + value);

        // 3. 删除测试数据（避免污染）
//        redissonClient.getBucket("test_online_redis").delete();

        // 4. 验证结果
        if ("hello_online_redis".equals(value)) {
            System.out.println("✅ 项目已成功连接线上 Redis！");
        } else {
            System.out.println("❌ 项目连接线上 Redis 失败！");
        }
    }
}
