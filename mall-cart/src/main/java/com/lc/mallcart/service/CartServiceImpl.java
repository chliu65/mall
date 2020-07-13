package com.lc.mallcart.service;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import com.lc.mallcart.clients.ProductClient;
import com.lc.mallcart.common.constants.Constants;
import com.lc.mallcart.common.exception.GlobalException;
import com.lc.mallcart.common.keys.ProductKey;
import com.lc.mallcart.common.resp.ResponseEnum;
import com.lc.mallcart.common.resp.ServerResponse;
import com.lc.mallcart.common.utils.JsonUtil;
import com.lc.mallcart.common.utils.PropertiesUtil;
import com.lc.mallcart.dao.CartMapper;
import com.lc.mallcart.entity.Cart;
import com.lc.mallcart.entity.Product;
import com.lc.mallcart.vo.CartProductVo;
import com.lc.mallcart.vo.CartVo;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;


@Service
@Slf4j
public class CartServiceImpl implements ICartService{
    @Autowired
    private CartMapper cartMapper;
    @Autowired
    private ProductClient productClient;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public ServerResponse add(Integer userId, Integer productId, Integer count) {
        //1.校验参数
        if(userId == null){
            throw new GlobalException(ResponseEnum.LOGIN_EXPIRED);
        }
        if(productId == null || count == null){
            return ServerResponse.createByError(ResponseEnum.ILLEGAL_ARGUMENTS);
        }
        //2.校验商品
        ProductKey productKey=new ProductKey(productId);
        String productStr = stringRedisTemplate.opsForValue().get(productKey.getPrefix());
        Product product = null;
        if(productStr .isEmpty()){
            ServerResponse response = productClient.queryProduct(productId);
            Object object = response.getData();
            String objStr = JsonUtil.obj2String(object);
            product = (Product) JsonUtil.Str2Obj(objStr,Product.class);
            ProductKey newProductKey=new ProductKey(product.getId());
            stringRedisTemplate.opsForValue().set(newProductKey.getPrefix(),JsonUtil.obj2String(product) ,productKey.expireSeconds() , TimeUnit.SECONDS );
        }else {
            product = (Product) JsonUtil.Str2Obj(productStr,Product.class);
        }

        if(product == null){
            return ServerResponse.createByErrorMessage("商品不存在");
        }
        if(!product.getStatus().equals(Constants.Product.PRODUCT_ON)){
            return ServerResponse.createByErrorMessage("商品下架或者删除");
        }
        //3.根据商品或者购物车，购物车存在则增加商品数量即可，不存在则创建新的购物车，一个用户对应一个购物车
        Cart cart = cartMapper.selectByUserIdProductId(userId,productId);
        if (cart == null){
            Cart cartItem = new Cart();
            cartItem.setUserId(userId);
            cartItem.setProductId(productId);
            cartItem.setQuantity(count);
            cartItem.setChecked(Constants.Cart.CHECKED);

            int resultCount = cartMapper.insert(cartItem);
            if(resultCount == 0){
                return ServerResponse.createByErrorMessage("添加购物车失败");
            }
        }else {
            cart.setQuantity(cart.getQuantity()+count);
            int resultCount = cartMapper.updateByPrimaryKeySelective(cart);
            if(resultCount == 0){
                return ServerResponse.createByErrorMessage("添加购物车失败");
            }
        }
        //构建购物车信息，返回给前端，并且要检查库存
        CartVo cartVo = getCartVoLimit(userId,true);
        return ServerResponse.createBySuccess(cartVo);
    }

    @Override
    public ServerResponse update(Integer userId, Integer productId, Integer count) {
        if(productId == null || count == null){
            return ServerResponse.createByErrorMessage("参数错误");
        }
        Cart cart = cartMapper.selectByUserIdProductId(userId,productId);
        if(cart == null){
            return ServerResponse.createByErrorMessage("购物车不存在");
        }
        cart.setQuantity(count);
        int updateCount = cartMapper.updateByPrimaryKeySelective(cart);
        if(updateCount == 0){
            return ServerResponse.createByErrorMessage("更新购物车失败");
        }
        CartVo cartVo = this.getCartVoLimit(userId,true);
        return ServerResponse.createBySuccess(cartVo);
    }

    @Override
    public ServerResponse delete(Integer userId, String productIds) {
        List<String> productIdList = Splitter.on(",").splitToList(productIds);
        if(CollectionUtils.isEmpty(productIdList)){
            return ServerResponse.createByErrorMessage("参数错误");
        }
        int rowCount = cartMapper.deleteByProductIds(userId,productIdList);
        if(rowCount == 0){
            return ServerResponse.createByErrorMessage("此商品已经不存在于购物车中，请勿重复删除");
        }
        CartVo cartVo = this.getCartVoLimit(userId,false);
        return ServerResponse.createBySuccess(cartVo);
    }

    @Override
    public ServerResponse list(Integer userId) {
        CartVo cartVo = this.getCartVoLimit(userId,false);
        return ServerResponse.createBySuccess(cartVo);
    }

    @Override
    public ServerResponse selectOrUnSelect(Integer userId, int checked, Integer productId) {
        cartMapper.selectOrUnSelectProduct(userId,checked,productId);
        CartVo cartVo = this.getCartVoLimit(userId,false);
        return ServerResponse.createBySuccess(cartVo);
    }

