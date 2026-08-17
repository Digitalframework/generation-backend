package com.image.generation.common;

import com.image.generation.common.ApiExceptions.ConflictException;
import com.image.generation.common.ApiExceptions.NotFoundException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.stream.Collectors;

/**
 * Ordered ahead of Boot's own ProblemDetailsExceptionHandler, which would otherwise
 * answer binding failures with a bare "Invalid request content.".
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class ApiExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail onNotFound(NotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    ProblemDetail onConflict(ConflictException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail onIllegalArgument(IllegalArgumentException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * Field-level detail instead of the default "Invalid request content." — a form
     * field that failed to convert keeps the converter's own message.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail onInvalidArgument(MethodArgumentNotValidException ex) {
        return bindingProblem(ex.getBindingResult());
    }

    @ExceptionHandler(BindException.class)
    ProblemDetail onBindFailure(BindException ex) {
        return bindingProblem(ex.getBindingResult());
    }

    private static ProblemDetail bindingProblem(BindingResult bindingResult) {
        String detail = bindingResult.getFieldErrors().stream()
                .map(error -> error.getField() + ": " + messageOf(error))
                .collect(Collectors.joining("; "));
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                detail.isEmpty() ? "Invalid request content." : detail);
    }

    private static String messageOf(FieldError error) {
        if (error.contains(TypeMismatchException.class)) {
            Throwable cause = NestedExceptionUtils.getMostSpecificCause(error.unwrap(TypeMismatchException.class));
            return cause.getMessage();
        }
        return error.getDefaultMessage();
    }

    /**
     * An unknown {@code ?sort=} property is a bad request, not a server error. Spring Data
     * reports it either as a property reference or, for JPA, as an invalid sort expression.
     */
    @ExceptionHandler({PropertyReferenceException.class, InvalidDataAccessApiUsageException.class})
    ProblemDetail onUnknownSortProperty(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                NestedExceptionUtils.getMostSpecificCause(ex).getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ProblemDetail onTooLarge(MaxUploadSizeExceededException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONTENT_TOO_LARGE, "The uploaded file is too large");
    }
}
