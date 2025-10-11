package com.exploresg.authservice.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * Interceptor to log HTTP request/response details for audit trail.
 * Logs at INFO level for security-related endpoints, DEBUG for others.
 */
@Slf4j
@Component
public class RequestLoggingInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler) throws Exception {
        long startTime = System.currentTimeMillis();
        request.setAttribute("startTime", startTime);

        String method = request.getMethod();
        String path = request.getRequestURI();

        // Log at INFO level for authentication/authorization endpoints
        if (isSecurityEndpoint(path)) {
            log.info("Incoming request: {} {} from IP: {}",
                    method, path, getClientIp(request));
        } else {
            log.debug("Incoming request: {} {}", method, path);
        }

        return true;
    }

    @Override
    public void postHandle(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler,
            @Nullable ModelAndView modelAndView) throws Exception {
        // No-op - can be used for additional logging if needed
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler,
            @Nullable Exception ex) throws Exception {
        long startTime = (Long) request.getAttribute("startTime");
        long duration = System.currentTimeMillis() - startTime;

        String method = request.getMethod();
        String path = request.getRequestURI();
        int status = response.getStatus();

        // Log completed requests with timing information
        if (isSecurityEndpoint(path)) {
            log.info("Completed request: {} {} - Status: {} - Duration: {}ms",
                    method, path, status, duration);
        } else {
            log.debug("Completed request: {} {} - Status: {} - Duration: {}ms",
                    method, path, status, duration);
        }

        // Log slow requests as WARNING
        if (duration > 2000) {
            log.warn("Slow request detected: {} {} took {}ms", method, path, duration);
        }

        // Log exceptions
        if (ex != null) {
            log.error("Request failed: {} {} - Error: {}",
                    method, path, ex.getMessage(), ex);
        }
    }

    private boolean isSecurityEndpoint(String path) {
        return path.contains("/auth/") ||
                path.contains("/signup") ||
                path.contains("/admin/") ||
                path.contains("/me");
    }

    private String getClientIp(HttpServletRequest request) {
        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = request.getHeader("X-Real-IP");
        }
        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = request.getRemoteAddr();
        }
        if (clientIp != null && clientIp.contains(",")) {
            clientIp = clientIp.split(",")[0].trim();
        }
        return clientIp;
    }
}
