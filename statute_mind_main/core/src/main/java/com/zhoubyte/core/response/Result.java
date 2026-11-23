package com.zhoubyte.core.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Result {

    private int code;
    private Object data;
    private String message;


    public static Result success(Object data) {
        return new Result(HttpStatus.OK.value(), data, "SUCCESS");
    }

    public static Result failed(int code, String message) {
        return new Result(code, null, message);
    }

}
