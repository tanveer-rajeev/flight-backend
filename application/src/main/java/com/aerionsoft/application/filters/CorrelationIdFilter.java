package com.aerionsoft.application.filters;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
public class CorrelationIdFilter implements Filter {

    private static final String HEADER = "X-Correlation-Id";

    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain
    ) throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String cid = req.getHeader(HEADER);
        if (cid == null || cid.isEmpty()) {
            cid = UUID.randomUUID().toString();
        }

        MDC.put("cid", cid);
        res.setHeader(HEADER, cid);

        try {
            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
