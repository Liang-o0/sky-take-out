package com.sky.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Component;


@Slf4j
@Configuration
public class RedisConfiguration {
    @Bean
    public RedisTemplate redisTemplate(RedisConnectionFactory redisConnectionFactory){
        log.info("开始创建redis模板对象....");
        RedisTemplate redisTemple = new RedisTemplate();
//        设置redis的连接工厂对象
        redisTemple.setConnectionFactory(redisConnectionFactory);
//        设置redis key的序列化器
        redisTemple.setKeySerializer(new StringRedisSerializer());
        return redisTemple;
    }
}
