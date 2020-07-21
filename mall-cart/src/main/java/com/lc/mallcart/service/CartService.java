package com.lc.mallcart.service;


import com.lc.mallcart.common.resp.ServerResponse;
import com.lc.mallcart.entity.Cart;

public interface CartService {

    /**  购物车更新商品 **/
    ServerResponse add(Integer userId, Integer productId, Integer count);

//    /**  更新购物车某个产品数量  **/
//    ServerResponse update(Integer userId, Integer productId, Integer count);

    /**  移除购物车某个产品 **/
    ServerResponse delete(Integer userId, String productIds);

    /**  购物车List列表 **/
    ServerResponse list(Integer userId);

    /**  购物车选中/取消某个商品 **/
    ServerResponse selectOrUnSelect(Integer userId, int checked, Integer productId);

    /**  查询在购物车里的产品数量 **/
    ServerResponse<Integer> get_cart_product_count(Integer userId);

    /** 清空购物车 **/
    ServerResponse removeCart(String userId);

    /*更新cart数据库记录*/
    ServerResponse updateCartDB(Cart cart);

    /*清空用户cart数据库记录*/
    ServerResponse removeCartDB(String userId);

    /*删除cart数据库记录*/
    ServerResponse deleteCartDB(Integer userId, Integer productId);
}
