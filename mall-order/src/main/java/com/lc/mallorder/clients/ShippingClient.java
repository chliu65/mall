package com.lc.mallorder.clients;


import com.lc.mallorder.common.resp.ServerResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient("shipping-service")
public interface ShippingClient {

    @RequestMapping("/shipping/getShipping.do")
    ServerResponse getShipping(@RequestParam("shippingId") Integer shippingId);

}
