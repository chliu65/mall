package com.lc.mallorder.common.exception;




import com.lc.mallorder.common.resp.ResponseEnum;
import lombok.Getter;


@Getter
public class GlobalException extends RuntimeException{
    private ResponseEnum responseEnum;

    public GlobalException(String msg){
        super(msg);
    }

    public GlobalException(ResponseEnum responseEnum){
        super(responseEnum.getMsg());
        this.responseEnum=responseEnum;
    }

}
