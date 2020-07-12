package com.lc.mallcategory.service;

import com.google.common.collect.Sets;
import com.lc.mallcategory.common.exception.GlobalException;
import com.lc.mallcategory.common.resp.ResponseEnum;
import com.lc.mallcategory.common.resp.ServerResponse;
import com.lc.mallcategory.dao.CategoryMapper;

import com.lc.mallcategory.entity.Category;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


@Service
@Slf4j
public class CategoryServiceImpl implements ICategoryService{
    @Autowired
    private CategoryMapper categoryMapper;


    @Override
    public ServerResponse getCategory(Integer categoryId) {
        //1.校验参数
        if(categoryId == null){
            throw new GlobalException(ResponseEnum.ILLEGAL_ARGUMENTS);
        }
        //2.根据父亲id获取这个父亲下一级所有子ID
        List<Category> categoryList = categoryMapper.selectCategoryChildrenByParentId(categoryId);
        if(CollectionUtils.isEmpty(categoryList)){
            log.info("该节点下没有任何子节点");
        }
        return ServerResponse.createBySuccess(categoryList);
    }

    @Override
    public ServerResponse addCategory(String categoryName, int parentId) {
        //1.校验参数
        if(StringUtils.isBlank(categoryName)){
            throw new GlobalException(ResponseEnum.ILLEGAL_ARGUMENTS);
        }
        //2.创建类目
        Category category = new Category();
        category.setName(categoryName);
        category.setParentId(parentId);
        category.setStatus(true);

        int resultCount = categoryMapper.insert(category);
        if(resultCount > 0){
            return ServerResponse.createBySuccess();
        }
        return ServerResponse.createByErrorMessage("添加品类失败");
    }

    @Override
    public ServerResponse<String> updateCategoryName(String categoryName, Integer categoryId) {
        //1.校验参数
        if(StringUtils.isBlank(categoryName)){
            throw new GlobalException(ResponseEnum.ILLEGAL_ARGUMENTS);
        }
        //2.根据id获取品类
        Category tmpCat = categoryMapper.selectByPrimaryKey(categoryId);
        if(tmpCat == null){
            throw new GlobalException(ResponseEnum.PARENT_CATEGORY_NOT_EXIST);
        }
        //3.更新品类名称
        Category category = new Category();
        category.setId(categoryId);
        category.setName(categoryName);
        int resultCount = categoryMapper.updateByPrimaryKeySelective(category);
        if(resultCount > 0){
            return ServerResponse.createBySuccess();
        }
        return ServerResponse.createByErrorMessage("更新品类名称失败");
    }
//递归查询数据库，不建议，待优化
    @Override
    public ServerResponse selectCategoryAndDeepChildrenById(Integer categoryId) {
        //1、创建一个空Set用来存放不重复的品类对象--去重
        Set<Category> categorySet = Sets.newHashSet();
        //2、递归获取所有的子节点（儿子、孙子、等等），包括自己也添加进去
        findChildCategory(categorySet,categoryId);
        //3、将递归获取到的品类id取出来放进list中
        List<Integer> categoryIdList = new ArrayList<>();
        if(categoryId != null){
            for(Category category:categorySet){
                categoryIdList.add(category.getId());
            }
        }
        return ServerResponse.createBySuccess(categoryIdList);
    }

    private void findChildCategory(Set<Category> categorySet,Integer categoryId){
        //4、如果自己不为空的话，首先把自己添加进去；如果自己为空，这个递归分支就结束，所以也是一个停止条件
        Category category = categoryMapper.selectByPrimaryKey(categoryId);
        if(category != null){
            categorySet.add(category);
        }
        //5、根据父亲id获取下一级所有品类（即先获取儿子们）
        List<Category> categoryList = categoryMapper.selectCategoryChildrenByParentId(categoryId);
        //6、根据每一个儿子再获取儿子的儿子们，递归下去//一边遍历集合，一边添加元素，有bug
        for(Category categoryItem:categoryList){
            findChildCategory(categorySet,categoryItem.getId());
        }
        return ;
    }


    @Override
    public ServerResponse getCategoryDetail(Integer categoryId) {
        if(categoryId == null){
            return ServerResponse.createByError(ResponseEnum.ILLEGAL_ARGUMENTS);
        }
        Category category = categoryMapper.selectByPrimaryKey(categoryId);
        if(category == null){
            return ServerResponse.createByError(ResponseEnum.CATEGORY_NOT_EXIST);
        }
        return ServerResponse.createBySuccess(category);
    }

}
