package com.lc.mallproduct.clients;


import com.lc.mallproduct.common.resp.ServerResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient("mall-category")
public interface CategoryClient {
    @RequestMapping("/manage/category/get_category_detail.do")
    ServerResponse getCategoryDetail(@RequestParam("categoryId") Integer categoryId);

    @RequestMapping("/manage/category/get_deep_category.do")
    ServerResponse getDeepCategory(@RequestParam(value = "categoryId") Integer categoryId);
}
