package com.lc.mallproduct.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class StockReduceVo {
    Integer productId;
    Integer reduceNum;
}
