package com.lc.mallorder.message;

import com.lc.mallorder.clients.PaymentClient;
import com.lc.mallorder.common.constants.Constants;
import com.lc.mallorder.common.resp.ResponseEnum;
import com.lc.mallorder.common.resp.ServerResponse;
import com.lc.mallorder.common.timer.OrderDelayHandlerTask;
import com.lc.mallorder.common.timer.Timer;
import com.lc.mallorder.common.timer.TimerTask;
import com.lc.mallorder.common.utils.JsonUtil;
import com.lc.mallorder.common.utils.RedisUtils;
import com.lc.mallorder.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;


@Slf4j
@Component
@RabbitListener(queues = "paymentstatus")
public class PaymentMsgListener {
    @Autowired
    private PaymentClient paymentClient;
    @Autowired
    private OrderService orderService;
    @Autowired
    private Timer timer;
    @Autowired
    private RedisUtils redisUtils;
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
            //支付成功，扣库存，改变订单状态
            //支付失败，关闭支付交易，开启延迟任务
            if (resultCode.equals("SUCCESS")){
                orderService.finishOrder(orderNO);
            }else {
                ServerResponse serverResponse= paymentClient.paymentCloseOrder(Long.valueOf(orderNO));
                //支付订单关闭失败
                if (!serverResponse.isSuccess()){
                    //判断是否失败原因是否为已支付
                    int code=serverResponse.getCode();
                    if (code== ResponseEnum.PAYMENT_FINISHED.getCode()){
                        orderService.finishOrder(orderNO);
                        return;
                    }
                }
                //其他情况，开启定时任务
                OrderDelayHandlerTask orderDelayHandlerTask=new OrderDelayHandlerTask(Long.valueOf(orderNO),redisUtils);
                TimerTask timerTask=new TimerTask(Constants.ORDER_LOCKERD_TIME,orderDelayHandlerTask);
                timer.addTask(timerTask);
            }
        }
    }
}
