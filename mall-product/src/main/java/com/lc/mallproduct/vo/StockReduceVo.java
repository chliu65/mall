package com.lc.mallproduct.vo;

import lombok.Data;

@Data
public class StockReduceVo {
    Integer productId;
    Integer reduceNum;

    public StockReduceVo(Integer productId, Integer reduceNum) {
        this.productId = productId;
        this.reduceNum= reduceNum;
    }
}
