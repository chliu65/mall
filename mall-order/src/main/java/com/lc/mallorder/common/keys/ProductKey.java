package com.lc.mallorder.common.keys;


import static com.lc.mallorder.common.constants.Constants.RedisCacheExtime.PRODUCT_KEY_EXPIRES;

public class ProductKey extends BasePrefix  {
    public ProductKey(Object prefix) {
        super(PRODUCT_KEY_EXPIRES,String.valueOf(prefix));
    }

    public ProductKey(int expireSeconds, Object prefix) {
        super(expireSeconds, String.valueOf(prefix));
    }
}
