package com.lc.mallpayment.controller;

import com.github.wxpay.sdk.WXPayUtil;
import com.lc.mallpayment.common.resp.ResponseEnum;
import com.lc.mallpayment.common.resp.ServerResponse;
import com.lc.mallpayment.service.PaymentService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.util.Map;

@RequestMapping("/payment")
@RestController
public class PaymentController {
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @PostMapping("/pre_payment")
    public ServerResponse prePayment(@RequestParam Map<String,String> parameterMap){
        return paymentService.prePayment(parameterMap);
    }

    @PostMapping("/query_payment")
    public ServerResponse queryPayment(@RequestParam ("orderNo") String outTradeNo){
        return paymentService.paymentStatusQuery(outTradeNo);
    }

    @PostMapping("/payment_callback")
    public String paymentCallback(HttpServletRequest request) throws Exception{
        //获取网络输入流
        ServletInputStream servletInputStream=request.getInputStream();
        //创建输出流到输入文件中
        ByteArrayOutputStream byteArrayOutputStreams=new ByteArrayOutputStream();
        byte[] buffer=new byte[1024];
        int len=0;
        while ((len=servletInputStream.read(buffer))!=-1){
            byteArrayOutputStreams.write(buffer,0 ,len );
        }
        byte [] bytes=byteArrayOutputStreams.toByteArray();
        String xmlresult=new String(bytes,"UTF-8");
        System.out.println(xmlresult);
        //XML字符串转map
        Map<String,String> resultMap= WXPayUtil.xmlToMap(xmlresult);
        rabbitTemplate.convertAndSend("order", "paymentstatus.to.order", resultMap);
        String returnMsg="<xml><return_code><![CDATA[SUCCESS]]></return_code><return_msg><![CDATA[OK]]></return_msg> </xml>";
        return returnMsg;
    }

    @PostMapping("/payment_close")
    public ServerResponse paymentCloseOrder(@RequestParam("orderNo") Long orderNo){
        ServerResponse serverResponse=paymentService.paymentOrderClose(orderNo);
        if (serverResponse.isSuccess()){
            Map<String,String> resultMap=(Map)serverResponse.getData();
            if (resultMap.get("return_code").equals("SUCCESS")){
                String result_code=resultMap.get("result_code");
                if (result_code.equals("SUCCESS")){
                    return ServerResponse.createBySuccess();
                }else {
                    String err_code=resultMap.get("err_code");
                    switch (err_code){
                        case "ORDERPAID":return ServerResponse.createByError(ResponseEnum.PAYMENT_FINISHED);
                        case  "ORDERCLOSED":return ServerResponse.createBySuccess("订单已关闭，无需重复关闭");
                        default: return ServerResponse.createByError(ResponseEnum.SERVER_ERROR);
                    }
                }
            }else {
                return ServerResponse.createByErrorMessage(resultMap.get("return_msg"));
            }
        }
        return ServerResponse.createByError(ResponseEnum.SERVER_ERROR);
    }
}
