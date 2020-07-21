package com.lc.mallcart.service;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import com.lc.mallcart.clients.ProductClient;
import com.lc.mallcart.common.constants.Constants;
import com.lc.mallcart.common.exception.GlobalException;
import com.lc.mallcart.common.keys.CartKey;
import com.lc.mallcart.common.keys.ProductKey;
import com.lc.mallcart.common.keys.ProductStockKey;
import com.lc.mallcart.common.resp.ResponseEnum;
import com.lc.mallcart.common.resp.ServerResponse;
import com.lc.mallcart.common.utils.JsonUtil;
import com.lc.mallcart.dao.CartMapper;
import com.lc.mallcart.entity.Cart;
import com.lc.mallcart.entity.Product;
import com.lc.mallcart.vo.*;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;


@Service
@Slf4j
public class CartServiceImpl implements CartService {
    @Autowired
    private CartMapper cartMapper;
    @Autowired
    private ProductClient productClient;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 更新购物车商品数量
     * @param userId
     * @param productId
     * @param count
     * @return
     */
    @Override
    public ServerResponse add(Integer userId, Integer productId, Integer count) {
//        //0.校验参数
//        if(userId == null){
//            throw new GlobalException(ResponseEnum.LOGIN_EXPIRED);
//        }
//        if(productId == null || count == null){
//            return ServerResponse.createByError(ResponseEnum.ILLEGAL_ARGUMENTS);
//        }
        //1.校验商品(是否在售，添加到购物车中的商品数量是否合理)
        ProductKey productKey=new ProductKey(productId);
        String productStr = stringRedisTemplate.opsForValue().get(productKey.getPrefix());
        ProductInfoVo productInfoVo=new ProductInfoVo();
        if(StringUtils.isBlank(productStr)){
            ServerResponse response = productClient.queryProduct(productId);
            Object object = response.getData();
            String objStr = JsonUtil.obj2String(object);
            Product product = (Product) JsonUtil.Str2Obj(objStr,Product.class);
            BeanUtils.copyProperties(product,productInfoVo );
        }else {
            productInfoVo = (ProductInfoVo) JsonUtil.Str2Obj(productStr,ProductInfoVo.class);
        }
        if(productInfoVo == null){
            return ServerResponse.createByError(ResponseEnum.PRODUCT_NOT_EXIST);
        }
        if(!productInfoVo.getStatus().equals(Constants.Product.PRODUCT_ON)){
            return ServerResponse.createByErrorMessage("商品下架或者删除");
        }
        ProductStockKey productStockKey=new ProductStockKey(productId);
        String productStock=stringRedisTemplate.opsForValue().get(productStockKey.getPrefix());
        if (count>Integer.valueOf(productStock)){
            return ServerResponse.createByError(ResponseEnum.STOCK_IS_NOT_ENOUGH);
        }
        Cart cartItem = new Cart();
        cartItem.setUserId(userId);
        cartItem.setProductId(productId);
        cartItem.setQuantity(count);
        cartItem.setChecked(Constants.Cart.CHECKED);
        MessageVo messageVo=new MessageVo();
        messageVo.setOrder("UPDATE");
        messageVo.setData(cartItem);
        //2.更新redis,商品数量符号表示是否选中，绝对值为购物车中商品真实数量,添加时默认为选中状态
        CartKey cartKey=new CartKey(userId);
        Map<String,String> map=new HashMap<>();
        map.put(String.valueOf(productId), String.valueOf(count));
        stringRedisTemplate.opsForHash().putAll(cartKey.getPrefix(),map );
        //3.异步添加到购物车数据库
        rabbitTemplate.convertAndSend(Constants.CART_EXCHANGE,
                Constants.QueueNameAndBindingEnum.UPDATE_CART.getRoutingKey() ,
                JsonUtil.obj2String(messageVo));
        return ServerResponse.createBySuccess();
    }

//    @Override
//    public ServerResponse update(Integer userId, Integer productId, Integer count) {
//        if(productId == null || count == null){
//            return ServerResponse.createByErrorMessage("参数错误");
//        }
//        Cart cart = cartMapper.selectByUserIdProductId(userId,productId);
//        if(cart == null){
//            cart=new Cart();
//            cart.setUserId(userId);
//            cart.setProductId(productId);
//            cart.setQuantity(count);
//            cart.setChecked(Constants.Cart.CHECKED);
//            cart.setCreateTime(new Date());
//            cart.setUpdateTime(new Date());
//            cartMapper.insert(cart);
//        }else {
//            cart.setQuantity(count);
//            cart.setUpdateTime(new Date());
//            cartMapper.updateByPrimaryKey(cart);
//        }
//        return ServerResponse.createBySuccess();
//    }

    /**
     * 删除购物车中的某商品
     * @param userId
     * @param productId
     * @return
     */
    @Override
    public ServerResponse delete(Integer userId, String productId) {
        //先删除redis中的数据，再异步更新数据库
        CartKey cartKey=new CartKey(userId);
        if (stringRedisTemplate.opsForHash().hasKey(cartKey.getPrefix(), productId)){
            stringRedisTemplate.opsForHash().delete(cartKey.getPrefix(),productId);
        }
        CartUserProductVo cartUserProductVo=new CartUserProductVo(userId,Integer.valueOf(productId));
        MessageVo messageVo=new MessageVo();
        messageVo.setOrder("DELETE");
        messageVo.setData(cartUserProductVo);
        rabbitTemplate.convertAndSend(Constants.CART_EXCHANGE,Constants.QueueNameAndBindingEnum.UPDATE_CART.getRoutingKey() , JsonUtil.obj2String(messageVo));
        return ServerResponse.createBySuccess();
    }

