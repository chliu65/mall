package com.lc.mallcategory.controller;


import com.lc.mallcategory.common.resp.ServerResponse;
import com.lc.mallcategory.service.ICategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;




@RestController
@RequestMapping("/category/")
public class CategoryController {
    @Autowired
    private ICategoryService categoryService;

    /**
     * 获取品类子节点(平级)
     */
    @RequestMapping("get_category.do")
    public ServerResponse getCategory(@RequestParam(value = "categoryId",defaultValue = "0") Integer categoryId){
        ServerResponse response = categoryService.getCategory(categoryId);
        return response;
    }

    /**
     * 增加节点
     */
    @RequestMapping("add_category.do")
    public ServerResponse addCategory(String categoryName, @RequestParam(value = "parentId",defaultValue = "0")int parentId){
        ServerResponse response = categoryService.addCategory(categoryName,parentId);
        return response;
    }

    /**
     * 修改品类名称
     */
    @RequestMapping("set_category_name.do")
    public ServerResponse<String> set_category_name(String categoryName,Integer categoryId){
        return categoryService.updateCategoryName(categoryName,categoryId);
    }

    /**
     * 递归获取自身和所有的子节点 //service实现有问题
     */
    @RequestMapping("get_deep_category.do")
    public ServerResponse get_deep_category(@RequestParam(value = "categoryId",defaultValue = "0") Integer categoryId){
        return categoryService.selectCategoryAndDeepChildrenById(categoryId);
    }


    /**
     * 这是为了给其他服务调用而新增的接口
     */
    @RequestMapping("get_category_detail.do")
    public ServerResponse get_category_detail(Integer categoryId){
        return categoryService.getCategoryDetail(categoryId);
    }

}
