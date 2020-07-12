package com.lc.mallorder.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.validation.Valid;

@Component
@Data
public class Parameters {
//    /*****redis config start*******/
//    @Value("${redis.host}")
//    private String redisHost;
//    @Value("${redis.port}")
//    private int redisPort;
//    @Value("${redis.max-idle}")
//    private int redisMaxTotal;
//    @Value("${redis.max-total}")
//    private int redisMaxIdle;
//    @Value("${redis.max-wait-millis}")
//    private int redisMaxWaitMillis;
//    /*****redis config end*******/
//
//    /*****curator config start*******/
//    @Value("${rabbitmq.host}")
//    private String rmqHost;
//    @Value("${rabbitmq.port}")
//    private String rmqPort;
//    @Value("${rabbitmq.username}")
//    private String rmqUsername;
//    @Value("${rabbitmq.password}")
//    private String rmqPassword;
//    @Value("${rabbitmq.VirtualHost}")
//    private String rmqVirtualHost;
//    /*****curator config end*******/
}
