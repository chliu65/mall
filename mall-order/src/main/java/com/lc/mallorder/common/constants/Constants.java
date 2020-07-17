package com.lc.mallorder.common.constants;


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


    /*订单锁定时间   */
    public static final long ORDER_LOCKERD_TIME= 60 * 1000;//ms

    /**
     * 用户登陆redis的过期时间
     */
    public interface RedisCacheExtime{
        int REDIS_SESSION_EXTIME = 60 * 60 * 10;
        int REDIS_FORGETTEN_PASSWORD_USER=60 *  60;
        int PRODUCT_KEY_EXPIRES=24*60*60;
        int USER_KEY_EXPIRES = 3600*24*2;
        int PRODUCT_STOCK_KEY_EXPIRES = -1;
        int ORDER_KEY_EXPIRES =15* 60 * 60;//s
        int ORDER_STATUS_KEY_EXPIRES=24 * 60* 60;
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
    public interface OrderKeyPrefix{
        static final String ORDER_DETAIL_KEY="ORDER_DETAIL_KEY";
        static final String ORDER_STATUS_KEY="ORDER_STATUS_KEY";
    }
    public enum OrderStatusEnum{
        CANCELED(0,"已取消"),
        NO_PAY(10,"未支付"),
        PAID(20,"已付款"),
        SHIPPED(40,"已发货"),
        ORDER_SUCCESS(50,"订单完成"),
        ORDER_CLOSE(60,"订单关闭");


        OrderStatusEnum(int code,String value){
            this.code = code;
            this.value = value;
        }
        private String value;
        private int code;

        public String getValue() {
            return value;
        }

        public int getCode() {
            return code;
        }
        public static OrderStatusEnum codeOf(int code){
            for(OrderStatusEnum orderStatusEnum : values()){
                if(orderStatusEnum.getCode() == code){
                    return orderStatusEnum;
                }
            }
            throw new RuntimeException("没有找到对应的枚举");
        }
    }
    public interface  AlipayCallback{
        String TRADE_STATUS_WAIT_BUYER_PAY = "WAIT_BUYER_PAY";
        String TRADE_STATUS_TRADE_SUCCESS = "TRADE_SUCCESS";

        String RESPONSE_SUCCESS = "success";
        String RESPONSE_FAILED = "failed";
    }

    public enum PayPlatformEnum{
        ALIPAY(1,"支付宝");

        PayPlatformEnum(int code,String value){
            this.code = code;
            this.value = value;
        }
        private String value;
        private int code;

        public String getValue() {
            return value;
        }

        public int getCode() {
            return code;
        }
    }

    public enum PaymentTypeEnum{
        ONLINE_PAY(1,"在线支付");

        PaymentTypeEnum(int code,String value){
            this.code = code;
            this.value = value;
        }
        private String value;
        private int code;

        public String getValue() {
            return value;
        }

        public int getCode() {
            return code;
        }


        public static PaymentTypeEnum codeOf(int code){
            for(PaymentTypeEnum paymentTypeEnum : values()){
                if(paymentTypeEnum.getCode() == code){
                    return paymentTypeEnum;
                }
            }
            throw new RuntimeException("么有找到对应的枚举");
        }
    }
}
