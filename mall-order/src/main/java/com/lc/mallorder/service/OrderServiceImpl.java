package com.lc.mallorder.service;


import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.lc.mallorder.clients.*;
import com.lc.mallorder.common.constants.Constants;
import com.lc.mallorder.common.exception.GlobalException;
import com.lc.mallorder.common.keys.*;
import com.lc.mallorder.common.resp.ResponseEnum;
import com.lc.mallorder.common.resp.ServerResponse;

import com.lc.mallorder.common.timer.Timer;
import com.lc.mallorder.common.timer.TimerTask;
import com.lc.mallorder.common.utils.JsonUtil;
import com.lc.mallorder.common.utils.RedisUtils;
import com.lc.mallorder.dao.OrderItemMapper;
import com.lc.mallorder.dao.OrderMapper;
import com.lc.mallorder.entity.Order;
import com.lc.mallorder.entity.OrderItem;
import com.lc.mallorder.entity.Product;
import com.lc.mallorder.entity.Shipping;
import com.lc.mallorder.vo.*;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;


@Service
@Slf4j
public class OrderServiceImpl implements OrderService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private ShippingClient shippingClient;
    @Autowired
    private CartClient cartClient;
    @Autowired
    private ProductClient productClient;
    @Autowired
    private RedisUtils redisUtils;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private UniqueIdClient uniqueIdClient;
    @Autowired
    private PaymentClient paymentClient;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private Timer timer;

    /*** 后台订单管理 start***/

    @Override
    public ServerResponse<PageInfo> manageList(int pageNum, int pageSize) {
        PageHelper.startPage(pageNum,pageSize);
        List<Order> orderList = orderMapper.selectAllOrder();
        List<OrderVo> orderVoList = this.assembleOrderVoList(orderList,null);
        PageInfo pageResult = new PageInfo(orderList);
        pageResult.setList(orderVoList);
        return ServerResponse.createBySuccess(pageResult);
    }

    @Override
    public ServerResponse<OrderVo> manageDetail(Long orderNo) {
        Order order = orderMapper.selectByOrderNo(orderNo);
        if(order != null){
            List<OrderItem> orderItemList = orderItemMapper.getByOrderNo(orderNo);
            OrderVo orderVo = assembleOrderVo(order,orderItemList);
            return ServerResponse.createBySuccess(orderVo);
        }
        return ServerResponse.createByErrorMessage("订单不存在");
    }

    @Override
    public ServerResponse<PageInfo> manageSearch(Long orderNo, int pageNum, int pageSize) {
        PageHelper.startPage(pageNum,pageSize);
        Order order = orderMapper.selectByOrderNo(orderNo);
        if(order != null){
            List<OrderItem> orderItemList = orderItemMapper.getByOrderNo(orderNo);
            OrderVo orderVo = assembleOrderVo(order,orderItemList);

            PageInfo pageInfo = new PageInfo(Lists.newArrayList(order));
            pageInfo.setList(Lists.newArrayList(orderVo));
            return ServerResponse.createBySuccess(pageInfo);
        }
        return ServerResponse.createByErrorMessage("订单不存在");
    }

    @Override
    public ServerResponse<String> manageSendGoods(Long orderNo) {
        Order order = orderMapper.selectByOrderNo(orderNo);
        if(order != null){
            if(order.getStatus() == Constants.OrderStatusEnum.PAID.getCode()){
                order.setStatus(Constants.OrderStatusEnum.SHIPPED.getCode());
                order.setSendTime(new Date());
                orderMapper.updateByPrimaryKeySelective(order);
                return ServerResponse.createBySuccessMessage("发货成功");
            }
            return ServerResponse.createByErrorMessage("发货失败");
        }
        return ServerResponse.createByErrorMessage("订单不存在");
    }

    private OrderVo assembleOrderVo(Order order,List<OrderItem> orderItemList){
        OrderVo orderVo = new OrderVo();
        orderVo.setOrderNo(order.getOrderNo());
        orderVo.setPayment(order.getPayment());
        orderVo.setPaymentType(order.getPaymentType());
        orderVo.setPaymentTypeDesc(Constants.PaymentTypeEnum.codeOf(order.getPaymentType()).getValue());

        orderVo.setPostage(order.getPostage());
        orderVo.setStatus(order.getStatus());
        orderVo.setStatusDesc(Constants.OrderStatusEnum.codeOf(order.getStatus()).getValue());

        orderVo.setShippingId(order.getShippingId());

        Shipping shipping = null;
        ServerResponse response = shippingClient.getShipping(order.getShippingId());
        if(response.getCode() == ResponseEnum.LOGIN_EXPIRED.getCode()){
            throw new GlobalException(ResponseEnum.LOGIN_EXPIRED);
        }
        if(!response.isSuccess()){
            throw new GlobalException("获取地址出现错误!");
        }else {
            Object object = response.getData();
            String objStr = JsonUtil.obj2String(object);
            shipping = JsonUtil.Str2Obj(objStr,Shipping.class);
        }

        if(shipping != null){
            orderVo.setReceiverName(shipping.getReceiverName());
            orderVo.setShippingVo(assembleShippingVo(shipping));
        }else{
            throw new GlobalException("地址没有获取成功");
        }

        orderVo.setPaymentTime(String.valueOf(order.getPaymentTime()));
        orderVo.setSendTime(String.valueOf(order.getSendTime()));
        orderVo.setEndTime(String.valueOf(order.getEndTime()));
        orderVo.setCreateTime(String.valueOf(order.getCreateTime()));
        orderVo.setCloseTime(String.valueOf(order.getCloseTime()));


        orderVo.setImageHost("http://img.oursnail.cn/");


        List<OrderItemVo> orderItemVoList = Lists.newArrayList();

        for(OrderItem orderItem : orderItemList){
            OrderItemVo orderItemVo = assembleOrderItemVo(orderItem);
            orderItemVoList.add(orderItemVo);
        }
        orderVo.setOrderItemVoList(orderItemVoList);
        return orderVo;
    }

    private OrderItemVo assembleOrderItemVo(OrderItem orderItem){
        OrderItemVo orderItemVo = new OrderItemVo();
        BeanUtils.copyProperties(orderItem,orderItemVo);
        orderItemVo.setCreateTime(orderItem.getCreateTime().toString());
        return orderItemVo;
    }

    private ShippingVo assembleShippingVo(Shipping shipping){
        ShippingVo shippingVo = new ShippingVo();
        BeanUtils.copyProperties(shipping,shippingVo );
        return shippingVo;
    }

    private List<OrderVo> assembleOrderVoList(List<Order> orderList,Integer userId){
        List<OrderVo> orderVoList = Lists.newArrayList();
        for(Order order : orderList){
            List<OrderItemVo>  orderItemVoList = Lists.newArrayList();
            List<OrderItem>  orderItemList = Lists.newArrayList();
            if(userId == null){
                orderItemList = orderItemMapper.getByOrderNo(order.getOrderNo());
            }else{
                orderItemList = orderItemMapper.getByOrderNoUserId(order.getOrderNo(),userId);
            }
            //OrderVo orderVo = assembleOrderVo(order,orderItemList);
            for (OrderItem orderItem:orderItemList){
                OrderItemVo orderItemVo=new OrderItemVo();
                BeanUtils.copyProperties(orderItem ,orderItemVo);
                orderItemVoList.add(orderItemVo);
            }
            OrderVo orderVo=new OrderVo();
            BeanUtils.copyProperties(order,orderVo );
            orderVo.setOrderItemVoList(orderItemVoList);
            orderVoList.add(orderVo);
        }
        return orderVoList;
    }

    /**
     * 创建订单
     */
    @Override
    public ServerResponse createOrder(Integer userId, Integer shippingId) {

        log.info("【开始创建订单，userId为：{}，shippingID为：{}】",userId,shippingId);
        //1.获取购物车
        //1.1 cart是存在redis中的
        CartKey cartKey=new CartKey(userId);
        List<String> productStockKeyStringList=new LinkedList<>();
        List<String> cartProductQuantityList=new LinkedList<>();
        Map<String,String> map=(Map) stringRedisTemplate.opsForHash().entries(cartKey.getPrefix());
        CartVo cartVo=new CartVo();
        if (map.isEmpty()){
         ServerResponse response=cartClient.getCartList(String.valueOf(userId));
         if (!response.isSuccess()){
             throw new GlobalException(response.getMsg());
         }
         String cartVoStr=JsonUtil.obj2String(response.getData().toString());
         cartVo=JsonUtil.Str2Obj(cartVoStr,CartVo.class );
         for (CartProductVo cartProductVo:cartVo.getCartProductVoList()){
             ProductStockKey productStockKey=new ProductStockKey(cartProductVo.getProductId());
             productStockKeyStringList.add(productStockKey.getPrefix());
             cartProductQuantityList.add(String.valueOf(cartProductVo.getQuantity()));
         }
        }else {
            List<CartProductVo> cartProductVoList=new LinkedList<>();
            for (String productId:map.keySet()){
                String productQuantity=map.get(productId);
                Integer productQuantityInt=Integer.valueOf(productQuantity);
                //小于0表示未选中,大于0表示选中
                if (productQuantityInt>0){
                    CartProductVo cartProductVo=new CartProductVo();
                    cartProductVo.setProductId(Integer.valueOf(productId));
                    cartProductVo.setQuantity(productQuantityInt);
                    cartProductVoList.add(cartProductVo);
                    ProductStockKey productStockKey=new ProductStockKey(productId);
                    productStockKeyStringList.add(productStockKey.getPrefix());
                    cartProductQuantityList.add(productQuantity);
                }
            }
            cartVo.setCartProductVoList(cartProductVoList);
        }
        if (cartVo.getCartProductVoList().size()==0){
            throw new GlobalException("购物车为空");
        }
        OrderUserCartVo orderUserCartVo=new OrderUserCartVo();
        orderUserCartVo.setUserId(userId);
        orderUserCartVo.setShippingId(shippingId);
        //这里生成订单号，方便查询
        String orderNoStr = uniqueIdClient.getUniqueId();
        orderNoStr = orderNoStr.substring(0,orderNoStr.length()-3);
        long orderNo = Long.parseLong(orderNoStr);
        log.info("【生成的订单号为:{}】",orderNo);
        orderUserCartVo.setOrderNo(orderNo);
        orderUserCartVo.setCartVo(cartVo);
        //3.对所有选中商品预减库存，原子操作，有库存不足或redis中不存在的，不做修改直接返回，
        String batchReduceStockResult=redisUtils.batchChangeStock(productStockKeyStringList,cartProductQuantityList );
        if(!batchReduceStockResult.equals("success")){
            log.info("【商品{}不存在或者库存不够】",batchReduceStockResult);
            return ServerResponse.createByErrorMessage("商品不存在或者库存不够");
        }
        MessageVo messageVo=new MessageVo();
        messageVo.setData(orderUserCartVo);
        messageVo.setOrder("creatOrder");
        //4. 扣减库存成功、清空redis中的购物车已选中商品，生成订单,参数userId,ShippingId传给MQ取异步下单，以及异步删除数据库购物车
        for (CartProductVo cartProductVo:cartVo.getCartProductVoList()){
            String productId=String.valueOf(cartProductVo.getProductId());
            stringRedisTemplate.opsForHash().delete(cartKey.getPrefix(),productId);
        }
        stringRedisTemplate.delete(cartKey.getPrefix());
        rabbitTemplate.convertAndSend("order-exchange","order.update" , JsonUtil.obj2String(messageVo));

        //应该是先显示排队中，所以直接就取数据库查询订单信息返回给前端）

        log.info("【返回给前端的orderNo：{}】",orderNo);
        OrderVo orderVo = new OrderVo();
        orderVo.setOrderNo(orderNo);
        log.info("【返回给前端的数据为：{}】",orderVo);
        return ServerResponse.createBySuccess(orderVo);
    }

    private OrderVo assembleResultOrderVo(long orderNo) {
        Order order = orderMapper.selectByOrderNo(orderNo);
        List<OrderItem> orderItemList = orderItemMapper.getByOrderNo(orderNo);
        return assembleOrderVo(order,orderItemList);
    }

    /**
     * 取消订单
     * @param userId
     * @param orderNo
     * @return
     */
    @Override
    public ServerResponse cancel(Integer userId, Long orderNo) {
        OrderKey orderStatusKey=new OrderKey(OrderKey.ORDER_STATUS,orderNo);
        String orderStatusStr=stringRedisTemplate.opsForValue().get(orderStatusKey.getPrefix());
        if (StringUtils.isBlank(orderStatusStr)){
            //redis为空，订单不可取消
            return ServerResponse.createByErrorMessage("订单不可取消");
        }else {
            if (Integer.valueOf(orderStatusStr).equals(Constants.OrderStatusEnum.NO_PAY.getCode())){
                stringRedisTemplate.delete(orderStatusKey.getPrefix());
                //amqpTemplate.convertAndSend(, , );
                return ServerResponse.createBySuccess();
            }else {
                return ServerResponse.createByErrorMessage("正在支付，不可取消");
            }
        }
    }


    public ServerResponse cancalOrderDB(Integer userId, Long orderNo){
        Order order=orderMapper.selectByOrderNo(orderNo);
        if (userId.equals(order.getUserId())){
            order.setStatus(Constants.OrderStatusEnum.CANCELED.getCode());
            orderMapper.updateByPrimaryKey(order);
        }else {
            throw new GlobalException("订单取消异常");
        }
        return ServerResponse.createBySuccess();
    }


    @Override
    public ServerResponse getOrderDetail(Integer userId, Long orderNo) {
        Order order = orderMapper.selectByUserIdOrderNo(userId,orderNo);
        if(order == null){
            log.error("【用户{}不存在{}订单】",userId,orderNo);
            return ServerResponse.createByErrorMessage("订单不存在");
        }
        List<OrderItem> orderItemList = orderItemMapper.getByOrderNoUserId(orderNo,userId);
        log.info("获取订单详情列表成功{}",orderItemList);
        OrderVo orderVo = this.assembleOrderVo(order,orderItemList);
        return ServerResponse.createBySuccess(orderVo);
    }

    @Override
    public ServerResponse getOrderList(Integer userId, int pageNum, int pageSize) {
        PageHelper.startPage(pageNum,pageSize);
        List<Order> orderList = orderMapper.selectByUserId(userId);
        List<OrderVo> orderVoList = this.assembleOrderVoList(orderList,userId);
        PageInfo pageResult = new PageInfo(orderList);
        pageResult.setList(orderVoList);
        return ServerResponse.createBySuccess(pageResult);
    }

    /**
     * 生成订单主附表
     * @param orderUserCartVo
     * @return
     */
    @Transactional
    public ServerResponse createOrderProcess(OrderUserCartVo orderUserCartVo){
        //0.获取userId和shippingId
        log.info("【生成订单主表和订单详情表】");
        Integer userId = orderUserCartVo.getUserId();
        Integer shippingId = orderUserCartVo.getShippingId();
        long orderNo = orderUserCartVo.getOrderNo();
        log.info("【获取userId:{}和shippingId:{}，orderNo:{}】",userId,shippingId,orderNo);

        //1.获取购物车列表
        CartVo cartVo = orderUserCartVo.getCartVo();
        log.info("【获取购物车列表:{}】",cartVo);

        List<CartProductVo> cartProductVoList = cartVo.getCartProductVoList();
        //2.根据购物车构建订单详情
        ServerResponse response = getCartOrderItem(userId,cartProductVoList,orderNo);
        if(!response.isSuccess()){
            return response;
        }
       List<OrderItem> orderItemList = (List<OrderItem>) response.getData();
//        List<OrderItem> orderItemList =(List<OrderItem>) JsonUtil.Str2Obj(response.getData(),List.class ,OrderItem.class);
        if(CollectionUtils.isEmpty(orderItemList)){
            log.error("【购物车为空】");
            return ServerResponse.createByErrorMessage("购物车为空");
        }
        //3.计算总价
        BigDecimal payment = getOrderTotalPrice(orderItemList);
        payment=payment.multiply(new BigDecimal(100));//单位为分
        log.info("【计算总价为：{}】",payment);
        //4.构建订单主表
        Order order = assembleOrder(userId,shippingId,payment,orderNo);
        if(order == null){
            log.error("【生成订单主表失败】");
            return ServerResponse.createByErrorMessage("生成订单失败");
        }
//        for(OrderItem orderItem:orderItemList){
//            orderItem.setOrderNo(orderNo);
//        }
        //4.批量插入订单详情
        orderItemMapper.batchInsert(orderItemList);
        orderMapper.insert(order);
        //5.订单花费缓存入redis,方便支付
        OrderKey orderCostKey=new OrderKey(OrderKey.ORDER_COST,orderNo);
        stringRedisTemplate.opsForValue().set(orderCostKey.getPrefix(), String.valueOf(payment),Constants.RedisCacheExtime.ORDER_STATUS_KEY_EXPIRES,TimeUnit.SECONDS);
        return ServerResponse.createBySuccess("扣减库存、生成订单成功...",userId);
    }

    @Override
    public ServerResponse stockAndOrderprocess(OrderUserCartVo orderUserCartVo) {
        log.info("【开始消费，参数未{}】",orderUserCartVo);
        ServerResponse response = this.createOrderProcess(orderUserCartVo);
        log.info("订单生成完毕，下面就是去清空购物车，MQ异步出去");
        MessageVo messageVo=new MessageVo();
        messageVo.setOrder("REMOVE");
        messageVo.setData(String.valueOf(orderUserCartVo.getUserId()));
        if(response.isSuccess()){
            log.info("【需要清空购物车的用户为:{}】",orderUserCartVo.getUserId());
            rabbitTemplate.convertAndSend("cart-exchange","cartitems.remove",JsonUtil.obj2String(messageVo));
        }
        return ServerResponse.createBySuccessMessage("扣减库存、生成订单成功");
    }

    /**
     * 生成订单主表
     * @param userId
     * @param shippingId
     * @param payment
     * @param orderNo
     * @return
     */
    private Order assembleOrder(Integer userId,Integer shippingId,BigDecimal payment,long orderNo){
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setStatus(Constants.OrderStatusEnum.NO_PAY.getCode());
        order.setPostage(0);
        order.setPaymentType(Constants.PaymentTypeEnum.ONLINE_PAY.getCode());
        order.setPayment(payment);

        order.setUserId(userId);
        order.setShippingId(shippingId);
        //发货时间等等
        //付款时间等等
        order.setCreateTime(new Date());
        order.setUpdateTime(new Date());
        return order;
    }

    /**
     * 计算订单总价
     * @param orderItemList
     * @return
     */
    private BigDecimal getOrderTotalPrice(List<OrderItem> orderItemList) {
        BigDecimal payment = new BigDecimal("0");
        for(OrderItem orderItem:orderItemList){
            payment = payment.add(orderItem.getTotalPrice());
        }
        return payment;
    }

    /**
     * 构建订单详情
     * @param userId
     * @param cartProductVoList
     * @param orderNo
     * @return
     */
    private ServerResponse getCartOrderItem(Integer userId, List<CartProductVo> cartProductVoList,Long orderNo) {
        if(CollectionUtils.isEmpty(cartProductVoList)){
            return ServerResponse.createByErrorMessage("购物车选中商品为空");
        }
        List<OrderItem> orderItemList = Lists.newArrayList();

        OrderKey orderProductKey=new OrderKey(OrderKey.ORDER_ITEM,orderNo);
        Map<String,String> map=new HashMap<>();
        //查询商品具体信息，并设置orderProductKey
        for(CartProductVo cartProductVo:cartProductVoList){
            OrderItem orderItem = new OrderItem();
            ProductKey productKey=new ProductKey(cartProductVo.getProductId());
            String productStr = stringRedisTemplate.opsForValue().get(productKey.getPrefix());
            ProductInfoVo productInfoVo=new ProductInfoVo();
            if(StringUtils.isBlank(productStr)){
                ServerResponse response = productClient.queryProduct(cartProductVo.getProductId());
                if (!response.isSuccess()){
                    throw new GlobalException(response.getMsg());
                }
                Object object = response.getData();
                String objStr = JsonUtil.obj2String(object);
                productInfoVo = (ProductInfoVo) JsonUtil.Str2Obj(objStr,ProductInfoVo.class);
            }else {
                productInfoVo = (ProductInfoVo) JsonUtil.Str2Obj(productStr,ProductInfoVo.class);
            }
            BeanUtils.copyProperties(productInfoVo,orderItem );
            orderItem.setCurrentUnitPrice(productInfoVo.getPrice());
            orderItem.setUserId(userId);
            orderItem.setTotalPrice(productInfoVo.getPrice().multiply(new BigDecimal(cartProductVo.getQuantity())));
            orderItem.setOrderNo(orderNo);
            orderItem.setId(null);
            orderItemList.add(orderItem);
            map.put(String.valueOf(cartProductVo.getProductId()),String.valueOf(cartProductVo.getQuantity() ));
        }
        //订单详情缓存入redis，方便定案取消回滚redis
        stringRedisTemplate.opsForHash().putAll(orderProductKey.getPrefix(), map);
        return ServerResponse.createBySuccess(orderItemList);
    }

    @Override
    public ServerResponse queryOrderStatus(Long orderNo) {
        OrderKey orderKey=new OrderKey(OrderKey.ORDER_STATUS,orderNo);
        if (stringRedisTemplate.hasKey(orderKey.getPrefix())){
            String orderStatus=stringRedisTemplate.opsForValue().get(orderKey.getPrefix());
            return ServerResponse.createBySuccess(orderStatus);
        }else {
            Order order=orderMapper.selectByOrderNo(orderNo);
            return ServerResponse.createBySuccess(order.getStatus());
        }
    }

    /**
     * 支付完成，商品数据库扣库存
     * @param orderNo
     * @return
     */
    //需要确保不会重复完成订单工作
    //分布式事务处理
    @Transactional
    @Override
    public ServerResponse finishOrder(String orderNo) {
        Order order=orderMapper.selectByOrderNo(Long.valueOf(orderNo));
        if (order.getStatus()==Constants.OrderStatusEnum.PAID.getCode()){
            throw new GlobalException("订单已完成，勿重复提交");
        }
        order.setUpdateTime(new Date());
        order.setStatus(Constants.OrderStatusEnum.PAID.getCode());
        List<OrderItem> orderItemList=orderItemMapper.getByOrderNo(Long.valueOf(orderNo));
        List<StockReduceVo> stockReduceVoList=new LinkedList<>();
        for (OrderItem orderItem:orderItemList){
            StockReduceVo stockReduceVo=new StockReduceVo(String.valueOf( orderItem.getProductId()),String.valueOf(orderItem.getQuantity()));
            stockReduceVoList.add(stockReduceVo);
        }
        if (stockReduceVoList.size()>0){
            ServerResponse serverResponse=productClient.reduceStock(stockReduceVoList);
            if (serverResponse.isSuccess()){
                orderMapper.updateByPrimaryKey(order);
                //支付成功，订单从redis中移除
                OrderKey orderKey=new OrderKey(OrderKey.ORDER_STATUS,orderNo);
                CartKey cartKey=new CartKey(orderNo);
                stringRedisTemplate.delete(orderKey.getPrefix());
                stringRedisTemplate.delete(cartKey.getPrefix());
//                timer.deleteTask();
                return serverResponse;
            }
        }
        throw new GlobalException(ResponseEnum.SERVER_ERROR);
    }

    /**
     * 请求支付
     * @param id
     * @param orderNo
     * @return
     */
    @Override
    public ServerResponse requestPayment(Integer id, Long orderNo) {
        //1.查询redis中订单状态
        OrderKey orderStatus=new OrderKey(OrderKey.ORDER_STATUS,orderNo);
        OrderKey orderCost=new OrderKey(OrderKey.ORDER_COST,orderNo);
        String orderStatusStr=stringRedisTemplate.opsForValue().get(orderStatus.getPrefix());
        String orderCostStr=stringRedisTemplate.opsForValue().get(orderCost.getPrefix());
        //2.如果订单未过期，则同意支付请求，并设置订单状态为正在支付
        if (StringUtils.isBlank(orderStatusStr)){
            //首次支付
            OrderKey orderProductKey =new OrderKey(OrderKey.ORDER_ITEM,orderNo);
            if (stringRedisTemplate.hasKey(orderProductKey.getPrefix())){
                ServerResponse serverResponse=paymentClient.prePayment(String.valueOf(orderNo), orderCostStr);
                return serverResponse;
            }
        }else {
            //非首次支付且订单待支付
            if (Integer.valueOf(orderStatusStr)==Constants.OrderStatusEnum.NO_PAY.getCode()){
                stringRedisTemplate.opsForValue().set(orderStatus.getPrefix(),String.valueOf(Constants.OrderStatusEnum.PAYING.getCode() ));
                ServerResponse serverResponse=paymentClient.prePayment(String.valueOf(orderNo),orderCostStr );
                return serverResponse;
            }
        }
        return ServerResponse.createByErrorMessage("订单已关闭");
    }
