package com.assurcy2000.controller.common;

import com.assurcy2000.dto.SessionUser;
import com.assurcy2000.model.User;
import com.assurcy2000.service.UserService;
import com.assurcy2000.service.exceptions.ValidationException;
import com.assurcy2000.util.FormView;
import com.assurcy2000.util.RequestParams;
import com.assurcy2000.security.UserSession;
import com.assurcy2000.util.Validation;
import com.assurcy2000.util.Views;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@WebServlet("/profil")
public class ProfilServlet extends HttpServlet {

    private static final String VIEW = Views.PROFIL;

    private final UserService userService = new UserService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        loadUser(req);
        req.getRequestDispatcher(VIEW).forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");

        // Les deux formulaires de common/profil.jsp envoient un champ caché "action"
        switch (RequestParams.get(req, "action")) {
            case "motdepasse" -> changePassword(req, resp);
            case "infos"      -> updateInfos(req, resp);
            default           -> resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    private void changePassword(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Noms des champs = attributs "nom" des <ui:champ> de profil.jsp (les erreurs s'affichent sous ces champs)
        String oldPassword = RequestParams.raw(req, "ancien");
        String password = RequestParams.raw(req, "motDePasse");
        String confirmation = RequestParams.raw(req, "confirmation");

        Map<String, String> errors = new LinkedHashMap<>();
        if (oldPassword.isEmpty()) errors.put("ancien", "Le mot de passe actuel est obligatoire.");
        Validation.validatePassword(password, confirmation, "motDePasse", "confirmation", errors);

        if (!errors.isEmpty()) {
            showForm(req, resp, errors);
            return;
        }

        SessionUser current = UserSession.currentUser(req);
        try {
            userService.changePassword(current, oldPassword, password);
        } catch (ValidationException e) {
            errors.putAll(e.getErrors());
            showForm(req, resp, errors);
            return;
        }

        UserSession.flash(req, "Votre mot de passe a été modifié.", "success");
        resp.sendRedirect(req.getContextPath() + "/profil");
    }

    private void updateInfos(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // À faire : lire prenom, nom, telephone, adresse ; valider (mêmes règles que RegisterServlet) ;
        // appeler userService.updateInfos(current, ...) ;
        // remettre à jour la session (le prénom et le nom affichés dans la sidebar) ;
        // puis flash + redirect vers /profil.
        UserSession.flash(req, "Modification des informations pas encore disponible.", "info");
        resp.sendRedirect(req.getContextPath() + "/profil");
    }

    /** Réaffiche la page avec les erreurs (et l'utilisateur, pour remplir le formulaire d'informations). */
    private void showForm(HttpServletRequest req, HttpServletResponse resp, Map<String, String> errors)
            throws ServletException, IOException {
        loadUser(req);
        FormView.show(req, resp, VIEW, errors);
    }

    /** Met l'utilisateur connecté en attribut "user", lu par profil.jsp. */
    private void loadUser(HttpServletRequest req) {
        SessionUser current = UserSession.currentUser(req);
        User user = userService.getById(current, current.id());
        req.setAttribute("user", user);
    }
}