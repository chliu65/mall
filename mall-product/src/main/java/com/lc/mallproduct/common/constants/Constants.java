package com.lc.mallproduct.common.constants;


import com.google.common.collect.Sets;

import java.util.Set;

public class Constants {
    /**自定义状态码 start**/
    public static final int RESP_STATUS_OK = 200;

    public static final int RESP_STATUS_NOAUTH = 401;

    public static final int RESP_STATUS_INTERNAL_ERROR = 500;

    public static final int RESP_STATUS_BADREQUEST = 400;

    /**自定义状态码 end**/

    /***redis user相关的key以这个打头**/
    public static final String TOKEN_PREFIX = "user_";

    /**
     * 用户登陆redis的过期时间
     */
    public interface RedisCacheExtime{
        int REDIS_SESSION_EXTIME = 60 * 60 * 10;
        int REDIS_FORGETTEN_PASSWORD_USER=60 *  60;
        int PRODUCT_KEY_EXPIRES=24*60*60;
        int USER_KEY_EXPIRES = 3600*24*2;
        int PRODUCT_STOCK_KEY_EXPIRES = -1;
    }

    /** 用户注册判断重复的参数类型 start **/
    public static final String EMAIL = "email";

    public static final String USERNAME = "username";
    /** 用户注册判断重复的参数类型 end **/

    /** 用户角色 **/
    public interface Role{
        int ROLE_CUSTOME = 0;//普通用户
        int ROLE_ADMIN = 1;//管理员用户
    }

    /**用户注册分布式锁路径***/
    public static final String USER_REGISTER_DISTRIBUTE_LOCK_PATH = "/user_reg";

    /** 产品的状态 **/
    public interface Product{
        int PRODUCT_ON = 1;
        int PRODUCT_OFF = 2;
        int PRODUCT_DELETED = 3;
    }
    public interface ProductListOrderBy{
        Set<String> PRICE_ASC_DESC = Sets.newHashSet("price_desc","price_asc");
    }

}
