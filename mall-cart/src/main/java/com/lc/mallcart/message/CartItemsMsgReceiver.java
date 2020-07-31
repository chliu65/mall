package com.lc.mallcart.message;

import com.fasterxml.jackson.core.type.TypeReference;
import com.lc.mallcart.common.constants.Constants;
import com.lc.mallcart.common.exception.GlobalException;
import com.lc.mallcart.common.resp.ServerResponse;
import com.lc.mallcart.common.utils.JsonUtil;
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
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;


@Component
@Slf4j
public class CartItemsMsgReceiver {
    @Autowired
    private CartService cartService;
    /**
     * 针对cart数据库单条数据的增删改
     * @param message
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue("cartitem-queue"),
            exchange = @Exchange(Constants.CART_EXCHANGE)
    ))
    private void updateCart(String message){
        log.info("接收到的消息为:{}",message);
        //复杂对象反序列化失败
        MessageVo messageVo=(MessageVo) JsonUtil.Str2Obj(message, new TypeReference<MessageVo<Object>>() {});
        ServerResponse serverResponse;
        switch (messageVo.getOrder()){
            //更新指令中携带的数据是cart
            case "UPDATE":
                String cartStr=JsonUtil.obj2String(messageVo.getData());
                Cart cart=JsonUtil.Str2Obj(cartStr,Cart.class );
                serverResponse=cartService.updateCartDB(cart);
                break;
            //删除指令中携带的是userID和productID
            case "DELETE":
                String cartUserProductVoStr=JsonUtil.obj2String(messageVo.getData());
                CartUserProductVo cartUserProductVo=JsonUtil.Str2Obj(cartUserProductVoStr,CartUserProductVo.class );
                serverResponse=cartService.deleteCartDB(cartUserProductVo.getUserId(), cartUserProductVo.getProductId());
                break;
            default:
                throw new GlobalException("cart-queue-product监听消息指令异常");
        }
    }


}
