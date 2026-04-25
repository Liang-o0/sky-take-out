package com.sky.handler;

import com.sky.constant.MessageConstant;
import com.sky.exception.BaseException;
import com.sky.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLIntegrityConstraintViolationException;

/**
 * 全局异常处理器，处理项目中抛出的业务异常
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    /**
     * 捕获业务异常
     * @param ex
     * @return
     */
    @ExceptionHandler/**是 Spring 的一个异常处理注解，意思是：当前这个方法是“专门用来处理异常的”
     表示：当程序运行时抛出 SQLIntegrityConstraintViolationException 这种异常时，
     Spring 会自动调用这个方法来处理，而不是让异常直接报错到前端
     **/
    public Result exceptionHandler(BaseException ex){
        log.error("异常信息：{}", ex.getMessage());
        return Result.error(ex.getMessage());
    }
    /*
    *
    *
    * */
    @ExceptionHandler
public Result exceptionHandler(SQLIntegrityConstraintViolationException ex){
//    提示数据库中用户名已经存在->Duplicate entry 'lisi' for key 'employee.idx_username'
    String message = ex.getMessage();
    if(message.contains("Duplicate entry")){
        String[] split = message.split(" ");
        String username = split[2];
        String mgs = username + MessageConstant.ALREADY_EXIST;
        return Result.error(mgs);
    }else {
        return Result.error(MessageConstant.UNKNOWN_ERROR);
    }
}
}
