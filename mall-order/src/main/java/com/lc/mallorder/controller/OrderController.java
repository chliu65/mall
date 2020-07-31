package com.lc.mallorder.controller;



import com.lc.mallorder.common.keys.OrderKey;
import com.lc.mallorder.common.keys.UserKey;
import com.lc.mallorder.common.resp.ServerResponse;
import com.lc.mallorder.common.utils.CookieUtil;
import com.lc.mallorder.common.utils.JsonUtil;
import com.lc.mallorder.entity.User;
import com.lc.mallorder.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;


@RestController
@RequestMapping("/order/")
@Slf4j
public class OrderController {
    @Autowired
    private OrderService orderService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    public User getUser(HttpServletRequest request){
        String token = CookieUtil.readLoginToken(request);
        UserKey userKey=new UserKey(token);
        String  userStr=stringRedisTemplate.opsForValue().get(userKey.getPrefix());
        return JsonUtil.Str2Obj(userStr,User.class );
    }
    /**
     * 创建订单
     */
    @RequestMapping("create.do")
    public ServerResponse create(HttpServletRequest request, @RequestParam("shippingId") Integer shippingId){
        User user=getUser(request);
        ServerResponse response = orderService.createOrder(user.getId(),shippingId);
        if(response.isSuccess()) {
            log.info("【生成订单成功:{}】",response.getData());
            return response;
        }
        log.error("【提交订单失败:{}】",response);
        return ServerResponse.createByErrorMessage("提交订单失败:"+response.getMsg());
    }

    /**
     * 取消订单
     */
    @RequestMapping("cancel.do")
    public ServerResponse cancel(HttpServletRequest request, @RequestParam("orderNo") Long orderNo){
        User user=getUser(request);
        return orderService.cancel(user.getId(),orderNo);
    }

    /**
     * 请求支付
     * @param request
     * @param orderNo
     * @return
     */
    @RequestMapping("finish.do")
    public ServerResponse finish(HttpServletRequest request, @RequestParam("orderNo") Long orderNo){
        User user=getUser(request);
        return orderService.requestPayment(user.getId(),orderNo );
    }

    /**
     * 获取订单商品信息
     */
    @RequestMapping("get_order_status.do")
    public ServerResponse queryOrderStatus(HttpServletRequest request,@RequestParam("orderNo") Long orderNo){
        User user=getUser(request);
        return orderService.queryOrderStatus(orderNo );
    }

    /**
     * 订单详情
     */
    @RequestMapping("detail.do")
    public ServerResponse detail(HttpServletRequest request,@RequestParam("orderNo") Long orderNo){
        User user=getUser(request);
        return orderService.getOrderDetail(user.getId(),orderNo);
    }

    /**
     * 订单列表
     */
    @RequestMapping("list.do")
    public ServerResponse list(HttpServletRequest request, @RequestParam(value = "pageNum",defaultValue = "1") int pageNum, @RequestParam(value = "pageSize",defaultValue = "10") int pageSize){
        User user=getUser(request);
        return orderService.getOrderList(user.getId(),pageNum,pageSize);
    }

}
