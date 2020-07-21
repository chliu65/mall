package com.lc.mallcart.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CartUserProductVo {

    private  Integer userId;

    private Integer productId;
}
