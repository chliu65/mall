package com.lc.mallorder.clients;

import com.lc.mallorder.common.resp.ServerResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient("payment")
public interface PaymentClient {
    @PostMapping("/payment/payment_close")
    ServerResponse paymentCloseOrder(@RequestParam("orderNo") Long orderNo);

}
