package com.assurcy2000.controller.auth;

import com.assurcy2000.controller.BaseServlet;
import com.assurcy2000.security.UserSession;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Déconnexion en POST : une action qui modifie l'état ne passe jamais par un GET.
 * Le formulaire de la sidebar envoie le jeton CSRF, vérifié par CsrfFilter.
 */
@WebServlet("/logout")
public class LogoutServlet extends BaseServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        UserSession.logout(req);  // invalide la session : utilisateur, jeton CSRF, etc.
        UserSession.flash(req, "Vous avez été déconnecté.", "success"); // dans une nouvelle session
        redirect(req, resp, "/login");
    }
}