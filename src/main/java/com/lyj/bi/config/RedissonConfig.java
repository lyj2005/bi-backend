package com.lyj.bi.config;

import lombok.Data;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "spring.redis")
@Data
public class RedissonConfig {

    private String host;
    private Integer port;
    private String password;
    private Integer database;

    public RedissonClient redissonClient() {

        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://"+host+":"+port)
                .setDatabase(database)
                .setPassword(password);
        RedissonClient redissonClient = Redisson.create(config);
        return redissonClient;
    }
}

