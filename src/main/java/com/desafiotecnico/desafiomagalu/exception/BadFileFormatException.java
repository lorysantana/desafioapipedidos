package com.desafiotecnico.desafiomagalu.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BadFileFormatException extends RuntimeException {

    public BadFileFormatException(String message) {
        super(message);
    }

    public BadFileFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
