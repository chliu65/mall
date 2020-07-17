package com.lc.mallpayment.common.keys;


import static com.lc.mallpayment.common.constants.Constants.RedisCacheExtime.USER_KEY_EXPIRES;

public class UserKey extends BasePrefix {


    public UserKey(Object prefix) {
        super(USER_KEY_EXPIRES, String.valueOf(prefix));
    }

    public UserKey(int expireSeconds, Object prefix) {
        super(expireSeconds, String.valueOf(prefix));
    }
}
