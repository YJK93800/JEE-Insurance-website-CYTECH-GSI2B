package com.assurcy2000.controller;

import com.assurcy2000.model.enums.ClaimStatus;
import com.assurcy2000.security.Auth;
import com.assurcy2000.security.SessionUser;
import com.assurcy2000.service.exceptions.ForbiddenException;
import com.assurcy2000.service.exceptions.NotFoundException;
import com.assurcy2000.service.exceptions.ValidationException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

public abstract class BaseServlet extends HttpServlet {

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        try {
            super.service(req, resp);
        } catch (NotFoundException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, e.getMessage());
        } catch (ForbiddenException e) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, e.getMessage());
        }
    }

    protected SessionUser user(HttpServletRequest req) {
        SessionUser user = Auth.current(req);
        if (user == null) throw new ForbiddenException();
        return user;
    }

    protected void render(HttpServletRequest req, HttpServletResponse resp, String view)
            throws ServletException, IOException {
        req.getRequestDispatcher(view).forward(req, resp);
    }

    protected void redirect(HttpServletRequest req, HttpServletResponse resp, String path) throws IOException {
        resp.sendRedirect(req.getContextPath() + path);
    }

    protected Long idParam(HttpServletRequest req, String name) {
        try {
            return Long.valueOf(req.getParameter(name).trim());
        } catch (RuntimeException e) {
            throw new NotFoundException("Ressource introuvable.");
        }
    }

    protected ClaimStatus claimStatusParam(HttpServletRequest req, String name) {
        try {
            return ClaimStatus.valueOf(req.getParameter(name).trim());
        } catch (RuntimeException e) {
            throw new ValidationException(Map.of(name, "Statut invalide."));
        }
    }

    protected static String param(HttpServletRequest req, String name) {
        String v = req.getParameter(name);
        return v == null ? "" : v;
    }
}
