package com.lc.mallorder.message;

import com.lc.mallorder.clients.PaymentClient;
import com.lc.mallorder.common.constants.Constants;
import com.lc.mallorder.common.keys.CartKey;
import com.lc.mallorder.common.keys.OrderKey;
import com.lc.mallorder.common.keys.ProductKey;
import com.lc.mallorder.common.keys.ProductStockKey;
import com.lc.mallorder.common.resp.ResponseEnum;
import com.lc.mallorder.common.resp.ServerResponse;
import com.lc.mallorder.common.timer.OrderDelayHandlerTask;
import com.lc.mallorder.common.timer.Timer;
import com.lc.mallorder.common.timer.TimerTask;
import com.lc.mallorder.common.utils.JsonUtil;
import com.lc.mallorder.common.utils.RedisUtils;
import com.lc.mallorder.dao.OrderItemMapper;
import com.lc.mallorder.entity.OrderItem;
import com.lc.mallorder.service.OrderService;
import com.lc.mallorder.vo.StockReduceVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;


@Slf4j
@Component
public class PaymentMsgListener {
    @Autowired
    private PaymentClient paymentClient;
    @Autowired
    private OrderService orderService;
    @Autowired
    private Timer timer;
    @Autowired
    private RedisUtils redisUtils;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @RabbitListener(bindings = @QueueBinding(value = @Queue("order-queue"),
            exchange = @Exchange("order-exchange"),
    key = "order.*"))
    @RabbitHandler
    public void getMessage(String message){
        log.info("支付状态监听消息{}",message );
        //支付结果
        Map<String,String> resultMap= JsonUtil.Str2Obj(message,Map.class );
        //通信标志
        String returnCode=resultMap.get("return_code");
        if (returnCode.equals("SUCCESS")){
            //业务结果  resultcode
            String resultCode=resultMap.get("result_code");

            //订单号
            String orderNO=resultMap.get("out_trade_no");
            if (resultCode.equals("SUCCESS")){
                //支付成功，扣库存，改变订单状态
                orderService.finishOrder(orderNO);
            }else {
                //支付失败，关闭支付交易，开启延迟任务
                ServerResponse serverResponse= paymentClient.paymentCloseOrder(Long.valueOf(orderNO));
                //关闭订单失败
                if (!serverResponse.isSuccess()){
                    //判断失败原因是否为已支付
                    int code=serverResponse.getCode();
                    if (code== ResponseEnum.PAYMENT_FINISHED.getCode()){
                        orderService.finishOrder(orderNO);
                        return;
                    }else {
                        //订单关闭失败，且未支付
                    }
                }else {
                    //关闭订单成功
                }
                //是否为首次支付
                OrderKey orderKey=new OrderKey(OrderKey.ORDER_STATUS,orderNO);
                if (orderKey==null){
                    //首次支付未成功，开启定时任务
                    CartKey cartKey=new CartKey(orderNO);
                    List<String> productStockKeyStringList=new LinkedList<>();
                    List<String> cartProductQuantityList=new LinkedList<>();
                    Map<String,String> map=(Map) stringRedisTemplate.opsForHash().entries(cartKey.getPrefix());
                    List<StockReduceVo> stockReduceVoList=new LinkedList<>();
                    for (String productId:map.keySet()){
                        StockReduceVo stockReduceVo=new StockReduceVo(productId,map.get(productId));
                        stockReduceVoList.add(stockReduceVo);
                    }
                    OrderDelayHandlerTask orderDelayHandlerTask=new OrderDelayHandlerTask(stockReduceVoList,orderNO,redisUtils);
                    TimerTask timerTask=new TimerTask(Constants.ORDER_LOCKERD_TIME,orderDelayHandlerTask);
                    stringRedisTemplate.opsForValue().set(orderKey.getPrefix(),String.valueOf(Constants.OrderStatusEnum.NO_PAY.getCode()));
                    timer.addTask(timerTask);
                }else {
                    //非首次支付，订单是否超时
                    String orderStatusStr=stringRedisTemplate.opsForValue().get(orderKey.getPrefix());
                    int orderStatusCode=Integer.valueOf(orderStatusStr);
                    //订单状态支付中，说明未到期，修改为未支付状态
                    if (orderStatusCode==Constants.OrderStatusEnum.PAYING.getCode()){
                        stringRedisTemplate.opsForValue().set(orderKey.getPrefix(),String.valueOf(Constants.OrderStatusEnum.NO_PAY.getCode() ));
                    }else {
                        //订单在支付前刚好到期，删除并回滚redis
                        CartKey cartKey=new CartKey(orderNO);
                        List<String> productStockKeyStringList=new LinkedList<>();
                        List<String> cartProductQuantityList=new LinkedList<>();
                        Map<String,String> map=(Map) stringRedisTemplate.opsForHash().entries(cartKey.getPrefix());
                        for (String productId:map.keySet()){
                            ProductStockKey productStockKey=new ProductStockKey(productId);
                            productStockKeyStringList.add(productStockKey.getPrefix());
                            cartProductQuantityList.add("-"+map.get(productId));
                        }
                        stringRedisTemplate.delete(orderKey.getPrefix());
                        stringRedisTemplate.delete(cartKey.getPrefix());
                        redisUtils.batchChangeStock(productStockKeyStringList,cartProductQuantityList);
                    }
                }
            }
        }
    }
}
