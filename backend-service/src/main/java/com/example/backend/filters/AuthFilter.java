package com.example.backend.filters;

import com.example.backend.services.AuthService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@Component
@Order(2)
public class AuthFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(AuthFilter.class);
    private static final Set<String> WHITELIST = Set.of(
            "/auth/login",
            "/auth/refresh",
            "/auth/logout",
            "/actuator/health",
            "/error"
    );

    private final AuthService authService;

    public AuthFilter(AuthService authService) {
        this.authService = authService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "OPTIONS".equalsIgnoreCase(request.getMethod()) || WHITELIST.contains(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        String requestId = (String) request.getAttribute("requestId");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.warn("[{}] Missing authorization header for {}", requestId, request.getRequestURI());
            sendUnauthorized(response, "Missing authorization token");
            return;
        }

        try {
            Claims claims = authService.validateAndExtractClaims(authHeader.substring(7));
            Long userId = Long.parseLong(claims.getSubject());
            String email = claims.get("email", String.class);
            String role = claims.get("role", String.class);

            request.setAttribute("userId", userId);
            request.setAttribute("email", email);
            request.setAttribute("role", role);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + role))
                    );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            try {
                chain.doFilter(request, response);
            } finally {
                SecurityContextHolder.clearContext();
            }
        } catch (ExpiredJwtException e) {
            logger.warn("[{}] Expired token for {}", requestId, request.getRequestURI());
            sendUnauthorized(response, "Token expired");
        } catch (JwtException | IllegalArgumentException e) {
            logger.warn("[{}] Invalid token for {}", requestId, request.getRequestURI());
            sendUnauthorized(response, "Invalid token");
        }
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                String.format("{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"%s\"}", message)
        );
    }
}
