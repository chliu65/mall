package com.lc.mallcart.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;


@NoArgsConstructor
@AllArgsConstructor
@Data
public class CartProductVo implements Serializable {

    private Integer id;

    private Integer userId;

    private Integer productId;

    private Integer quantity;//购物车中此商品的数量

    private String name;

    private String subtitle;

    private String mainImage;

    private BigDecimal price;

    private BigDecimal productTotalPrice;

    private Integer checked;//此商品是否勾选

}
