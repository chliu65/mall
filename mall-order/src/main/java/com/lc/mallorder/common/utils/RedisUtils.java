package com.lc.mallorder.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;

import java.util.Collections;



@Component
@Slf4j
public class RedisUtils {
    @Autowired
    private RedisTemplate redisTemplate;


    /**
     * 先查询库存，够的话再减库存
     * -2：库存不存在
     * -1：库存不足
     * >=0：返回扣减后的库存数量，肯定大于等于0的
     */
    public static final String STOCK_REDUCE_LUA=
                    "local stock = KEYS[1] " +
                    "local stock_change = tonumber(ARGV[1]) " +
                    "local is_exists = redis.call(\"EXISTS\", stock) " +
                    "if stock_change == nil then "+
//                    "else "+
                    "return ARGV[1] "+
                    "end "+
                    "if is_exists == 1 then " +
                    "    local stockAftChange = tonumber(redis.call(\"GET\", stock)) - stock_change " +
                    "    if(stockAftChange<0) then " +
                    "        return -1 " +
                    "    else  " +
                    "        redis.call(\"SET\", stock,stockAftChange) " +
                    "        return stockAftChange " +
                    "    end " +
                    "else " +
                    "    return -2 " +
                    "end";


    /**
     *
     * @Description  改变库存
     */
    public Object changeStock(String stockKey,String stockChange){
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/changestock.lua")));
        redisScript.setResultType(Long.class);
        return redisTemplate.execute(redisScript, Collections.singletonList(stockKey),stockChange );

    }

}
