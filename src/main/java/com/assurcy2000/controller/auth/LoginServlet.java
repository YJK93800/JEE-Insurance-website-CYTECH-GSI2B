package com.assurcy2000.controller.auth;

import com.assurcy2000.dto.SessionUser;
import com.assurcy2000.filter.AuthFilter;
import com.assurcy2000.service.AuthService;
import com.assurcy2000.util.FormView;
import com.assurcy2000.util.RequestParams;
import com.assurcy2000.util.UserSession;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@WebServlet("/auth/login")
public class LoginServlet extends HttpServlet {
    private static final String VIEW = "/WEB-INF/views/auth/login.jsp";

    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        SessionUser current = UserSession.currentUser(req);
        if (current != null){
            resp.sendRedirect(req.getContextPath() + current.homePath());
            return;
        }
        req.getRequestDispatcher(VIEW).forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");

        String email = RequestParams.get(req, "email");
        String password = RequestParams.raw(req, "password"); // pas de trim sur un mot de passe

        // Validation coté serveur
        Map<String, String> errors = new HashMap<>();
        if (email.isEmpty()) {
            errors.put("email", "L'adresse e-mail est obligatoire.");
        }
        if (password.isEmpty()) {
            errors.put("password", "Le mot de passe est obligatoire.");
        }
        if (!errors.isEmpty()){
            FormView.show(req, resp, VIEW, errors);
            return;
        }

        // Vérification des identifiants
        Optional<SessionUser> result;
        try {
            result = authService.login(email, password);
        } catch (RuntimeException e) {
            log("Erreur lors de la connexion de " + email, e);
            FormView.show(req, resp, VIEW, errors, "Une erreur est survenue. Veuillez réessayer.");
            return;
        }

        if (result.isEmpty()){
            FormView.show(req, resp, VIEW, errors, "Email ou mot de passe incorrect.");
            return;
        }
        SessionUser user = result.get();

        // Ouverture de la session
        String redirect = UserSession.login(req, user);

        // Retour à la page demandée avant la connexion, si elle est autorisée pour ce rôle
        String target = (redirect != null && AuthFilter.isAllowed(redirect, user)) ? redirect : user.homePath();
        resp.sendRedirect(req.getContextPath() + target);
    }
}
