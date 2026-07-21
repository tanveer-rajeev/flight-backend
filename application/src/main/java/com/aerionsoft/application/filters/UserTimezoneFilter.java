package com.aerionsoft.application.filters;

import java.io.IOException;

import com.aerionsoft.application.context.UserTimezoneContext;
import com.aerionsoft.application.util.UserTimezoneUtil;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(0)
public class UserTimezoneFilter implements Filter {

    public static final String HEADER = "X-User-Time-Offset";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        String offset = req.getHeader(HEADER);
        if (offset == null || offset.isBlank()) {
            offset = UserTimezoneUtil.DEFAULT_OFFSET;
        }
        UserTimezoneContext.set(offset);

        try {
            chain.doFilter(request, response);
        } finally {
            UserTimezoneContext.clear();
        }
    }
}
