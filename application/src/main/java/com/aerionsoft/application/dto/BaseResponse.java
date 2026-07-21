package com.aerionsoft.application.dto;

import com.aerionsoft.application.enums.common.ErrorCode;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder

public class BaseResponse<T> implements Serializable {
    private boolean success;
    private String message;
    private Integer status;
    private T data;
    private Object error;
  /** Echo of request timezone from X-User-Time-Offset header or userTimeOffset body field. */
    private String userTimeOffset;

    public static <T> BaseResponse<T> ok(T data) {
        return BaseResponse.<T>builder().success(true).message("Success").status(200).data(data).build();
    }

    public static <T> BaseResponse<T> created(String message, T data) {
        return BaseResponse.<T>builder().success(true).message(message).status(201).data(data).build();
    }

    public static <T> BaseResponse<T> ok(String message, T data) {
        return BaseResponse.<T>builder().success(true).message(message).status(200).data(data).build();
    }

    public static <T> BaseResponse<T> ok(T data, String message) {
        return BaseResponse.<T>builder().success(true).message(message).status(200).data(data).build();
    }

    public static <T> BaseResponse<T> ok(String message) {
        return BaseResponse.<T>builder().success(true).message(message).status(200).data(null).build();
    }


    public static <T> BaseResponse<T> error(int status, String message, Object error) {
        return BaseResponse.<T>builder().success(false).status(status).message(message).error(error).build();
    }

    public static <T> BaseResponse<T> error(String message) {
        return error(400, message, null);
    }

    public static <T> BaseResponse<T> error(int status, String message) {
        return BaseResponse.<T>builder().success(false).status(status).message(message).build();
    }

    public static <T> BaseResponse<T> error(ErrorCode code, String message, ApiError error) {
        int status = code.getStatus();
        String resolvedMessage = message != null && !message.isBlank() ? message : code.getDefaultMessage();
        return BaseResponse.<T>builder()
                .success(false)
                .status(status)
                .message(resolvedMessage)
                .error(error)
                .build();
    }

    public static <T> BaseResponse<T> error(ErrorCode code, ApiError error) {
        return error(code, code.getDefaultMessage(), error);
    }

}