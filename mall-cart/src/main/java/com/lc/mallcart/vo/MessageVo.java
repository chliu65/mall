package com.lc.mallcart.vo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 消息队列通用消息体
 * @param <T>
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageVo<T> implements Serializable {
    //数据库操作指令
    private String order;
    //对应需要的数据
    private T data;
}
