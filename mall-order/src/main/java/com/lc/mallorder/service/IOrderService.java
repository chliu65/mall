package com.lc.mallorder.service;


import com.github.pagehelper.PageInfo;
import com.lc.mallorder.common.resp.ServerResponse;
import com.lc.mallorder.vo.MessageVo;
import com.lc.mallorder.vo.OrderVo;

import java.util.List;
import java.util.Map;


public interface IOrderService {
    /***后台订单管理 start***/
    /***获取订单列表，分页***/
    ServerResponse<PageInfo> manageList(int pageNum, int pageSize);

    /***获取订单详情***/
    ServerResponse<OrderVo> manageDetail(Long orderNo);

    /***获取订单***/
    ServerResponse<PageInfo> manageSearch(Long orderNo, int pageNum, int pageSize);

    /***发货***/
    ServerResponse<String> manageSendGoods(Long orderNo);

    /***門戶订单管理 start***/
    /**创建订单**/
    ServerResponse createOrder(Integer userId, Integer shippingId);

    /**取消订单**/
    ServerResponse cancel(Integer userId, Long orderNo);

//    /**获取订单商品详情**/
//    ServerResponse getOrderCartProduct(Integer userId);

    /**订单详情**/
    ServerResponse getOrderDetail(Integer userId, Long orderNo);

    /**用户订单列表**/
    ServerResponse getOrderList(Integer userId, int pageNum, int pageSize);


    /**扣减库存、下订单**/
    ServerResponse stockAndOrderprocess(MessageVo result);

//    ServerResponse pay(Integer id, Long orderNo, String path);

    ServerResponse query_order_pay_status(Integer id, Long orderNo);

//    ServerResponse aliCallback(Map<String, String> params);
}
