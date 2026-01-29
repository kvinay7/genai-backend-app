package com.example.backend.filters;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import java.io.IOException;

@Component
public class AuthFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain)
            throws IOException, ServletException {

        String auth = req.getHeader("Authorization");

        if (auth == null) {
            res.setStatus(HttpStatus.UNAUTHORIZED.value());
            return;
        }

        req.setAttribute("userId", "user-123");
        chain.doFilter(req, res);
    }
}
