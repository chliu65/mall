package com.lc.mallshipping.controller;


import com.lc.mallshipping.common.exception.GlobalException;
import com.lc.mallshipping.common.keys.UserKey;
import com.lc.mallshipping.common.resp.ResponseEnum;
import com.lc.mallshipping.common.utils.CookieUtil;
import com.lc.mallshipping.common.utils.JsonUtil;
import com.lc.mallshipping.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.servlet.http.HttpServletRequest;


@Slf4j
public class BaseController {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    User getCurrentUser(HttpServletRequest httpServletRequest){
        String loginToken = CookieUtil.readLoginToken(httpServletRequest);
        if(StringUtils.isBlank(loginToken)){
            throw new GlobalException("用户未登陆，无法获取当前用户信息");
        }
        UserKey userKey=new UserKey(loginToken);
        String userJsonStr = stringRedisTemplate.opsForValue().get(userKey.getPrefix());
        if(StringUtils.isBlank(userJsonStr)){
            throw new GlobalException(ResponseEnum.LOGIN_EXPIRED);
        }
        User user = JsonUtil.Str2Obj(userJsonStr,User.class);
        return user;
    }
}
