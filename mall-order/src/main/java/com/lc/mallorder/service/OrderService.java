package com.lc.mallorder.service;


import com.github.pagehelper.PageInfo;
import com.lc.mallorder.common.resp.ServerResponse;
import com.lc.mallorder.vo.MessageVo;
import com.lc.mallorder.vo.OrderUserCartVo;
import com.lc.mallorder.vo.OrderVo;

import java.util.List;
import java.util.Map;


public interface OrderService {
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

//    /**
//    ServerResponse getOrderCartProduct(Integer userId);

    /**
     * 订单详情
     * @param userId
     * @param orderNo
     * @return
     */
    ServerResponse getOrderDetail(Integer userId, Long orderNo);

    /**
     * 用户订单列表
     * @param userId
     * @param pageNum
     * @param pageSize
     * @return
     */
    ServerResponse getOrderList(Integer userId, int pageNum, int pageSize);


    /**
     * 扣减库存(redis预减库存)、下订单*
     * @param orderUserCartVo
     * @return
     */
    ServerResponse stockAndOrderprocess(OrderUserCartVo orderUserCartVo);

//    ServerResponse pay(Integer id, Long orderNo, String path);

    /**
     * 查询订单状态
     * @param orderNo
     * @return
     */
    ServerResponse queryOrderStatus(Long orderNo);

    /**
     * 支付成功，完成下单，数据库扣库存
     * @param orderNo
     * @return
     */
    ServerResponse finishOrder(String orderNo);

    /**
     * 用户再次请求支付订单
     * @param id
     * @param orderNo
     * @return
     */

    ServerResponse requestPayment(Integer id, Long orderNo);



//    ServerResponse aliCallback(Map<String, String> params);
}
