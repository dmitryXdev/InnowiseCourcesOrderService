package com.innowise.orderservice.exception.handler;

import com.innowise.orderservice.exception.AccessDeniedException;
import com.innowise.orderservice.exception.BadIncomeDataException;
import com.innowise.orderservice.exception.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestValueException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> internalServerError(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .body(
                        ErrorResponse.builder()
                                .message(e.getMessage())
                                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                .build()
                );
    }

    @ExceptionHandler(MissingRequestValueException.class)
    public ResponseEntity<ErrorResponse> missingRequestValueException(MissingRequestValueException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST.value())
                .body(
                        ErrorResponse.builder()
                                .message(e.getMessage())
                                .status(HttpStatus.BAD_REQUEST.value())
                                .build()
                );
    }

    @ExceptionHandler(BadIncomeDataException.class)
    public ResponseEntity<ErrorResponse> badIncomeData(BadIncomeDataException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST.value())
                .body(
                        ErrorResponse.builder()
                                .message(e.getMessage())
                                .status(HttpStatus.BAD_REQUEST.value())
                                .build()
                );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> accessDeniedException(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN.value())
                .body(
                        ErrorResponse.builder()
                                .message(e.getMessage())
                                .status(HttpStatus.FORBIDDEN.value())
                                .build()
                );
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> entityNotFoundException(EntityNotFoundException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST.value())
                .body(
                        ErrorResponse.builder()
                                .message(e.getMessage())
                                .status(HttpStatus.BAD_REQUEST.value())
                                .build()
                );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> methodArgumentNotValidException(MethodArgumentNotValidException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST.value())
                .body(
                        ErrorResponse.builder()
                                .message(e.getMessage())
                                .status(HttpStatus.BAD_REQUEST.value())
                                .build()
                );
    }
}
