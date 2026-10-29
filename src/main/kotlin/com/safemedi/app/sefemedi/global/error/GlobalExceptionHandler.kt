package com.safemedi.app.sefemedi.global.error

import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.slf4j.LoggerFactory
@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(exception: BusinessException): ResponseEntity<ErrorResponse> {
        val errorCode = exception.errorCode
        return ResponseEntity.status(errorCode.status)
            .body(ErrorResponse(errorCode.code, errorCode.message))
    }

    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingRequestParameter(
        exception: MissingServletRequestParameterException
    ): ResponseEntity<ErrorResponse> {
        val errorCode = ErrorCode.INVALID_REQUEST
        return ResponseEntity.status(errorCode.status)
            .body(ErrorResponse(errorCode.code, errorCode.message))
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleMethodArgumentTypeMismatch(
        exception: MethodArgumentTypeMismatchException
    ): ResponseEntity<ErrorResponse> {
        log.warn("Failed to convert request parameter: {}", exception.message)
        val errorCode = ErrorCode.INVALID_REQUEST
        return ResponseEntity.status(errorCode.status)
            .body(ErrorResponse(errorCode.code, errorCode.message))
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadable(
        exception: HttpMessageNotReadableException
    ): ResponseEntity<ErrorResponse> {
        log.warn("Failed to read HTTP message: {}", exception.message)
        val errorCode = ErrorCode.INVALID_REQUEST
        return ResponseEntity.status(errorCode.status)
            .body(ErrorResponse(errorCode.code, errorCode.message))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValid(
        exception: MethodArgumentNotValidException
    ): ResponseEntity<ErrorResponse> {
        log.warn("Failed to validate request body: {}", exception.message)
        val errorCode = ErrorCode.INVALID_REQUEST
        return ResponseEntity.status(errorCode.status)
            .body(ErrorResponse(errorCode.code, errorCode.message))
    }

    @ExceptionHandler(Exception::class)
    fun handleException(exception: Exception): ResponseEntity<ErrorResponse> {
        log.error("Unhandled exception occurred", exception)

        val errorCode = ErrorCode.INTERNAL_SERVER_ERROR
        return ResponseEntity.status(errorCode.status)
            .body(ErrorResponse(errorCode.code, errorCode.message))
    }
}
