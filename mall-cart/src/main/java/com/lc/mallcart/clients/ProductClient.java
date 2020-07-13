package com.lc.mallcart.clients;


import com.lc.mallcart.common.resp.ServerResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;


@FeignClient("mall-product")
public interface ProductClient {

    @RequestMapping("/product/detail.do")
    ServerResponse getProductDetail(@RequestParam("productId") Integer productId);


    @RequestMapping("/product/queryProduct.do")
    ServerResponse queryProduct(@RequestParam("productId") Integer productId);
}
