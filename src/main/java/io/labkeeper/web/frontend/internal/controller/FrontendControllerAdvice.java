package io.labkeeper.web.frontend.internal.controller;

import io.labkeeper.businesslogic.api.exception.InvalidCodeException;
import io.labkeeper.businesslogic.api.exception.NoSuchUserException;
import io.labkeeper.businesslogic.api.exception.UserWithSuchEmailAlreadyExistsException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import reactor.core.publisher.Mono;

@ControllerAdvice
public class FrontendControllerAdvice {
    @ExceptionHandler
    public Mono<ResponseEntity<?>> handleNoSuchUserException(NoSuchUserException ignored) {
        return Mono.just(ResponseEntity.notFound().build());
    }

    @ExceptionHandler
    public Mono<ResponseEntity<?>> handleUserWithSuchEmailAlreadyExistsException(UserWithSuchEmailAlreadyExistsException ignored) {
        return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).build());
    }

    @ExceptionHandler
    public Mono<ResponseEntity<?>> handleInvalidCodeException(InvalidCodeException ignored) {
        return Mono.just(ResponseEntity.badRequest().build());
    }
}
