package com.lc.mallorder.vo;

import lombok.Data;

@Data
public class StockReduceVo {
    String productId;
    String reduceNum;

    public StockReduceVo(String productId, String reduceNum) {
        this.productId = productId;
        this.reduceNum= reduceNum;
    }
}
