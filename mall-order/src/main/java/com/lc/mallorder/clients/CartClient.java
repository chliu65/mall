package com.lc.mallorder.clients;


import com.lc.mallorder.common.resp.ServerResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;


@FeignClient("mall-cart")
public interface CartClient {
    @RequestMapping("/cart/getCartList.do")
    ServerResponse getCartList(@RequestParam("userId")String userId);

    @RequestMapping("/cart/removeCart.do")
    ServerResponse removeCart(@RequestParam("userId") String userId);

}
