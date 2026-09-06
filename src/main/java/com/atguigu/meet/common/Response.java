package com.atguigu.meet.common;

/**
 * @Description
 * @Date 2026-04-23 16:17
 * 全局统一返回结果
 * 格式：{code:状态码, msg:提示信息, data:返回数据}
 */
public class Response<T> {
    // 状态码
    private int code;
    // 提示信息
    private String msg;
    // 返回给前端的数据
    private T data;

    public Response() {
    }

    public Response(int code, String msg, Object data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public static <T> Response<T> ok() {
        return new Response<>(200, "操作成功", null);
    }

    public static <T> Response<T> ok(T data) {
        return new Response<>(200, "操作成功", data);
    }

    public static <T> Response<T> ok(int code, String msg) {
        return new Response<>(code, msg, null);
    }

    public static <T> Response<T> ok(String msg, T data) {
        return new Response<>(200, msg, data);
    }

    public static <T> Response<T> ok(int code, String msg, T data) {
        return new Response<>(code, msg, data);
    }

    public static <T> Response<T> fail() {
        return new Response<>(500, "操作失败", null);
    }

    public static <T> Response<T> fail(T data) {
        return new Response<>(500, "操作失败", data);
    }

    public static <T> Response<T> fail(int code, String msg) {
        return new Response<>(code, msg, null);
    }

    public static <T> Response<T> fail(String msg, T data) {
        return new Response<>(500, msg, data);
    }

    public static <T> Response<T> fail(int code, String msg, T data) {
        return new Response<>(code, msg, data);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}