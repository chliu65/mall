package com.lc.mallpayment.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Data
public class Parameters {
    @Value("${wechat.appid}")
    private String appid;

    @Value("${wechat.partner}")
    private String partner;

    @Value("${wechat.partnerkey}")
    private String partnerkey;

    @Value("${wechat.notifyurl}")
    private String notifyurl;

//    @Value("${rabbitmq.queue}")
//    private String queue;
//    @Value("${rabbitmq.exchange}")
//    private String exchange;
//    @Value("${rabbitmq.routingkey}")
//    private String routingkey;



}
