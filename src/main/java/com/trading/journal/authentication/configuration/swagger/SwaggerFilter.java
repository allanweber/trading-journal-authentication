package com.trading.journal.authentication.configuration.swagger;

import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class SwaggerFilter  implements Filter {

    public static final String EMPTY = "";
    public static final String SLASH = "/";
    public static final String SWAGGER_UI_INDEX_HTML = "/swagger-ui/index.html";

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        if (EMPTY.equals(request.getRequestURI()) || SLASH.equals(request.getRequestURI())) {
            response.sendRedirect(SWAGGER_UI_INDEX_HTML);
            return;
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }
}
