package com.lc.malluser.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;

@FeignClient("mall-uniqueid")
public interface UniqueIdClient {
    @RequestMapping("/uniqueid/getUniqueId")
    String getUniqueId();
}
