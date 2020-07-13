package com.lc.mallshipping.controller;


import com.github.pagehelper.PageInfo;
import com.lc.mallshipping.common.resp.ResponseEnum;
import com.lc.mallshipping.common.resp.ServerResponse;
import com.lc.mallshipping.common.utils.JsonUtil;
import com.lc.mallshipping.entity.Shipping;
import com.lc.mallshipping.entity.User;
import com.lc.mallshipping.service.IShippingService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;


@RestController
@RequestMapping("/shipping/")
@Slf4j
public class ShippingController extends BaseController{

    @Autowired
    private IShippingService shippingService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    /**
     * 添加地址
     */
    @RequestMapping("add.do")
    public ServerResponse add(HttpServletRequest httpServletRequest, Shipping shipping){
        User user = getCurrentUser(httpServletRequest);
        return shippingService.add(user.getId(),shipping);
    }

    /**
     * 删除地址
     */
    @RequestMapping("del.do")
    public ServerResponse del(HttpServletRequest httpServletRequest, Integer shippingId){
        User user = getCurrentUser(httpServletRequest);
        return shippingService.del(user.getId(),shippingId);
    }

    /**
     * 更新地址
     */
    @RequestMapping("update.do")
    public ServerResponse update(HttpServletRequest httpServletRequest, Shipping shipping){
        User user = getCurrentUser(httpServletRequest);
        return shippingService.update(user.getId(),shipping);
    }

    /**
     * 选择选中的地址
     */
    @RequestMapping("select.do")
    public ServerResponse select(HttpServletRequest httpServletRequest, Integer shippingId){
        User user = getCurrentUser(httpServletRequest);
        return shippingService.select(user.getId(),shippingId);
    }

    /**
     * 地址列表
     */
    @RequestMapping("list.do")
    public ServerResponse<PageInfo> list(HttpServletRequest httpServletRequest,
                                         @RequestParam(value = "pageNum",defaultValue = "1")int pageNum,
                                         @RequestParam(value = "pageSize",defaultValue = "10")int pageSize){
        User user = getCurrentUser(httpServletRequest);
        return shippingService.list(user.getId(),pageNum,pageSize);
    }

    /**
     * 根据id获取地址
     */
    @RequestMapping("getShipping.do")
    ServerResponse getShipping(HttpServletRequest httpServletRequest,Integer shippingId){
        log.info("【开始根据{}获取地址】",shippingId);
        User user = null;
        Enumeration<String> headerNames = httpServletRequest.getHeaderNames();
        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                String name = headerNames.nextElement();
                if(name.equalsIgnoreCase("snailmall_login_token")){
                    String value = httpServletRequest.getHeader(name);
                    if(StringUtils.isBlank(value)){
                        log.error("【获取用户cookie失败】");
                        return ServerResponse.createByError(ResponseEnum.LOGIN_EXPIRED);
                    }
                    String userJsonStr = stringRedisTemplate.opsForValue().get(value);
                    if(StringUtils.isBlank(userJsonStr)){
                        log.error("【获取用户信息失败】");
                        return ServerResponse.createByError(ResponseEnum.LOGIN_EXPIRED);
                    }
                    user = JsonUtil.Str2Obj(userJsonStr,User.class);
                    log.info("【获取到的用户为：{}】",user);
                }
            }
        }

        return shippingService.getShippingById(user.getId(),shippingId);

    }



}
