package com.back.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ServiceException extends RuntimeException {

	private final HttpStatus status;
	private final String field;

	public ServiceException(HttpStatus status, String message) {
		this(status, null, message);
	}

	public ServiceException(HttpStatus status, String field, String message) {
		super(message);
		this.status = status;
		this.field = field;
	}
}
