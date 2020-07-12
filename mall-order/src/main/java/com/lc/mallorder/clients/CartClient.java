package com.lc.mallorder.clients;


import com.lc.mallorder.common.resp.ServerResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;


@FeignClient("cart-service")
public interface CartClient {
    @RequestMapping("/cart/getCartList.do")
    ServerResponse getCartList(String userId);

    @RequestMapping("/cart/removeCart.do")
    ServerResponse removeCart(String userId);

}
