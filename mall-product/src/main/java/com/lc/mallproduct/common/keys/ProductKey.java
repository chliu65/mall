package com.lc.mallproduct.common.keys;

import static com.lc.mallproduct.common.constants.Constants.RedisCacheExtime.PRODUCT_KEY_EXPIRES;

public class ProductKey extends BasePrefix  {
    public ProductKey(String prefix) {
        super(PRODUCT_KEY_EXPIRES,prefix);
    }

    public ProductKey(int expireSeconds, String prefix) {
        super(expireSeconds, prefix);
    }
}
