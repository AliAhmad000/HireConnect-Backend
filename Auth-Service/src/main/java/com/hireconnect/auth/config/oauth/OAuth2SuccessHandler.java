package com.hireconnect.auth.config.oauth;

import com.hireconnect.auth.config.security.JwtService;
import com.hireconnect.auth.enums.*;
import com.hireconnect.auth.entity.UserCredential;
import com.hireconnect.auth.repository.UserCredentialRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserCredentialRepository userRepository;
    private final JwtService jwtService;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();

        String email = oauthUser.getAttribute("email");
        String name = oauthUser.getAttribute("name");

        if (email == null) {
            email = oauthUser.getAttribute("login") + "@github.com";
        }

        Optional<UserCredential> existingUser =
                userRepository.findByEmail(email);

        UserCredential user;

        if (existingUser.isPresent()) {

            user = existingUser.get();

        } else {

            user = UserCredential.builder()
                    .email(email)
                    .fullName(name != null ? name : "GitHub User")
                    .provider(AuthProvider.GITHUB)
                    .role(UserRole.CANDIDATE)
                    .passwordHash("OAUTH_USER")
                    .build();

            user = userRepository.save(user);
        }

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        String redirectUrl =
        	    "http://localhost:4200/auth/github/callback"
        	        + "?accessToken=" + accessToken
        	        + "&refreshToken=" + refreshToken
        	        + "&userId=" + user.getUserId()
        	        + "&email=" + user.getEmail()
        	        + "&fullName=" + user.getFullName()
        	        + "&role=" + user.getRole().name()
        	        + "&provider=" + user.getProvider().name();

        response.sendRedirect(redirectUrl);
    }
}