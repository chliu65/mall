package com.lc.mallorder.clients;


import com.lc.mallorder.common.resp.ServerResponse;
import com.lc.mallorder.vo.StockReduceVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;


@FeignClient("mall-product")
public interface ProductClient {
    @RequestMapping("/product/queryProduct.do")
    ServerResponse queryProduct(@RequestParam("productId") Integer productId);
    @RequestMapping("/product/reduceStock.do")
    ServerResponse reduceStock(@RequestBody List<StockReduceVo> stockReduceVoList);
}