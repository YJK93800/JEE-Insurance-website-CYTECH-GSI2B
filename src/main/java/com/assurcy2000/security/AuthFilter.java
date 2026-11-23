package com.assurcy2000.security;

import com.assurcy2000.dto.SessionUser;
import com.assurcy2000.security.UserSession;
import jakarta.servlet.*;
import jakarta.servlet.http.*;

import java.io.IOException;

public class AuthFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        String path = UserSession.currentPath(req);

        if (isPublic(path)) {
            chain.doFilter(request, response);
            return;
        }

        SessionUser user = UserSession.currentUser(req);
        if (user == null) {
            if ("GET".equals(req.getMethod())) { // on ne rejoue pas un formulaire POST
                String query = req.getQueryString();
                UserSession.rememberRedirect(req, path + (query == null ? "" : "?" + query));
            }
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        req.setAttribute("currentUser", user);
        resp.setHeader("Cache-Control", "no-store");
        chain.doFilter(request, response);
    }

    private static boolean isPublic(String path) {
        return path.equals("/login")
                || path.equals("/register")
                || path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.equals("/preview"); // à retirer avant le rendu
    }
}