    @Override
    public ServerResponse<Integer> get_cart_product_count(Integer userId) {
        if(userId == null){
            return ServerResponse.createByError(ResponseEnum.LOGIN_EXPIRED);
        }
        return ServerResponse.createBySuccess(cartMapper.selectCartProductCount(userId));
    }

    @Override
    public ServerResponse removeCart(Integer userId) {
//        List<Cart> cartList = cartMapper.selectCartByUserId(userId);
//        for(Cart cart:cartList){
//            cartMapper.deleteByPrimaryKey(cart.getId());
//        }
        cartMapper.deleteByUserId(userId);
        return ServerResponse.createBySuccessMessage("清除购物车成功");
    }

    /**
     * 比较通用的构建购物车的方法
     * @param userId
     * @return
     */
    private CartVo getCartVoLimit(Integer userId,boolean isJudgeStock) {
        CartVo cartVo = new CartVo();
        List<CartProductVo> cartProductVoList = Lists.newArrayList();
        List<Cart> cartList = cartMapper.selectCartByUserId(userId);
        if (CollectionUtils.isEmpty(cartList)){
            throw new GlobalException(ResponseEnum.CART_EMPTY);
        }
        BigDecimal cartTotalPrice = new BigDecimal("0");
        //1.遍历购物车，一条购物车记录对应一个商品，这些购物车共同对应到一个用户userId
        for(Cart cart:cartList){
            CartProductVo cartProductVo = new CartProductVo();
            cartProductVo.setId(cart.getId());
            cartProductVo.setUserId(cart.getUserId());
            cartProductVo.setProductId(cart.getProductId());
            //2.从redis中获取商品，获取不到则feign获取并且重置进redis中                //可优化为redis批量查询
            ProductKey productKey=new ProductKey(cart.getProductId());
            String productStr = stringRedisTemplate.opsForValue().get(productKey.getPrefix());
            Product product ;
            if(StringUtils.isBlank(productStr)){
                ServerResponse response = productClient.queryProduct(cart.getProductId());
                String dataStr=response.getData().toString();
                product = (Product) JsonUtil.Str2Obj(dataStr,Product.class);
                stringRedisTemplate.opsForValue().set(productKey.getPrefix(),dataStr , productKey.expireSeconds(), TimeUnit.SECONDS);
            }else {
                product = (Product) JsonUtil.Str2Obj(productStr, Product.class);
            }
            if (product==null){
                throw new GlobalException("product解析异常");
            }
            cartProductVo.setProductMainImage(product.getMainImage());
            cartProductVo.setProductName(product.getName());
            cartProductVo.setProductSubtitle(product.getSubtitle());
            cartProductVo.setProductStatus(product.getStatus());
            cartProductVo.setProductPrice(product.getPrice());
            cartProductVo.setProductStock(product.getStock());
            //3.判断这个商品的库存,有些接口不需要再去判断库存了，所以根据传进来的isJudgeStock这个boolean参数来决定是否判断库存
            int buyLimitCount = 0;
            if (isJudgeStock){
                if(product.getStock() > cart.getQuantity()){
                    //4.库存是够的
                    buyLimitCount = cart.getQuantity();
                    cartProductVo.setLimitQuantity(Constants.Cart.STOCK_IS_ENOUGH);
                }else {
                    //5.库存不够了,则返回当前最大库存
                    buyLimitCount = product.getStock();
                    cartProductVo.setLimitQuantity(Constants.Cart.STOCK_IS_NOT_ENOUGH);
                    Cart cartItem = new Cart();
                    cartItem.setId(cart.getId());
                    cartItem.setQuantity(buyLimitCount);
                    cartMapper.updateByPrimaryKeySelective(cartItem);
                }
            }else {
                buyLimitCount = cart.getQuantity();
            }

            //6.购买的数量已经是确定的了，下面就可以直接计算价格了
            cartProductVo.setQuantity(buyLimitCount);
            cartProductVo.setProductTotalPrice(product.getPrice().multiply(new BigDecimal(buyLimitCount)));
            cartProductVo.setProductChecked(cart.getChecked());
            //7.选中的，就加入到总价中
            if(cart.getChecked() == Constants.Cart.CHECKED){
                cartTotalPrice =cartTotalPrice.add(cartProductVo.getProductTotalPrice());
            }
            cartProductVoList.add(cartProductVo);
        }
        cartVo.setCartTotalPrice(cartTotalPrice);
        cartVo.setCartProductVoList(cartProductVoList);
        cartVo.setAllChecked(this.getAllCheckedStatus(userId));
 //       cartVo.setImageHost(PropertiesUtil.getProperty("ftp.server.http.prefix","http://img.oursnail.cn/"));
        log.info("购物车列表内容为：{}",cartVo);
        return cartVo;
    }

    /**
     * 0-未勾选，1-已勾选，所以我就找有没有未勾选的商品，找到就说明没有全选
     */
    private Boolean getAllCheckedStatus(Integer userId) {
        if(userId == null){
            throw new GlobalException(ResponseEnum.SERVER_ERROR);
        }
        return cartMapper.selectCartCheckedStatusByUserId(userId) == 0;
    }
}
