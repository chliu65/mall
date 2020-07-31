package com.lc.mallorder.common.timer;

import com.lc.mallorder.common.constants.Constants;
import com.lc.mallorder.common.exception.GlobalException;
import com.lc.mallorder.common.keys.OrderKey;
import com.lc.mallorder.common.keys.ProductStockKey;
import com.lc.mallorder.common.utils.JsonUtil;
import com.lc.mallorder.common.utils.RedisUtils;
import com.lc.mallorder.vo.StockReduceVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 订单延迟处理任务
 */
@Slf4j
public class OrderDelayHandlerTask implements Runnable {

    private List<StockReduceVo> stockReduceVoList;

    private String orderNo;

    private RedisUtils redisUtils;


    public OrderDelayHandlerTask(List<StockReduceVo> stockReduceVoList, String orderNo,RedisUtils redisUtils) {
        this.stockReduceVoList = stockReduceVoList;
        this.redisUtils = redisUtils;
        this.orderNo=orderNo;
    }

    @Override
    public void run() {
        OrderKey orderKey=new OrderKey(OrderKey.ORDER_STATUS,orderNo);
        Object orderStatus=redisUtils.getValue(orderKey.getPrefix());
        if (orderKey!=null){
            int orderStatusCode=Integer.valueOf(orderStatus.toString());
            if (orderStatusCode==Constants.OrderStatusEnum.NO_PAY.getCode()){
                //未支付,订单取消
                for (StockReduceVo stockReduceVo:stockReduceVoList){
                    ProductStockKey productStockKey=new ProductStockKey(stockReduceVo.getProductId());
                    Long result=(Long) redisUtils.changeStock(productStockKey.getPrefix(),"-"+stockReduceVo.getReduceNum() );
                    if (result==-2){
                        log.info("商品{}回滚失败", stockReduceVo.getProductId());
                    }else {
                        log.info("商品{}回滚数量{}", stockReduceVo.getProductId(),stockReduceVo.getReduceNum());
                    }
                }
            }else if (orderStatusCode==Constants.OrderStatusEnum.PAYING.getCode()){
                //正在支付，延迟异步取消
                //加上到期标志，留给支付处理。
                redisUtils.putKey(orderKey,String.valueOf(0-orderStatusCode) );
            }else {
                //没有其他情况，（其他情况key已被删除）
            }
        }
    }
}
