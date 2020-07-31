package com.lc.mallorder.message;


import com.fasterxml.jackson.core.type.TypeReference;
import com.lc.mallorder.common.constants.Constants;
import com.lc.mallorder.common.timer.Timer;
import com.lc.mallorder.common.utils.JsonUtil;
import com.lc.mallorder.common.utils.RedisUtils;
import com.lc.mallorder.service.OrderService;
import com.lc.mallorder.vo.MessageVo;
import com.lc.mallorder.vo.OrderUserCartVo;
import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;


@Component
@Slf4j
public class MessageReceiver {
    @Autowired
    private OrderService orderService;
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue("order-queue"),
            exchange = @Exchange("order-exchange"),
            key = "order.*"
    ))
    public void proess(String message){
        log.info("接收到的消息为:{}",message);
        MessageVo messageVo = JsonUtil.Str2Obj(message, new TypeReference<MessageVo<Object>>(){} );
        log.info("【MQ解析数据,前者为userId,后者为product信息：{}】",messageVo);
        String orderUserCartVoStr=JsonUtil.obj2String(messageVo.getData());
        OrderUserCartVo orderUserCartVo=JsonUtil.Str2Obj(orderUserCartVoStr,OrderUserCartVo.class );
        //数据库购物车选中项清空，生成未支付订单存入数据库
        orderService.stockAndOrderprocess(orderUserCartVo);
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue("order-queue"),
            exchange = @Exchange("order-exchange"),
            key = "order.*"
    ))
    public void removeCart(@Payload String message){
        log.info("接收到的消息为:{}",message);
        MessageVo messageVo=(MessageVo) JsonUtil.Str2Obj(message,new TypeReference<MessageVo<Object>>() {} );
    }

}
