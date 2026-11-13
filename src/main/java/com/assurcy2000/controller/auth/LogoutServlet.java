package com.assurcy2000.controller.auth;

import com.assurcy2000.util.UserSession;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/logout")
public class LogoutServlet extends HttpServlet {

    // GET car la sidebar utilise un simple lien <a href="/logout">
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        UserSession.logout(req);  // supprime "utilisateur", "csrfToken", etc.
        UserSession.flash(req, "Vous avez été déconnecté.", "success"); // dans une nouvelle session vide
        resp.sendRedirect(req.getContextPath() + "/auth/login");
    }
}
