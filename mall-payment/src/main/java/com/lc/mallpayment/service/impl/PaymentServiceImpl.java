package com.lc.mallpayment.service.impl;

import com.github.wxpay.sdk.WXPayUtil;
import com.lc.mallpayment.common.constants.Constants;
import com.lc.mallpayment.common.resp.ResponseEnum;
import com.lc.mallpayment.common.resp.ServerResponse;
import com.lc.mallpayment.common.utils.HttpClientUtil;
import com.lc.mallpayment.config.Parameters;
import com.lc.mallpayment.service.PaymentService;
import org.omg.CORBA.PRIVATE_MEMBER;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import sun.net.www.http.HttpClient;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
@Service
public class PaymentServiceImpl implements PaymentService {
   @Autowired
    private Parameters parameters;


    @Override
    public ServerResponse prePayment(Map<String, String> map) {
        try {
            Map<String,String> paramMap=new HashMap<>();
            paramMap.put("appid", parameters.getAppid());
            paramMap.put("mch_id", parameters.getPartner());
            paramMap.put("nonce_str", WXPayUtil.generateNonceStr());
            paramMap.put("body", "mall");
            //订单号
            paramMap.put("out_trade_no", map.get("orderNo"));
            paramMap.put("total_fee", map.get("totalfee"));
            paramMap.put("spbill_create_ip", map.get("127.0.0.1"));
            paramMap.put("notifyurl",parameters.getNotifyurl() );
            paramMap.put("trade_type", map.get("NATIVE"));
            //添加签名
            String xmlParam=WXPayUtil.generateSignedXml(paramMap, parameters.getPartnerkey());
            //微信支付URL地址
            String url= Constants.PaymentURL.WECHAT_PAYMENT_CREATEORDER_URL;
            HttpClientUtil httpClient=new HttpClientUtil(url);
            httpClient.setHttps(true);
            //提交方式
            httpClient.setXmlParam(xmlParam);
            httpClient.post();
            //获取返回结果
            String result=httpClient.getContent();
            Map<String,String> resultMap=WXPayUtil.xmlToMap(result);
            return ServerResponse.createBySuccess(resultMap);
        }catch (Exception e){
            e.printStackTrace();
        }
        return ServerResponse.createByError(ResponseEnum.ACQUIRE_NATIVE_ERROR);
    }

    @Override
    public ServerResponse paymentStatusQuery(String outTradeNo) {
        try {
            Map<String,String> paramMap=new HashMap<>();
            paramMap.put("appid", parameters.getAppid());
            paramMap.put("mch_id", parameters.getPartner());
            paramMap.put("nonce_str", WXPayUtil.generateNonceStr());
            //订单号
            paramMap.put("out_trade_no", outTradeNo);
            //添加签名
            String xmlParam=WXPayUtil.generateSignedXml(paramMap, parameters.getPartnerkey());
            //微信支付URL地址
            String url= Constants.PaymentURL.WECHAT_PAYMENT_QUERYORDER_URL;
            HttpClientUtil httpClient=new HttpClientUtil(url);
            httpClient.setHttps(true);
            //提交方式
            httpClient.setXmlParam(xmlParam);
            httpClient.post();
            //获取返回结果
            String result=httpClient.getContent();
            Map<String,String> resultMap=WXPayUtil.xmlToMap(result);
            return ServerResponse.createBySuccess(resultMap);
        }catch (Exception e){
            e.printStackTrace();
        }
        return ServerResponse.createByError(ResponseEnum.ACQUIRE_NATIVE_ERROR);
    }

    @Override
    public ServerResponse paymentResultCallback() {
        return null;
    }

    @Override
    public ServerResponse paymentOrderClose(Long orderNo) {
        try {
            Map<String,String> paramMap=new HashMap<>();
            paramMap.put("appid", parameters.getAppid());
            paramMap.put("mch_id", parameters.getPartner());
            paramMap.put("nonce_str", WXPayUtil.generateNonceStr());
            //订单号
            paramMap.put("out_trade_no", String.valueOf(orderNo));
            //添加签名
            String xmlParam=WXPayUtil.generateSignedXml(paramMap, parameters.getPartnerkey());
            //微信支付URL地址
            String url= Constants.PaymentURL.WECHAT_PAYMENT_CLOSEORDER_URL;
            HttpClientUtil httpClient=new HttpClientUtil(url);
            httpClient.setHttps(true);
            //提交方式
            httpClient.setXmlParam(xmlParam);
            httpClient.post();
            //获取返回结果
            String result=httpClient.getContent();
            Map<String,String> resultMap=WXPayUtil.xmlToMap(result);
            return ServerResponse.createBySuccess(resultMap);
        }catch (Exception e){
            e.printStackTrace();
        }
        return ServerResponse.createByErrorMessage("订单关闭请求异常");
    }
}
