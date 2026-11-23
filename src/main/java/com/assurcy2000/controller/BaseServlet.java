package com.assurcy2000.controller;

import com.assurcy2000.model.enums.ClaimStatus;
import com.assurcy2000.dto.SessionUser;
import com.assurcy2000.service.exceptions.ForbiddenException;
import com.assurcy2000.service.exceptions.NotFoundException;
import com.assurcy2000.service.exceptions.ValidationException;
import com.assurcy2000.security.Policy;
import com.assurcy2000.security.UserSession;
import com.assurcy2000.util.RequestParams;
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
        return Policy.requireLoggedIn(UserSession.currentUser(req));
    }

    protected void render(HttpServletRequest req, HttpServletResponse resp, String view)
            throws ServletException, IOException {
        req.getRequestDispatcher(view).forward(req, resp);
    }

    protected void redirect(HttpServletRequest req, HttpServletResponse resp, String path) throws IOException {
        resp.sendRedirect(req.getContextPath() + path);
    }

    protected Long idParam(HttpServletRequest req, String name) {
        Long id = RequestParams.optionalLong(req, name);
        if (id == null) throw new NotFoundException("Ressource introuvable.");
        return id;
    }

    protected ClaimStatus claimStatusParam(HttpServletRequest req, String name) {
        try {
            return ClaimStatus.valueOf(RequestParams.get(req, name));
        } catch (IllegalArgumentException e) {
            throw new ValidationException(Map.of(name, "Statut invalide."));
        }
    }
}
