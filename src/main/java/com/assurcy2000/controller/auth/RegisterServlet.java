package com.assurcy2000.controller.auth;

import com.assurcy2000.controller.BaseServlet;
import com.assurcy2000.dto.request.RegisterRequest;
import com.assurcy2000.dto.SessionUser;
import com.assurcy2000.security.UserSession;
import com.assurcy2000.service.AuthService;
import com.assurcy2000.service.exceptions.ValidationException;
import com.assurcy2000.util.Validation;
import com.assurcy2000.util.Views;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@WebServlet("/register")
public class RegisterServlet extends BaseServlet {

    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        SessionUser current = UserSession.currentUser(req);
        if (current != null) { redirect(req, resp, current.homePath()); return; }
        render(req, resp, Views.REGISTER);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        RegisterRequest form = new RegisterRequest(
                param(req, "prenom"), param(req, "nom"), param(req, "email"), param(req, "telephone"),
                param(req, "dateNaissance"), param(req, "adresse"), param(req, "motDePasse"), param(req, "confirmation"));

        // 1. Vérification des saisies par la servlet, avant tout enregistrement (consigne, section 4)
        Map<String, String> errors = validate(form);

        // 2. Si le format est bon, le service vérifie ce qui demande la base (email déjà utilisé)
        if (errors.isEmpty()) {
            try {
                authService.register(form);
                UserSession.flash(req, "Compte créé. Vous pouvez vous connecter.", "success");
                redirect(req, resp, "/login");
                return;
            } catch (ValidationException e) {
                errors.putAll(e.getErrors());
            }
        }

        req.setAttribute("erreurs", errors);               // lu par <ui:champ> sous chaque champ
        req.setAttribute("form", new RegisterRequest(      // on réaffiche la saisie SANS les mots de passe
                form.firstName(), form.lastName(), form.email(), form.phone(),
                form.birthDate(), form.address(), "", ""));
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        render(req, resp, Views.REGISTER);
    }

    private static Map<String, String> validate(RegisterRequest form) {
        Map<String, String> errors = new LinkedHashMap<>();
        Validation.firstName(form.firstName(), errors);
        Validation.lastName(form.lastName(), errors);
        Validation.email(form.email(), errors);
        Validation.phone(form.phone(), errors);
        Validation.birthDate(form.birthDate(), errors);
        Validation.address(form.address(), errors);
        Validation.validatePassword(form.password(), form.passwordConfirm(), "motDePasse", "confirmation", errors);
        return errors;
    }
}