package com.fileweaver.api;

import com.fileweaver.api.dto.ApiError;
import com.fileweaver.reports.exceptions.UnknownReportTypeException;
import com.fileweaver.writers.exceptions.UnsupportedFormatException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(UnknownReportTypeException.class)
    public ResponseEntity<ApiError> unknownType(UnknownReportTypeException e) {
        return ResponseEntity.badRequest().body(new ApiError("INVALID_REPORT_TYPE", e.getMessage()));
    }

    @ExceptionHandler(UnsupportedFormatException.class)
    public ResponseEntity<ApiError> unsupportedFormat(UnsupportedFormatException e) {
        return ResponseEntity.badRequest().body(new ApiError("UNSUPPORTED_FORMAT", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> illegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(new ApiError("INVALID_PAYLOAD", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(MethodArgumentNotValidException e) {
        var fe = e.getBindingResult().getFieldError();
        String msg = fe != null
            ? fe.getField() + " " + fe.getDefaultMessage()
            : "Validation failed";
        return ResponseEntity.badRequest().body(new ApiError("VALIDATION_FAILED", msg));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> badJson(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest()
            .body(new ApiError("MALFORMED_REQUEST", "Could not parse request body"));
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ApiError> duplicate(DuplicateKeyException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new ApiError("DUPLICATE", "Resource already exists"));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> statusException(ResponseStatusException e) {
        String code = switch (e.getStatusCode().value()) {
            case 401 -> "UNAUTHORIZED";
            case 403 -> "FORBIDDEN";
            case 404 -> "NOT_FOUND";
            case 409 -> "CONFLICT";
            default -> "ERROR";
        };
        String msg = e.getReason() != null ? e.getReason() : e.getStatusCode().toString();
        return ResponseEntity.status(e.getStatusCode()).body(new ApiError(code, msg));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> unhandled(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ApiError("INTERNAL_ERROR", "Something went wrong"));
    }
}
