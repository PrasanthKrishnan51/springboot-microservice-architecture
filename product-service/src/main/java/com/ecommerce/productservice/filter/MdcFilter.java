package com.ecommerce.productservice.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Component
@Order(1)
public class MdcFilter implements Filter {
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest h = (HttpServletRequest) req;
        MDC.put("correlationId", Optional.ofNullable(h.getHeader("X-Correlation-Id")).orElse(UUID.randomUUID().toString()));
        MDC.put("userId", Optional.ofNullable(h.getHeader("X-User-Id")).orElse("anonymous"));
        MDC.put("requestUri", h.getRequestURI());
        MDC.put("method", h.getMethod());
        try {
            chain.doFilter(req, res);
        } finally {
            MDC.clear();
        }
    }
}
