package com.lc.mallcart.message;

import com.fasterxml.jackson.core.type.TypeReference;
import com.lc.mallcart.common.constants.Constants;
import com.lc.mallcart.common.exception.GlobalException;
import com.lc.mallcart.common.resp.ServerResponse;
import com.lc.mallcart.common.utils.JsonUtil;
import com.lc.mallcart.dao.CartMapper;
import com.lc.mallcart.entity.Cart;
import com.lc.mallcart.service.CartService;
import com.lc.mallcart.vo.CartUserProductVo;
import com.lc.mallcart.vo.MessageVo;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Date;


@Component
@Slf4j
public class CartItemMsgReceiver {
    @Autowired
    private CartService cartService;
    /**
     * 针对cart数据库某一用户所有商品的增删改
     * @param message
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue("cartitems-queue"),
            exchange = @Exchange(Constants.CART_EXCHANGE)
    ))
    public void removeCart(@Payload String message){
        log.info("接收到的消息为:{}",message);
        MessageVo messageVo=(MessageVo) JsonUtil.Str2Obj(message,new TypeReference<MessageVo<Object>>() {} );
        switch (messageVo.getOrder()){
            case "REMOVE":
                String userIdstr=JsonUtil.obj2String(messageVo.getData());
                cartService.removeCartDB(userIdstr);
            break;
            default:
                throw new GlobalException("cart-queue-product监听消息指令异常");
        }
    }

}
