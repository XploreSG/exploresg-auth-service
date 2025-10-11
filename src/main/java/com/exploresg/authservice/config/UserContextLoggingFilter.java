package com.exploresg.authservice.config;

import com.exploresg.authservice.model.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter that adds authenticated user information to MDC for logging.
 * This runs after authentication so we can include user details in logs.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class UserContextLoggingFilter extends OncePerRequestFilter {

    private static final String USER_ID_MDC_KEY = "userId";
    private static final String USER_EMAIL_MDC_KEY = "userEmail";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication != null && authentication.isAuthenticated()
                    && authentication.getPrincipal() instanceof User) {
                User user = (User) authentication.getPrincipal();

                // Add user information to MDC
                MDC.put(USER_ID_MDC_KEY, String.valueOf(user.getId()));
                MDC.put(USER_EMAIL_MDC_KEY, user.getEmail());
            }

            filterChain.doFilter(request, response);
        } finally {
            // Clean up user-specific MDC keys
            MDC.remove(USER_ID_MDC_KEY);
            MDC.remove(USER_EMAIL_MDC_KEY);
        }
    }
}
