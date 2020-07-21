package com.lc.mallcart.controller;


import com.lc.mallcart.common.constants.Constants;
import com.lc.mallcart.common.resp.ServerResponse;
import com.lc.mallcart.entity.User;
import com.lc.mallcart.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * 购物车服务
 */
@RestController
@RequestMapping("/cart/")
public class CartController extends BaseController{
    @Autowired
    private CartService cartService;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 1.购物车添加商品
     */
    @RequestMapping("add.do")
    public ServerResponse add(HttpServletRequest httpServletRequest, Integer productId, Integer count){
        User user = getCurrentUser(httpServletRequest);

        return cartService.add(user.getId(),productId,count);
    }


    /**
     * 2.移除购物车某个产品
     */
    @RequestMapping("delete_product.do")
    public ServerResponse delete_product(HttpServletRequest httpServletRequest,String productIds){
        User user = getCurrentUser(httpServletRequest);

        return cartService.delete(user.getId(),productIds);
    }

    /**
     * 3.购物车List列表
     * 查询操作，不需要异步处理，查询后放入redis中
     * 价格的单位是元,保留小数后2位
     */
    @RequestMapping("list.do")
    public ServerResponse list(HttpServletRequest httpServletRequest){
        User user = getCurrentUser(httpServletRequest);
        return cartService.list(user.getId());
    }


//    /**
//     * 5.购物车全选
//     */
//    @RequestMapping("select_all.do")
//    public ServerResponse select_all(HttpServletRequest httpServletRequest){
//        User user = getCurrentUser(httpServletRequest);
//
//        return cartService.selectOrUnSelect(user.getId(), Constants.Cart.CHECKED,null);
//    }
//
//    /**
//     * 6.购物车全不选
//     */
//    @RequestMapping("un_select_all.do")
//    public ServerResponse un_select_all(HttpServletRequest httpServletRequest){
//        User user = getCurrentUser(httpServletRequest);
//
//        return cartService.selectOrUnSelect(user.getId(),Constants.Cart.UN_CHECKED,null);
//    }

    /**
     * 7.购物车选中某个商品
     */
    @RequestMapping("select.do")
    public ServerResponse select(HttpServletRequest httpServletRequest,Integer productId){
        User user = getCurrentUser(httpServletRequest);
        return cartService.selectOrUnSelect(user.getId(),Constants.Cart.CHECKED,productId);
    }

    /**
     * 8.购物车取消选中某个商品
     */
    @RequestMapping("un_select.do")
    public ServerResponse un_select(HttpServletRequest httpServletRequest,Integer productId){
        User user = getCurrentUser(httpServletRequest);

        return cartService.selectOrUnSelect(user.getId(),Constants.Cart.UN_CHECKED,productId);
    }


    /**
     * 9.查询在购物车里的产品种类数
     */
    @RequestMapping("get_cart_product_count.do")
    public ServerResponse<Integer> get_cart_product_count(HttpServletRequest httpServletRequest){
        User user = getCurrentUser(httpServletRequest);
        return cartService.get_cart_product_count(user.getId());
    }


    /**
     * 提供的feign接口，根据userId获取购物车列表()
     */
    @RequestMapping("getCartList.do")
    public ServerResponse getCartList(@RequestParam("userId") String userId){
        return cartService.list(Integer.valueOf(userId));
    }

    /**
     * 提供的feign接口，清空购物车
     */
    @RequestMapping("removeCart.do")
    public ServerResponse removeCart(@RequestParam("userId") String userId){
        return cartService.removeCart(userId);
    }



}
