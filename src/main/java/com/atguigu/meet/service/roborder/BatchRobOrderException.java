package com.atguigu.meet.service.roborder;

import com.atguigu.meet.common.Response;

/** 批量操作中途失败时触发事务回滚，并将原操作结果返回给调用方。 */
public class BatchRobOrderException extends RuntimeException {

    private final Response<?> response;

    public BatchRobOrderException(Response<?> response) {
        super(response.getMsg());
        this.response = response;
    }

    public Response<?> getResponse() {
        return response;
    }
}
