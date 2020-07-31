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
import org.springframework.amqp.rabbit.support.CorrelationData;
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
        //1.校验商品(是否在售，添加到购物车中的商品数量是否合理)
        ProductKey productKey=new ProductKey(productId);
        String productStr = stringRedisTemplate.opsForValue().get(productKey.getPrefix());
        ProductInfoVo productInfoVo=new ProductInfoVo();
        if(StringUtils.isBlank(productStr)){
            ServerResponse response = productClient.queryProduct(productId);
            if (!response.isSuccess()){
                return response;
            }
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
        MessageVo<Cart> messageVo=new MessageVo();
        messageVo.setOrder("UPDATE");
        messageVo.setData(cartItem);
        //2.更新redis,商品数量符号表示是否选中，绝对值为购物车中商品真实数量,添加时默认为选中状态
        CartKey cartKey=new CartKey(userId);
        Map<String,String> map=new HashMap<>();
        map.put(String.valueOf(productId), String.valueOf(count));
        stringRedisTemplate.opsForHash().putAll(cartKey.getPrefix(),map );
        //3.异步添加到购物车数据库
        rabbitTemplate.convertAndSend(Constants.CART_EXCHANGE,
                "cartitem.update" ,
                JsonUtil.obj2String(messageVo));
        rabbitTemplate.setConfirmCallback(new RabbitTemplate.ConfirmCallback() {
            @Override
            public void confirm(CorrelationData correlationData, boolean ack, String cause) {
                log.info(correlationData.toString() );
            }
        });
        return ServerResponse.createBySuccess();
    }


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
        MessageVo<CartUserProductVo> messageVo=new MessageVo();
        messageVo.setOrder("DELETE");
        messageVo.setData(cartUserProductVo);
        rabbitTemplate.convertAndSend(Constants.CART_EXCHANGE,"cartitem.delete" , JsonUtil.obj2String(messageVo));
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
     * @param checkStatus
     * @param productId
     * @return
     */
    @Override
    public ServerResponse selectOrUnSelect(Integer userId, int checkStatus, Integer productId) {
        CartKey cartKey=new CartKey(userId);
        Cart cart=new Cart();
        cart.setUserId(userId);
        cart.setProductId(productId);
        cart.setChecked(checkStatus);
        if (stringRedisTemplate.hasKey(cartKey.getPrefix())){
            //更新redis，选中商品数量为正，未选中为负
            String productQuantityStr=(String) stringRedisTemplate.opsForHash().get(cartKey.getPrefix(),String.valueOf(productId) );
            Integer productQuantityInt=Math.abs(Integer.valueOf(productQuantityStr));
            String newProductQuantityStr=checkStatus==Constants.Cart.CHECKED?String.valueOf(productQuantityInt):String.valueOf(0-productQuantityInt);
            stringRedisTemplate.opsForHash().put(cartKey.getPrefix(), String.valueOf(productId), newProductQuantityStr);
            MessageVo messageVo=new MessageVo();
            messageVo.setOrder("UPDATE");
            messageVo.setData(cart);
            rabbitTemplate.convertAndSend(Constants.CART_EXCHANGE,"cartitem.update" , JsonUtil.obj2String(messageVo) );
        }else {
            cartMapper.updateByPrimaryKeySelective(cart);
            List<Cart> cartList=cartMapper.selectCartByUserId(userId);
            Map<String,String> map=new HashMap<>();
            for (Cart newCart:cartList){
                if (newCart.getChecked()==Constants.Cart.CHECKED){
                   map.put(String.valueOf(productId) ,String.valueOf(newCart.getQuantity()) );
                }else {
                    map.put(String.valueOf(productId) ,String.valueOf(0-newCart.getQuantity()) );
                }
            }
            stringRedisTemplate.opsForHash().putAll(cartKey.getPrefix(),map );
        }
        return ServerResponse.createBySuccess();
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
        rabbitTemplate.convertAndSend(Constants.CART_EXCHANGE,"cartitems.remove" , JsonUtil.obj2String(messageVo) );
        return ServerResponse.createBySuccess();
    }

    /**
     * 删除cart数据库用户购物车已选中记录
     * @param userId
     * @return
     */
    @Override
    public ServerResponse removeCartDB(String userId) {
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
     * 下单时获取的购物车信息,滤除未选中的
     * @param userId
     * @return
     */
    @Override
    public ServerResponse getCartVo(Integer userId) {
        ServerResponse response=list(userId);
        if (!response.isSuccess()){
            return response;
        }
//        List<Cart> cartList=(List<Cart>) response.getData();
        List<Cart>cartList=JsonUtil.Str2Obj(response.getData().toString(),List.class ,Cart.class);
        List<CartProductVo> cartProductVoList=new LinkedList<>();
        CartVo cartVo=new CartVo();
        for (Cart cart:cartList){
            if (cart.getChecked()==Constants.Cart.CHECKED){
                CartProductVo cartProductVo=new CartProductVo();
                BeanUtils.copyProperties(cart,cartProductVo );
                cartProductVoList.add(cartProductVo);
            }
        }
        cartVo.setCartProductVoList(cartProductVoList);
        return ServerResponse.createBySuccess(cartVo);
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

}
