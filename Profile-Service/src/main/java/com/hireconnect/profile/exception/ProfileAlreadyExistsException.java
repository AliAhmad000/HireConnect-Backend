package com.hireconnect.profile.exception;

public class ProfileAlreadyExistsException extends RuntimeException {
    public ProfileAlreadyExistsException(Long userId) {
        super("Profile already exists for userId: " + userId);
    }
}
