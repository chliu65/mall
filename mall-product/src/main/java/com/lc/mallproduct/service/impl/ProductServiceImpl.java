package com.lc.mallproduct.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.lc.mallproduct.clients.CategoryClient;
import com.lc.mallproduct.common.constants.Constants;
import com.lc.mallproduct.common.exception.GlobalException;
import com.lc.mallproduct.common.keys.ProductKey;
import com.lc.mallproduct.common.keys.ProductStockKey;
import com.lc.mallproduct.common.resp.ResponseEnum;
import com.lc.mallproduct.common.resp.ServerResponse;
import com.lc.mallproduct.common.utils.DateTimeUtil;
import com.lc.mallproduct.common.utils.JsonUtil;
import com.lc.mallproduct.dao.ProductMapper;
import com.lc.mallproduct.entity.Category;
import com.lc.mallproduct.entity.Product;
import com.lc.mallproduct.service.ProductService;
import com.lc.mallproduct.vo.ProductDetailVo;
import com.lc.mallproduct.vo.ProductListVo;
import com.lc.mallproduct.vo.StockReduceVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;


@Service
@Slf4j
public class ProductServiceImpl implements ProductService {
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private CategoryClient categoryClient;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;


    @Override
    public ServerResponse list(int pageNum, int pageSize) {
        //1.pagehelper对下一行取出的集合进行分页
        PageHelper.startPage(pageNum,pageSize);
        List<Product> productList = productMapper.selectList();
        List<ProductListVo> productListVoList = Lists.newArrayList();
        for(Product product:productList){
            ProductListVo productListVo = assembleProductListVo(product);
            productListVoList.add(productListVo);
        }
        //2.返回给前端的还需要一些其他的分页信息，为了不丢失这些信息，需要进行下面的处理
        PageInfo pageInfo = new PageInfo(productList);
        pageInfo.setList(productListVoList);
        return ServerResponse.createBySuccess(pageInfo);
    }

    @Override
    public ServerResponse<PageInfo> search(String productName, Integer productId, int pageNum, int pageSize) {
        //开始准备分页
        PageHelper.startPage(pageNum,pageSize);
        //如果有内容，可以先在这里封装好，直接传到sql中去
        if(StringUtils.isNotBlank(productName)){
            productName = new StringBuilder().append("%").append(productName).append("%").toString();
        }
        List<Product> productList = productMapper.selectProductByNameAndId(productName,productId);
        //转换一下传给前端显示
        List<ProductListVo> productListVoList = Lists.newArrayList();
        for(Product product:productList){
            ProductListVo productListVo = assembleProductListVo(product);
            productListVoList.add(productListVo);
        }
        PageInfo pageInfo = new PageInfo(productList);
        pageInfo.setList(productListVoList);
        return ServerResponse.createBySuccess(pageInfo);
    }

    @Override
    public ServerResponse<ProductDetailVo> detail(Integer productId) {
        //1，校验参数
        if(productId == null){
            throw new GlobalException(ResponseEnum.ILLEGAL_ARGUMENTS);
        }
        //1-在售 2-下架 3-删除
        Product product = productMapper.selectByPrimaryKey(productId);
        if(product == null){
            return ServerResponse.createByError(ResponseEnum.PRODUCT_NOT_EXIST);
        }
        ProductDetailVo productDetailVo = assembleProductDetailVo(product);
        return ServerResponse.createBySuccess(productDetailVo);
    }

    @Override
    public ServerResponse<String> set_sale_status(Integer productId, Integer status) {
        //1.校验参数
        if(productId == null || status == null){
            return ServerResponse.createByError(ResponseEnum.ILLEGAL_ARGUMENTS);
        }
        //2.更新状态
        Product product = new Product();
        product.setId(productId);
        product.setStatus(status);
        //3.删除该商品缓存
        stringRedisTemplate.delete(new ProductKey(String.valueOf(productId)).getPrefix());
        int rowCount = productMapper.updateByPrimaryKeySelective(product);
        if(rowCount > 0){
            return ServerResponse.createBySuccess();
        }
        return ServerResponse.createByErrorMessage("更新产品状态失败");
    }

