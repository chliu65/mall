package com.lc.mallorder.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * @Author swg.
 * @Date 2019/1/8 10:20
 * @CONTACT 317758022@qq.com
 * @DESC
 */
@Data
public class OrderProductVo {
    private List<OrderItemVo> orderItemVoList;
    private BigDecimal productTotalPrice;
    private String imageHost;
}