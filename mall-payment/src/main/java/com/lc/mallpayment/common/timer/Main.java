package com.lc.mallpayment.common.timer;


import com.lc.mallpayment.common.constants.Constants;
import com.lc.mallpayment.common.utils.RedisUtils;

public class Main {

    public static void main(String[] args) {
//        Timer timer = new Timer(1000, 60);
//        RedisUtils redisUtils=new RedisUtils();
//        OrderDelayHandlerTask orderDelayHandlerTask=new OrderDelayHandlerTask(1283235487804297L,redisUtils);
//        TimerTask timerTask=new TimerTask(Constants.ORDER_LOCKERD_TIME,orderDelayHandlerTask);
//        timer.addTask(timerTask);
        //System.out.println("stop\t" + System.currentTimeMillis());

        try {
            int i=1/0;
        }catch (Exception e){
            e.printStackTrace();
        }
        System.out.println(1);
    }
}