    /**
     * 查询数据库后放入redis
     * @param userId
     * @return
     */
    @Override
    public ServerResponse list(Integer userId) {
        List<Cart> cartList=cartMapper.selectCartByUserId(userId);
        if (cartList.size()==0){
            return ServerResponse.createBySuccessMessage("购物车为空");
        }
        Map<String,String> map=new HashMap<>();
        //正负号表示是否被选中，绝对值表示数量
        for (Cart cart:cartList){
            if (cart.getChecked()==Constants.Cart.CHECKED){
                map.put(String.valueOf(cart.getProductId()),String.valueOf(cart.getQuantity()) );
            }else {
                map.put(String.valueOf(cart.getProductId()),String.valueOf(0-cart.getQuantity()) );
            }
        }
        CartKey cartKey=new CartKey(userId);
        stringRedisTemplate.opsForHash().putAll(cartKey.getPrefix(),map );
        return ServerResponse.createBySuccess(cartList);
    }

    /**
     * 选中商品或取消选中
     * @param userId
     * @param checked
     * @param productId
     * @return
     */
    @Override
    public ServerResponse selectOrUnSelect(Integer userId, int checked, Integer productId) {
        CartKey cartKey=new CartKey(userId);
        Cart cart=new Cart();
        cart.setUserId(userId);
        cart.setProductId(productId);
        cart.setChecked(checked);
        if (stringRedisTemplate.hasKey(cartKey.getPrefix())){
            String productQuantityStr=(String) stringRedisTemplate.opsForHash().get(cartKey.getPrefix(),String.valueOf(productId) );
            Integer productQuantityInt=Math.abs(Integer.valueOf(productQuantityStr));
            String newProductQuantityStr=checked==Constants.Cart.CHECKED?String.valueOf(productQuantityInt):String.valueOf(0-productQuantityInt);
            stringRedisTemplate.opsForHash().put(cartKey.getPrefix(), String.valueOf(productId), newProductQuantityStr);
            MessageVo messageVo=new MessageVo();
            messageVo.setOrder("UPDATE");
            messageVo.setData(cart);
            rabbitTemplate.convertAndSend(Constants.CART_EXCHANGE,Constants.QueueNameAndBindingEnum.UPDATE_CART.getRoutingKey() , JsonUtil.obj2String(messageVo) );
        }else {
            cartMapper.updateByPrimaryKeySelective(cart);
            Cart newCart=cartMapper.selectByUserIdProductId(userId, productId);
            if (newCart.getChecked()==Constants.Cart.CHECKED){
                stringRedisTemplate.opsForHash().put(cartKey.getPrefix(),String.valueOf(productId) ,String.valueOf(newCart.getQuantity()) );
            }else {
                stringRedisTemplate.opsForHash().put(cartKey.getPrefix(),String.valueOf(productId) ,String.valueOf(0-newCart.getQuantity()) );
            }
        }
        return ServerResponse.createBySuccess();
    }

    /**
     * 获取购物车商品种类数
     * @param userId
     * @return
     */
    @Override
    public ServerResponse<Integer> get_cart_product_count(Integer userId) {

        if(userId == null){
            return ServerResponse.createByError(ResponseEnum.LOGIN_EXPIRED);
        }
        return ServerResponse.createBySuccess(cartMapper.selectCartProductCount(userId));
    }

    /**
     * 清空购物车
     * @param userId
     * @return
     */
    @Override
    public ServerResponse removeCart(String userId) {
        CartKey cartKey=new CartKey(userId);
        if (stringRedisTemplate.hasKey(cartKey.getPrefix())){
            stringRedisTemplate.delete(cartKey.getPrefix());
        }
        MessageVo messageVo =new MessageVo();
        messageVo.setOrder("REMOVE");
        messageVo.setData(userId);
        rabbitTemplate.convertAndSend(Constants.CART_EXCHANGE,Constants.QueueNameAndBindingEnum.UPDATE_CART.getRoutingKey() , JsonUtil.obj2String(messageVo) );
    }

    /**
     * 删除cart数据库用户购物车记录
     * @param userId
     * @return
     */
    @Override
    public ServerResponse removeCartDB(String userId) {
//        List<Cart> cartList = cartMapper.selectCartByUserId(userId);
//        for(Cart cart:cartList){
//            cartMapper.deleteByPrimaryKey(cart.getId());
//        }
        cartMapper.deleteByUserId(Integer.valueOf(userId));
        return ServerResponse.createBySuccessMessage("清除购物车成功");
    }

    /**
     * 删除cart数据库记录
     * @param userId
     * @param productId
     * @return
     */
    @Override
    public ServerResponse deleteCartDB(Integer userId, Integer productId) {
        List<String> productIdList=new LinkedList<>();
        productIdList.add(String.valueOf(productId));
        cartMapper.deleteByProductIds(userId, productIdList);
        return ServerResponse.createBySuccess();
    }

    /**
     * 更新cart数据库记录（insert或update）
     * @param cart
     * @return
     */
    @Override
    public ServerResponse updateCartDB(Cart cart) {

        log.info("更新userid:{},productid:{}",cart.getId(),cart.getProductId() );
        //更新cart数据库表
        Cart oldcart=cartMapper.selectByUserIdProductId(cart.getUserId(), cart.getProductId());
        if (oldcart==null){
            cart.setCreateTime(new Date());
            cart.setUpdateTime(new Date());
            cartMapper.insert(cart);
        }else {
            cart.setId(oldcart.getId());
            cart.setUpdateTime(new Date());
            cartMapper.updateByPrimaryKeySelective(cart);
        }
        return ServerResponse.createBySuccess();
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
