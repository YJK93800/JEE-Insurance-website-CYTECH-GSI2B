package com.assurcy2000.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

import java.io.IOException;

public class AuthFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        String path = Auth.currentPath(req);

        if (isPublic(path)) {
            chain.doFilter(request, response);
            return;
        }

        SessionUser user = Auth.current(req);
        if (user == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        req.setAttribute("currentUser", user);
        resp.setHeader("Cache-Control", "no-store");
        chain.doFilter(request, response);
    }

    private static boolean isPublic(String path) {
        return path.equals("/login") || path.equals("/register") || path.startsWith("/assets/");
    }
}
