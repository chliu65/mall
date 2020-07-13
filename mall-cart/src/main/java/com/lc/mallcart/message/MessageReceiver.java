package com.lc.mallcart.message;

import com.lc.mallcart.service.ICartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
@Slf4j
public class MessageReceiver {
    @Autowired
    private ICartService cartService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue("cart-queue"),
            exchange = @Exchange("cart-exchange")
    ))
    public void proess(String message){
        log.info("接收到的消息为:{}",message);
        Integer userId = Integer.parseInt(message);
        log.info("【MQ解析数据,前者为userId,后者为productId：{}】",userId);
        //清除购物车
        cartService.removeCart(userId);
    }

}
