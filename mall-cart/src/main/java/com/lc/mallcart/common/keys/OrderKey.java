package com.lc.mallcart.common.keys;


import com.lc.mallcart.common.constants.Constants;

public class OrderKey extends BasePrefix{
    public OrderKey(Object id) {
        super(Constants.RedisCacheExtime.ORDER_KEY_EXPIRES,String.valueOf(id));
    }
    public OrderKey(String prefix, Object id){
        super(Constants.RedisCacheExtime.ORDER_KEY_EXPIRES,prefix+ String.valueOf(id));
    }
    public OrderKey(int expireSeconds, String prefix, Object id){
        super(expireSeconds,prefix+ String.valueOf(id));
    }
    public OrderKey(int expireSeconds, Object id) {
        super(expireSeconds, String.valueOf(id));
    }
}
