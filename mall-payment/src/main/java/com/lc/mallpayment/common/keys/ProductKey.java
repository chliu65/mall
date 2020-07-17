package com.lc.mallpayment.common.keys;


import static com.lc.mallpayment.common.constants.Constants.RedisCacheExtime.PRODUCT_KEY_EXPIRES;

public class ProductKey extends BasePrefix  {
    public ProductKey(Object prefix) {
        super(PRODUCT_KEY_EXPIRES,String.valueOf(prefix));
    }

    public ProductKey(int expireSeconds, Object prefix) {
        super(expireSeconds, String.valueOf(prefix));
    }
}
