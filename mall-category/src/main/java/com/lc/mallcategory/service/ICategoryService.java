package com.lc.mallcategory.service;


import com.lc.mallcategory.common.resp.ServerResponse;

public interface ICategoryService {

    /** 根据类目id获取其下面所有的一级子类目 **/
    ServerResponse getCategory(Integer categoryId);

    /** 新建一个商品类目 **/
    ServerResponse addCategory(String categoryName, int parentId);

    /** 更新品类名称 **/
    ServerResponse<String> updateCategoryName(String categoryName, Integer categoryId);

    /** 递归查询出所有品类 **/
    ServerResponse selectCategoryAndDeepChildrenById(Integer categoryId);


    /** 被其他服务调用的接口 **/
    ServerResponse getCategoryDetail(Integer categoryId);
}
