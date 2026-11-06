package com.churnguard.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class MlExceptionHandler {

    @ExceptionHandler(MlServiceException.class)
    public ProblemDetail handleMlUnavailable(MlServiceException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE, "Prediction service unavailable");
        problem.setTitle("Service unavailable");
        return problem;
    }
}