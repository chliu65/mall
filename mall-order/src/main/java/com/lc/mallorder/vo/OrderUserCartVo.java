package com.lc.mallorder.vo;

import lombok.Data;


@Data
public class OrderUserCartVo {
    private Integer userId;
    private Integer shippingId;
    private CartVo cartVo;
    private Long orderNo;
}
