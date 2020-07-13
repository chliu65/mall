package com.lc.mallcart.common.keys;

import static com.lc.mallcart.common.constants.Constants.RedisCacheExtime.PRODUCT_STOCK_KEY_EXPIRES;

public class ProductStockKey extends BasePrefix {
    public ProductStockKey(Object prefix) {
        super(PRODUCT_STOCK_KEY_EXPIRES,String.valueOf(prefix));
    }

    public ProductStockKey(int expireSeconds, Object prefix) {
        super(expireSeconds, String.valueOf(prefix));
    }
}
