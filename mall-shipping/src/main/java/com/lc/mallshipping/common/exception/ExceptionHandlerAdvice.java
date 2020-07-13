package com.lc.mallshipping.common.exception;




import com.lc.mallshipping.common.resp.ResponseEnum;
import com.lc.mallshipping.common.resp.ServerResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;


@ControllerAdvice
@ResponseBody
@Slf4j
public class ExceptionHandlerAdvice {
    @ExceptionHandler(Exception.class)
    public ServerResponse handleException(Exception e){
        log.error(e.getMessage(),e);
        return ServerResponse.createByError(ResponseEnum.SERVER_ERROR);
    }

    @ExceptionHandler(GlobalException.class)
    public ServerResponse handleException(GlobalException e){
        log.error(e.getMessage(),e);
        return ServerResponse.createByError(e.getResponseEnum());
    }

}
