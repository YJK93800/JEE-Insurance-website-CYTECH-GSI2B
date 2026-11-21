package com.assurcy2000.security;

import com.assurcy2000.dto.SessionUser;
import com.assurcy2000.model.Role;
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

    /** La page est-elle accessible pour ce rôle ? (même règle que RoleFilter) */
    public static boolean isAllowed(String path, SessionUser user) {
        if (path.startsWith("/admin/"))  return user.is(Role.ADMIN);
        if (path.startsWith("/agent/"))  return user.is(Role.AGENT) || user.is(Role.ADMIN);
        if (path.startsWith("/client/")) return user.is(Role.CLIENT);
        return true;
    }
}
