package com.ourhr.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        List<ErrorDetail.FieldErrorDetail> fieldErrors = ex.getBindingResult()
                .getAllErrors()
                .stream()
                .filter(FieldError.class::isInstance)
                .map(error -> {
                    FieldError fe = (FieldError) error;
                    return new ErrorDetail.FieldErrorDetail(fe.getField(), fe.getDefaultMessage());
                })
                .collect(Collectors.toList());

        ErrorDetail errorDetail = ErrorDetail.builder()
                .code("VALIDATION_FAILED")
                .message("Request validation failed")
                .details(fieldErrors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.failure(errorDetail));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException ex) {
        ErrorDetail errorDetail = ErrorDetail.builder()
                .code("ACCESS_DENIED")
                .message(ex.getMessage() != null ? ex.getMessage() : "Insufficient privileges")
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.failure(errorDetail));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException ex) {
        ErrorDetail errorDetail = ErrorDetail.builder()
                .code("INVALID_ARGUMENT")
                .message(ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.failure(errorDetail));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        ErrorDetail errorDetail = ErrorDetail.builder()
                .code("INTERNAL_ERROR")
                .message("An unexpected error occurred")
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.failure(errorDetail));
    }
}
