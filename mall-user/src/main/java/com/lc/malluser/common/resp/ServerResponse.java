package com.lc.malluser.common.resp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Getter;

import java.io.Serializable;


@Getter
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
public class ServerResponse<T> implements Serializable {
    private int code;
    private String msg;
    private T data;

    public ServerResponse(){}

    private ServerResponse(int code){
        this.code = code;
    }
    private ServerResponse(int code,String msg){
        this.code = code;
        this.msg = msg;
    }
    private ServerResponse(int code,T data){
        this.code = code;
        this.data = data;
    }
    private ServerResponse(int code,String msg,T data){
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    @JsonIgnore
    public boolean isSuccess(){
        return this.code == ResponseEnum.SUCCESS.getCode();
    }

    /**
     * 成功的方法
     */
    public static <T>ServerResponse<T> createBySuccess(){
        return new ServerResponse<>(ResponseEnum.SUCCESS.getCode(),ResponseEnum.SUCCESS.getMsg());
    }
    public static <T>ServerResponse<T> createBySuccessMessage(String message){
        return new ServerResponse<>(ResponseEnum.SUCCESS.getCode(),message);
    }
    public static <T>ServerResponse<T> createBySuccess(T data){
        return new ServerResponse<>(ResponseEnum.SUCCESS.getCode(),data);
    }
    public static <T>ServerResponse<T> createBySuccess(String message,T data){
        return new ServerResponse<>(ResponseEnum.SUCCESS.getCode(),message,data);
    }

    /**
     * 失败的方法
     */
    public static <T>ServerResponse<T> createByError(ResponseEnum responseEnum){
        return new ServerResponse<>(responseEnum.getCode(),responseEnum .getMsg());
    }
    public static <T>ServerResponse<T> createByErrorMessage(String msg){
        return new ServerResponse<>(ResponseEnum.ILLEGAL_ARGUMENTS.getCode(),msg);
    }
    public static <T>ServerResponse<T> createByErrorCodeMessage(int code,String msg){
        return new ServerResponse<>(code,msg);
    }



}
