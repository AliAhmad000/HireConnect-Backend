package com.hireconnect.job.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data @Builder @JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private Integer statusCode;
    @Builder.Default private LocalDateTime timestamp = LocalDateTime.now();

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder().success(true).message(message).data(data).statusCode(200).build();
    }
    public static <T> ApiResponse<T> success(T data) { return success("Operation successful", data); }
    public static <T> ApiResponse<T> created(String msg, T data) {
        return ApiResponse.<T>builder().success(true).message(msg).data(data).statusCode(201).build();
    }
    public static <T> ApiResponse<T> error(String msg, int code) {
        return ApiResponse.<T>builder().success(false).message(msg).statusCode(code).build();
    }
}