    @Override
    public ServerResponse<String> saveOrUpdateProduct(Product product) {
        Integer productId=product.getId();
        //1.校验参数
        if(product == null || product.getCategoryId()==null){
            return ServerResponse.createByError(ResponseEnum.ILLEGAL_ARGUMENTS);
        }
        //2.设置一下主图，主图为子图的第一个图
        if(StringUtils.isNotBlank(product.getSubImages())){
            String[] subImages = product.getSubImages().split(",");
            if(subImages.length > 0){
                product.setMainImage(subImages[0]);
            }
        }
        //3.看前端传过来的产品id是否存在，存在则为更新，否则为新增
        if(productId != null){
            int rowCount = productMapper.updateByPrimaryKeySelective(product);
            if (rowCount<=0){
                log.info("【商品{}更新失败】",productId);
                throw new GlobalException(ResponseEnum.SERVER_ERROR);
            }
        }else {
            product.setCreateTime(new Date());//这两句可能多余，因为xml中已经保证了，就先放这里
            product.setUpdateTime(new Date());
            int rowCount = productMapper.insert(product);
            if(rowCount <=0){
                log.info("【商品{}添加失败】",productId);
                throw new GlobalException(ResponseEnum.SERVER_ERROR);
            }

        }
        ProductKey productKey=new ProductKey(String.valueOf(productId));
        stringRedisTemplate.opsForValue().set(productKey.getPrefix(),
                JsonUtil.obj2String(product),
                productKey.expireSeconds(),TimeUnit.SECONDS );
        ProductStockKey productStockKey=new ProductStockKey(String.valueOf(productId));
        stringRedisTemplate.opsForValue().set(productStockKey.getPrefix(),
                String.valueOf(product.getStock()),
                productStockKey.expireSeconds(),TimeUnit.SECONDS );
        return ServerResponse.createBySuccess();
    }



    @Override
    public ServerResponse getPortalProductDetail(Integer productId) {
        if(productId == null){
            return ServerResponse.createByError(ResponseEnum.ILLEGAL_ARGUMENTS);
        }
        Product product = productMapper.selectByPrimaryKey(productId);
        if(null==product || product.getStatus() != Constants.Product.PRODUCT_ON){
            return ServerResponse.createByError(ResponseEnum.PRODUCT_NOT_EXIST);
        }
        ProductDetailVo productDetailVo = assembleProductDetailVo(product);
        return ServerResponse.createBySuccess(productDetailVo);
    }

    @Override
    public ServerResponse<PageInfo> portalList(String keyword, Integer categoryId, String orderBy, int pageNum, int pageSize) {
        //准备盛放categoryIds
        List<Integer> categoryIdList = Lists.newArrayList();
        //如果categoryId不为空
        if(categoryId != null){
            //对于这里，直接强转出错了，所以我就序列化处理了一下
            ServerResponse response = categoryClient.getCategoryDetail(categoryId);
            Object object = response.getData();
            String objStr = JsonUtil.obj2String(object);
            Category category = JsonUtil.Str2Obj(objStr,Category.class);
            if(category == null && StringUtils.isBlank(keyword)){
                ////直接返回空
                PageHelper.startPage(pageNum,pageSize);
                List<ProductListVo> productListVoList = Lists.newArrayList();
                PageInfo pageInfo = new PageInfo(productListVoList);
                return ServerResponse.createBySuccess(pageInfo);
            }
            //说明category还是存在的
            categoryIdList = (List<Integer>) categoryClient.getDeepCategory(categoryId).getData();
        }
        //如果keyword不为空
        if(StringUtils.isNotBlank(keyword)){
            keyword = new StringBuilder().append("%").append(keyword).append("%").toString();
        }
        //如果orderBy不为空
        if(StringUtils.isNotBlank(orderBy)){
            if(Constants.ProductListOrderBy.PRICE_ASC_DESC.contains(orderBy)){
                String[] orderByArray = orderBy.split("_");
                //特定的格式
                PageHelper.orderBy(orderByArray[0]+" "+orderByArray[1]);
            }
        }
        PageHelper.startPage(pageNum,pageSize);
        //模糊查询
        List<Product> productList = productMapper.selectByNameAndCategoryIds(StringUtils.isBlank(keyword)?null:keyword,
                                                                             categoryIdList.size()==0?null:categoryIdList);
        //封装返回对象
        List<ProductListVo> productListVoList = Lists.newArrayList();
        for(Product product : productList){
            ProductListVo productListVo = assembleProductListVo(product);
            productListVoList.add(productListVo);
        }
        //返回
        PageInfo pageInfo = new PageInfo(productList);
        pageInfo.setList(productListVoList);
        return ServerResponse.createBySuccess(pageInfo);
    }

