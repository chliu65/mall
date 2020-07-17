package com.lc.mallorder.common.timer;

import com.lc.mallorder.common.constants.Constants;
import com.lc.mallorder.common.exception.GlobalException;
import com.lc.mallorder.common.keys.OrderKey;
import com.lc.mallorder.common.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
public class OrderDelayHandlerTask implements Runnable {

    private long orderNo;

    private RedisUtils redisUtils;


    public OrderDelayHandlerTask(long orderNo,RedisUtils redisUtils) {
        this.orderNo = orderNo;
        this.redisUtils=redisUtils;
    }

    @Override
    public void run() {
        OrderKey orderStatusKey=new OrderKey(Constants.OrderKeyPrefix.ORDER_STATUS_KEY, String.valueOf(orderNo));
        String newOrderStatus=String.valueOf( Constants.OrderStatusEnum.ORDER_CLOSE.getCode());
        String oldOrderStatus=String.valueOf(Constants.OrderStatusEnum.NO_PAY .getCode());
        String result=String.valueOf(redisUtils.changeOrderStatus(orderStatusKey.getPrefix(),newOrderStatus, oldOrderStatus));
        if (result.equals("null")){
            Thread.currentThread().interrupt();
            throw new GlobalException("redis中订单状态key过期");
        }
        if (result.equals(newOrderStatus)){
            log.info("订单{}过期，已关闭",orderNo);
        }else {
            log.info("订单{}已完成或取消:{}",orderNo,oldOrderStatus);
        }
    }
}
