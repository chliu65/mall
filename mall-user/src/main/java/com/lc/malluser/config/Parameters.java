package com.lc.malluser.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


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

    /*****curator config start*******/
    @Value("${zk.host}")
    private String zkHost;
    /*****curator config end*******/
}
