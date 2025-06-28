package com.piotr_brus.learning;

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

private const val invalidValueMessage = "Invalid value"
private const val errorsName = "errors"

@RestControllerAdvice
class GlobalValidationHandler {

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationError(e: MethodArgumentNotValidException): ResponseEntity<Map<String, Any>> {
        val errors = e.bindingResult.allErrors.map {
            it.defaultMessage ?: invalidValueMessage
        }
        return ResponseEntity
            .status(400)
            .body(mapOf(errorsName to errors))
    }
}
