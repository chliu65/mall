package com.lc.mallorder.clients;


import com.lc.mallorder.common.resp.ServerResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;


@FeignClient("mall-product")
public interface ProductClient {
    @RequestMapping("/product/queryProduct.do")
    ServerResponse queryProduct(@RequestParam("productId") Integer productId);
}