package com.lc.mallorder.vo;

import lombok.Data;


@Data
public class MessageVo {
    private Integer userId;
    private Integer shippingId;
    private CartVo cartVo;
    private long orderNo;
}
