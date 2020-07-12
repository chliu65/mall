package com.lc.mallcategory.common.resp;

import lombok.Getter;


@Getter
public enum ResponseEnum {
    //请求成功
    SUCCESS(20000,"SUCCESS"),

    //客户端错误
    ILLEGAL_ARGUMENTS(40001,"参数异常"),

    LOGIN_EXPIRED(40002,"登录超时，需要重新登录"),

    USER_NOT_EXIST(40003,"用户不存在"),

    PASSWORD_WRONG(40004,"密码错误"),

    USERNAME_EXISTED(40005,"用户名已存在"),

    EMAIL_EXISTED(40006,"邮箱已注册"),

    ANSWER_WRONG(40007,"回答错误"),

    PRODUCT_NOT_EXIST(40008,"商品不存在"),

    PARENT_CATEGORY_NOT_EXIST(40009,"父种类不存在"),

    CATEGORY_NOT_EXIST(400010,"种类不存在"),

    //服务端错误
    SERVER_ERROR(50000,"服务器内部错误");

;

    private int code;
    private String msg;

    ResponseEnum(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
