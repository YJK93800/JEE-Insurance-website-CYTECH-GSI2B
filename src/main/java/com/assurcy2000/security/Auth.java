package com.assurcy2000.security;

import com.assurcy2000.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

public final class Auth {

    private static final String SESSION_USER = "sessionUser";

    private Auth() { }

    public static void signIn(HttpServletRequest req, User user) {
        req.getSession(true);
        req.changeSessionId();
        req.getSession().setAttribute(SESSION_USER, SessionUser.from(user));
    }

    public static void signOut(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session != null) session.invalidate();
    }

    public static SessionUser current(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        return session == null ? null : (SessionUser) session.getAttribute(SESSION_USER);
    }

    public static String currentPath(HttpServletRequest req) {
        String path = req.getServletPath() + (req.getPathInfo() == null ? "" : req.getPathInfo());
        return path.isEmpty() ? "/" : path;
    }
}
