package com.lc.mallorder.common.keys;

public interface KeyPrefix {
    //redis里边键的过期时间
    public int expireSeconds();

    public String getPrefix();
}
