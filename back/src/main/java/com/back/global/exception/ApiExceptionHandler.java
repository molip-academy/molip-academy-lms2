package com.back.global.exception;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** 거부될 때 어떤 항목이 왜 잘못됐는지 알 수 있어야 한다. */
@RestControllerAdvice
public class ApiExceptionHandler {

	public record ErrorResponse(String message, Map<String, String> errors) {}

	@ExceptionHandler(ServiceException.class)
	public ResponseEntity<ErrorResponse> handle(ServiceException e) {
		Map<String, String> errors = new LinkedHashMap<>();
		if (e.getField() != null) {
			errors.put(e.getField(), e.getMessage());
		}
		return ResponseEntity.status(e.getStatus()).body(new ErrorResponse(e.getMessage(), errors));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handle(MethodArgumentNotValidException e) {
		Map<String, String> errors = new LinkedHashMap<>();
		e.getBindingResult().getFieldErrors()
				.forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
		return ResponseEntity.badRequest().body(new ErrorResponse("입력값이 올바르지 않습니다.", errors));
	}

	/** 경로의 일지 날짜가 날짜로 읽히지 않을 때. */
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ErrorResponse> handle(MethodArgumentTypeMismatchException e) {
		return ResponseEntity.badRequest().body(new ErrorResponse(
				"'%s' 값을 읽을 수 없습니다.".formatted(e.getName()),
				Map.of(e.getName(), "형식이 올바르지 않습니다.")));
	}

	@ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponse> handle(org.springframework.http.converter.HttpMessageNotReadableException e) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(new ErrorResponse("요청 본문을 읽을 수 없습니다.", Map.of()));
	}
}
