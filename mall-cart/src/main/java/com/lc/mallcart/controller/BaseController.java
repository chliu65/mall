package com.lc.mallcart.controller;


import com.lc.mallcart.common.exception.GlobalException;
import com.lc.mallcart.common.keys.UserKey;
import com.lc.mallcart.common.resp.ResponseEnum;
import com.lc.mallcart.common.utils.CookieUtil;
import com.lc.mallcart.common.utils.JsonUtil;
import com.lc.mallcart.entity.User;
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
        if(StringUtils.isEmpty(loginToken)){
            throw new GlobalException(ResponseEnum.LOGIN_EXPIRED);
        }
        UserKey userKey=new UserKey(loginToken);
        String userJsonStr = stringRedisTemplate.opsForValue().get(userKey.getPrefix());
        if(userJsonStr.isEmpty()){
            throw new GlobalException(ResponseEnum.LOGIN_EXPIRED);
        }
        User user = JsonUtil.Str2Obj(userJsonStr,User.class);
        return user;
    }
}
