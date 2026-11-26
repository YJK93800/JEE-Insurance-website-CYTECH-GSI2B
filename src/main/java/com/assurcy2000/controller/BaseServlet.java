package com.assurcy2000.controller;

import com.assurcy2000.model.ClaimAttachment;
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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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

    /**
     * Envoie une pièce justificative au navigateur (affichée dans l'onglet si possible).
     * Les droits ont déjà été vérifiés par ClaimService.attachment.
     * nosniff + sandbox : le navigateur ne devine pas un autre type et n'exécute aucun script du fichier.
     */
    protected void sendAttachment(HttpServletResponse resp, ClaimAttachment attachment, Path file) throws IOException {
        if (!Files.isRegularFile(file)) throw new NotFoundException("Fichier introuvable.");
        String name = attachment.getFileName();
        String ascii = name.replaceAll("[^A-Za-z0-9._-]", "_");
        String utf8 = URLEncoder.encode(name, StandardCharsets.UTF_8).replace("+", "%20");
        resp.setContentType(attachment.getMimeType());
        resp.setContentLengthLong(Files.size(file));
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("Content-Security-Policy", "sandbox");
        resp.setHeader("Content-Disposition", "inline; filename=\"" + ascii + "\"; filename*=UTF-8''" + utf8);
        Files.copy(file, resp.getOutputStream());
    }

    protected ClaimStatus claimStatusParam(HttpServletRequest req, String name) {
        try {
            return ClaimStatus.valueOf(RequestParams.get(req, name));
        } catch (IllegalArgumentException e) {
            throw new ValidationException(Map.of(name, "Statut invalide."));
        }
    }
}
