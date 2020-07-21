package com.lc.mallcart.message;

import com.lc.mallcart.common.constants.Constants;
import com.lc.mallcart.common.exception.GlobalException;
import com.lc.mallcart.common.resp.ServerResponse;
import com.lc.mallcart.common.utils.JsonUtil;
import com.lc.mallcart.dao.CartMapper;
import com.lc.mallcart.entity.Cart;
import com.lc.mallcart.service.CartService;
import com.lc.mallcart.vo.CartUserProductVo;
import com.lc.mallcart.vo.MessageVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;


@Component
@Slf4j
public class MessageReceiver {
    @Autowired
    private CartService cartService;

    /**
     * 针对数据库某一用户所有商品的增删改
     * @param message
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue("cart-queue-user"),
            exchange = @Exchange(Constants.CART_EXCHANGE)
    ))
    public void removeCart(String message){
        log.info("接收到的消息为:{}",message);
        MessageVo messageVo=(MessageVo) JsonUtil.Str2Obj(message,MessageVo.class );
        switch (messageVo.getOrder()){
            case "REMOVE":cartService.removeCartDB((String) messageVo.getData());
            break;
            default:
                throw new GlobalException("cart-queue-product监听消息指令异常");
        }
    }

    /**
     * 针对数据库单条数据的增删改
     * @param message
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue("cart-queue-product"),
            exchange = @Exchange(Constants.CART_EXCHANGE)
    ))
    private void updateCart(String message){
        log.info("接收到的消息为:{}",message);
        MessageVo messageVo=(MessageVo) JsonUtil.Str2Obj(message, MessageVo.class );
        ServerResponse serverResponse;
        switch (messageVo.getOrder()){
            case "UPDATE":
                serverResponse=cartService.updateCartDB((Cart) messageVo.getData());
                break;
            case "DELETE":
                CartUserProductVo cartUserProductVo=(CartUserProductVo) messageVo.getData();
                serverResponse=cartService.deleteCartDB(cartUserProductVo.getUserId(), cartUserProductVo.getProductId());
                break;
            default:
                throw new GlobalException("cart-queue-product监听消息指令异常");
        }
    }


}
