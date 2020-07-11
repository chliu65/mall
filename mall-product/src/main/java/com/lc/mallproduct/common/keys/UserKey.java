package com.lc.mallproduct.common.keys;

import static com.lc.mallproduct.common.constants.Constants.RedisCacheExtime.USER_KEY_EXPIRES;

public class UserKey extends BasePrefix {


    public UserKey(String prefix) {
        super(USER_KEY_EXPIRES, prefix);
    }

}
