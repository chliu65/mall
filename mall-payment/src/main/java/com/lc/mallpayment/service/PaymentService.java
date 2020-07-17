package com.lc.mallpayment.service;

import com.lc.mallpayment.common.resp.ServerResponse;

import java.util.Map;

public interface PaymentService {
    /**
     * 根据订单号获取微信支付二维码
     * @param map
     * @return
     */
    ServerResponse prePayment(Map<String,String> map);

    /**
     * 支付状态查询
     * @param outTradeNo
     * @return
     */
    ServerResponse paymentStatusQuery(String outTradeNo);

    /**
     * 支付结果回调通知处理
     * @return
     */
    ServerResponse paymentResultCallback();

    /**
     * 发起关闭支付订单请求并返回请求结果
     * @param orderNo
     * @return
     */
    ServerResponse paymentOrderClose(Long orderNo);
}
