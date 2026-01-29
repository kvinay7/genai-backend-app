package com.example.backend.filters;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

@Component
public class LoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain)
            throws IOException, ServletException {

        String requestId = UUID.randomUUID().toString();
        req.setAttribute("requestId", requestId);

        System.out.println("[" + requestId + "] "
                + req.getMethod() + " " + req.getRequestURI());

        chain.doFilter(req, res);
    }
}