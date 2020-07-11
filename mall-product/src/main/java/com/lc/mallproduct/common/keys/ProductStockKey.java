package com.lc.mallproduct.common.keys;

import static com.lc.mallproduct.common.constants.Constants.RedisCacheExtime.PRODUCT_STOCK_KEY_EXPIRES;

public class ProductStockKey extends BasePrefix {
    public ProductStockKey(String prefix) {
        super(PRODUCT_STOCK_KEY_EXPIRES,prefix);
    }

    public ProductStockKey(int expireSeconds, String prefix) {
        super(expireSeconds, prefix);
    }
}
