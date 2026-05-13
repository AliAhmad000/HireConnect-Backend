package com.hireconnect.application.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateApplicationException extends RuntimeException {
    public DuplicateApplicationException(Long jobId, Long candidateId) {
        super("Candidate " + candidateId + " has already applied for job " + jobId);
    }
}
