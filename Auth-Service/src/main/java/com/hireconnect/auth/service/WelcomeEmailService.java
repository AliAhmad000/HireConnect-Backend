package com.hireconnect.auth.service;

import com.hireconnect.auth.entity.UserCredential;

public interface WelcomeEmailService {

    void sendWelcomeEmail(UserCredential user);

    void sendPasswordResetEmail(UserCredential user, String resetToken, String resetUrl);
}