/*
    @Override
    public ServerResponse aliCallback(Map<String, String> params) {
        Long orderNo = Long.parseLong(params.get("out_trade_no"));
        String tradeNo = params.get("trade_no");
        String tradeStatus = params.get("trade_status");
        Order order = orderMapper.selectByOrderNo(orderNo);
        if(order == null){
            return ServerResponse.createByErrorMessage("非快乐蜗牛商城的订单,回调忽略");
        }
        //判断订单的状态是否大于已支付的状态，如果大于，说明是支付宝的重复通知，直接返回success即可
        if(order.getStatus() >= Constants.OrderStatusEnum.PAID.getCode()){
            return ServerResponse.createBySuccess("支付宝重复调用");
        }
        //根据支付宝的回调状态来更新订单的状态
        if(Constants.AlipayCallback.TRADE_STATUS_TRADE_SUCCESS.equals(tradeStatus)){
            order.setPaymentTime(DateTimeUtil.strToDate(params.get("gmt_payment")));
            order.setStatus(Constants.OrderStatusEnum.PAID.getCode());
            orderMapper.updateByPrimaryKeySelective(order);
        }
        //插入支付信息
        PayInfo payInfo = new PayInfo();
        payInfo.setUserId(order.getUserId());
        payInfo.setOrderNo(order.getOrderNo());
        payInfo.setPayPlatform(Constants.PayPlatformEnum.ALIPAY.getCode());
        payInfo.setPlatformNumber(tradeNo);
        payInfo.setPlatformStatus(tradeStatus);
        payInfoMapper.insert(payInfo);
        return ServerResponse.createBySuccess();
    }
*/
}
