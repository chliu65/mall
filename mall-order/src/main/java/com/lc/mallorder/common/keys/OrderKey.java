package com.lc.mallorder.common.keys;

import com.lc.mallorder.common.constants.Constants;
import org.apache.commons.lang3.StringUtils;

public class OrderKey extends BasePrefix{
    public static final String ORDER_COST="_COST_";
    public static final String ORDER_STATUS="_STATUS_";
    public static final String ORDER_ITEM="_ITEM_";
    public OrderKey(Object id) {
        super(Constants.RedisCacheExtime.ORDER_KEY_EXPIRES,String.valueOf(id));
    }
    public OrderKey(String prefix,Object id){
        super(Constants.RedisCacheExtime.ORDER_KEY_EXPIRES,prefix+ String.valueOf(id));
    }
    public OrderKey(int expireSeconds,String prefix,Object id){
        super(expireSeconds,prefix+ String.valueOf(id));
    }
    public OrderKey(int expireSeconds, Object id) {
        super(expireSeconds, String.valueOf(id));
    }
}
