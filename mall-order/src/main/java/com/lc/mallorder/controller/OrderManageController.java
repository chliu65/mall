package com.lc.mallorder.controller;

import com.github.pagehelper.PageInfo;
import com.lc.mallorder.common.resp.ServerResponse;
import com.lc.mallorder.service.OrderService;

import com.lc.mallorder.vo.OrderVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/manage/order")
public class OrderManageController {

    @Autowired
    private OrderService orderService;

    @RequestMapping("list.do")
    public ServerResponse<PageInfo> orderList(@RequestParam(value = "pageNum",defaultValue = "1") int pageNum,
                                              @RequestParam(value = "pageSize",defaultValue = "10") int pageSize){
        return orderService.manageList(pageNum,pageSize);
    }

    @RequestMapping("detail.do")
    public ServerResponse<OrderVo> orderDetail(Long orderNo){
        return orderService.manageDetail(orderNo);
    }


    @RequestMapping("search.do")
    public ServerResponse<PageInfo> orderSearch(Long orderNo,@RequestParam(value = "pageNum",defaultValue = "1") int pageNum,
                                                @RequestParam(value = "pageSize",defaultValue = "10")int pageSize){
        return orderService.manageSearch(orderNo,pageNum,pageSize);
    }


    @RequestMapping("send_goods.do")
    public ServerResponse<String> orderSendGoods(Long orderNo){
        return orderService.manageSendGoods(orderNo);
    }
}
