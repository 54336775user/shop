package com.eample.shop.common;

import lombok.Data;

@Data
public class Result<T> {
    private Integer code;
    private String message;
    private T data;

    public static <T>Result<T> ok(T date){
        Result<T> r=new Result<T>();
        r.setCode(200);
        r.setData(date);
        r.setMessage("success");
        return r;
    }
    public static <T> Result<T> ok(String message, T data) {
        Result<T> r = ok(data);
        r.setMessage(message);
        return r;
    }
    public static <T> Result<T> fail(String message) {
        Result<T> r = new Result<>();
        r.setCode(500);
        r.setMessage(message);
        return r;
    }
    public static <T> Result<T> fail(int code, String message) {
        Result<T> r = new Result<>();
        r.setCode(code);
        r.setMessage(message);
        return r;
    }
}
