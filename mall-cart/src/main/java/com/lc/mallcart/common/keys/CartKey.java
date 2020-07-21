package com.lc.mallcart.common.keys;

public class CartKey extends BasePrefix {
    public CartKey(Object id) {
        super(id.toString());
    }

    public CartKey(int expireSeconds, Object id) {
        super(expireSeconds, id.toString());
    }
}
