package com.lc.mallcart.controller;


import com.lc.mallcart.common.constants.Constants;
import com.lc.mallcart.common.keys.UserKey;
import com.lc.mallcart.common.resp.ResponseEnum;
import com.lc.mallcart.common.resp.ServerResponse;
import com.lc.mallcart.common.utils.CookieUtil;
import com.lc.mallcart.common.utils.JsonUtil;
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
public class CartController {
    @Autowired
    private CartService cartService;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    public User getUser(HttpServletRequest request){
        String token = CookieUtil.readLoginToken(request);
        UserKey userKey=new UserKey(token);
        String  userStr=stringRedisTemplate.opsForValue().get(userKey.getPrefix());
        return JsonUtil.Str2Obj(userStr,User.class );
    }

    /**
     * 1.购物车添加商品
     */
    @RequestMapping("add.do")
    public ServerResponse add(HttpServletRequest httpServletRequest, @RequestParam("productId") Integer productId, @RequestParam("count") Integer count){
        User user = getUser(httpServletRequest);
        if (productId==null || count==null || count.intValue()<=0){
            return ServerResponse.createByError(ResponseEnum.ILLEGAL_ARGUMENTS);
        }
        return cartService.add(user.getId(),productId,count);
    }


    /**
     * 2.移除购物车某个产品
     */
    @RequestMapping("delete_product.do")
    public ServerResponse delete_product(HttpServletRequest httpServletRequest,@RequestParam("productId") String productId){
        User user = getUser(httpServletRequest);

        return cartService.delete(user.getId(),productId);
    }

    /**
     * 3.购物车List列表
     * 查询操作，不需要异步处理，查询后放入redis中
     * 价格的单位是元,保留小数后2位
     */
    @RequestMapping("list.do")
    public ServerResponse list(HttpServletRequest httpServletRequest){
        User user = getUser(httpServletRequest);
        return cartService.list(user.getId());
    }


    /**
     * 4.购物车选中某个商品
     */
    @RequestMapping("select.do")
    public ServerResponse select(HttpServletRequest httpServletRequest,Integer productId){
        User user = getUser(httpServletRequest);
        return cartService.selectOrUnSelect(user.getId(),Constants.Cart.CHECKED,productId);
    }

    /**
     * 5.购物车取消选中某个商品
     */
    @RequestMapping("un_select.do")
    public ServerResponse un_select(HttpServletRequest httpServletRequest,Integer productId){
        User user = getUser(httpServletRequest);

        return cartService.selectOrUnSelect(user.getId(),Constants.Cart.UN_CHECKED,productId);
    }




    /**
     * 提供的feign接口，根据userId获取购物车列表()
     */
    @RequestMapping("getCartList.do")
    public ServerResponse getCartList(@RequestParam("userId") String userId){
        return cartService.getCartVo(Integer.valueOf(userId));
    }

    /**
     * 提供的feign接口，清空购物车
     */
    @RequestMapping("removeCart.do")
    public ServerResponse removeCart(@RequestParam("userId") String userId){
        return cartService.removeCart(userId);
    }

}