    @Override
    public ServerResponse queryProduct(Integer productId) {
        //1.校验参数
        if(productId == null){
            return ServerResponse.createByError(ResponseEnum.ILLEGAL_ARGUMENTS);
        }
        //2.去redis中查询，没有则把商品重新添加进redis中
        String redisProductStr = stringRedisTemplate.opsForValue().get(new ProductKey(String.valueOf(productId)).getPrefix());
        Product product;
        if (StringUtils.isBlank(redisProductStr)){
             product= productMapper.selectByPrimaryKey(productId);
            if(product == null|| product.getStatus() != Constants.Product.PRODUCT_ON){
                return ServerResponse.createByError(ResponseEnum.PRODUCT_NOT_EXIST);
            }
            ProductKey productKey=new ProductKey(String.valueOf(productId));
            stringRedisTemplate.opsForValue().set(productKey.getPrefix(),JsonUtil.obj2String(product) , productKey.expireSeconds(), TimeUnit.SECONDS );
        }else {
            product = JsonUtil.Str2Obj(redisProductStr,Product.class);
        }
        return ServerResponse.createBySuccess(product);
    }


    private ProductListVo assembleProductListVo(Product product) {
        ProductListVo productListVo = new ProductListVo();
        BeanUtils.copyProperties(product,productListVo );
//        productListVo.setImageHost(PropertiesUtil.getProperty("ftp.server.http.prefix","http://image.snail.com/"));
        return productListVo;
    }

    private ProductDetailVo assembleProductDetailVo(Product product){
        ProductDetailVo productDetailVo = new ProductDetailVo();
        BeanUtils.copyProperties(product,productDetailVo );

//        productDetailVo.setImageHost(PropertiesUtil.getProperty("ftp.server.http.prefix","http://image.snail.com/"));
        ServerResponse categorySeverResponse=categoryClient.getCategoryDetail(product.getCategoryId());
        if(categorySeverResponse.isSuccess()){
            Category category = (Category) JsonUtil.Str2Obj( categorySeverResponse.getData().toString(),Category.class);
            productDetailVo.setParentCategoryId(category.getParentId());
        }else {
            productDetailVo.setParentCategoryId(0);
        }
        productDetailVo.setCreateTime(DateTimeUtil.dateToStr(product.getCreateTime()));
        productDetailVo.setUpdateTime(DateTimeUtil.dateToStr(product.getUpdateTime()));
        return productDetailVo;
    }



    @Override
    public ServerResponse preInitProductStcokToRedis() {
        List<Product> productList = productMapper.selectList();
        for(Product product:productList){
            Integer productId = product.getId();
            Integer stock = product.getStock();
            if(productId != null && stock != null && product.getStatus().equals(Constants.Product.PRODUCT_ON)){
                ProductStockKey productStockKey=new ProductStockKey(String.valueOf(productId));
                stringRedisTemplate.opsForValue().set(productStockKey.getPrefix(),String.valueOf(stock) ,productStockKey.expireSeconds() ,TimeUnit.SECONDS );
            }
        }
        return ServerResponse.createBySuccess();
    }

    @Override
    public ServerResponse preInitProductListToRedis() {
        List<Product> productList = productMapper.selectList();
        for(Product product:productList){
            Integer productId = product.getId();
            if(productId != null  && product.getStatus().equals(Constants.Product.PRODUCT_ON)){
                ProductKey productKey=new ProductKey(String.valueOf(productId));
                stringRedisTemplate.opsForValue().set(productKey.getPrefix(),JsonUtil.obj2String(product) ,productKey.expireSeconds() , TimeUnit.SECONDS);
            }
        }
        return ServerResponse.createBySuccess();
    }

    @Override
    @Transactional
    public ServerResponse reduceStock(List<StockReduceVo> stockReduceVoList) {
        for (StockReduceVo stockReduceVo:stockReduceVoList){
            Product product=productMapper.selectByPrimaryKey(stockReduceVo.getProductId());
            if (product==null){
                throw new GlobalException(ResponseEnum.PRODUCT_NOT_EXIST);
            }
            int result=productMapper.reduceStock(stockReduceVo);
            //更新库存失败，库存不足
            if (result==0){
                throw new GlobalException(ResponseEnum.STOCK_IS_NOT_ENOUGH);

            }
        }
        return ServerResponse.createBySuccess();
    }


}
