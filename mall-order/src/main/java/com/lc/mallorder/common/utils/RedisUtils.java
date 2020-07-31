package com.lc.mallorder.common.utils;

import com.lc.mallorder.common.keys.BasePrefix;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;


@Component
@Slf4j
public class RedisUtils {
    @Autowired
    private RedisTemplate<String,String> redisTemplate;



    public Object getValue(String key){
        return redisTemplate.opsForValue().get(key);
    }
    public void putKey(BasePrefix basePrefix, String value){
        redisTemplate.opsForValue().set(basePrefix.getPrefix(),value,basePrefix.expireSeconds(), TimeUnit.SECONDS );
    }
    public void putKey(BasePrefix basePrefix, String value,int expireSecond){
        redisTemplate.opsForValue().set(basePrefix.getPrefix(),value,expireSecond, TimeUnit.SECONDS );
    }
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

//    public Object changeOrderStatus(String orderStatusKey,String newOrderStatus,String oldOrderStatus){
//        DefaultRedisScript<String> redisScript = new DefaultRedisScript<>();
//        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/batchreducestock.lua")));
//        redisScript.setResultType(String.class);
//        return redisTemplate.execute(redisScript, Collections.singletonList(orderStatusKey),newOrderStatus,oldOrderStatus );
//
//    }

    /**
     * 批量原子操作改库存
     * @param productKeyStringList
     * @param cartProductQuantityList
     * @return
     */
    public String batchChangeStock(List<String> productKeyStringList, List<String> cartProductQuantityList) {
        DefaultRedisScript<String> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/batchreducestock.lua")));
        redisScript.setResultType(String.class);
        return redisTemplate.execute(redisScript, productKeyStringList, cartProductQuantityList.toArray());
//        return "success";
    }
}
