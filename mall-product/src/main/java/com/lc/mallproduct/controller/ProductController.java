package com.lc.mallproduct.controller;


import com.lc.mallproduct.common.resp.ServerResponse;
import com.lc.mallproduct.service.ProductService;
import com.lc.mallproduct.vo.StockReduceVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequestMapping("/product/")
@Slf4j
public class ProductController implements InitializingBean {
    @Autowired
    private ProductService productService;

    /**
     * 查询商品全部信息(访问量小，不会缓存到redis)
     * @param productId
     * @return
     */
    @RequestMapping("detail.do")
    public ServerResponse detail(@RequestParam(value = "productId",required = true) Integer productId){
        return productService.getPortalProductDetail(productId);
    }

    //搜索罗列商品
    @RequestMapping("list.do")
    public ServerResponse list(@RequestParam(value = "keyword",required = false)String keyword,
                                         @RequestParam(value = "categoryId",required = false)Integer categoryId,
                                         @RequestParam(value = "pageNum",defaultValue = "1")int pageNum,
                                         @RequestParam(value = "pageSize",defaultValue = "10")int pageSize,
                                         @RequestParam(value = "orderBy",defaultValue = "")String orderBy){
        return productService.portalList(keyword,categoryId,orderBy,pageNum,pageSize);
    }

    /**
     * 到数据库查询商品静态信息，查询后缓存到redis中
     * @param productId
     * @return
     */
    @RequestMapping("queryProduct.do")
    public ServerResponse queryProduct(@RequestParam("productId") Integer productId){
        return productService.queryProduct(productId);
    }

    /**
     * 用于支付后扣减库存
     * @param stockReduceVoList
     * @return
     */
    @RequestMapping("reduceStock.do")
    public ServerResponse reduceStock(@RequestBody List<StockReduceVo> stockReduceVoList){
        log.info(stockReduceVoList.toString() );
        return productService.reduceStock(stockReduceVoList);
    }

    //初始化预存商品静态信息和商品库存到redis
    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("初始化商品信息");
        productService.preInitProductInfoAndStcokToRedis();
    }
}
