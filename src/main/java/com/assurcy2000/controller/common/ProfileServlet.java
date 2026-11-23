package com.assurcy2000.controller.common;

import com.assurcy2000.controller.BaseServlet;
import com.assurcy2000.dto.SessionUser;
import com.assurcy2000.dto.request.UpdateProfileRequest;
import com.assurcy2000.model.User;
import com.assurcy2000.security.UserSession;
import com.assurcy2000.service.UserService;
import com.assurcy2000.service.exceptions.ValidationException;
import com.assurcy2000.util.FormView;
import com.assurcy2000.util.RequestParams;
import com.assurcy2000.util.Validation;
import com.assurcy2000.util.Views;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Profil de l'utilisateur connecté, commun aux trois rôles : coordonnées et mot de passe.
 * Hérite de BaseServlet : encodage UTF-8, NotFound/Forbidden convertis en 404/403, user(req) jamais null.
 */
@WebServlet("/profile")
public class ProfileServlet extends BaseServlet {

    private final UserService userService = new UserService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        loadUser(req);
        render(req, resp, Views.PROFILE);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Les deux formulaires de common/profile.jsp envoient un champ caché "action"
        switch (RequestParams.get(req, "action")) {
            case "password" -> changePassword(req, resp);
            case "details"  -> updateDetails(req, resp);
            default           -> resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    private void changePassword(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Noms des champs = attributs "name" des <ui:field> de profile.jsp (les erreurs s'affichent sous ces champs)
        String oldPassword = RequestParams.raw(req, "oldPassword");
        String password = RequestParams.raw(req, "password");
        String confirmation = RequestParams.raw(req, "confirmation");

        Map<String, String> errors = new LinkedHashMap<>();
        if (oldPassword.isEmpty()) errors.put("oldPassword", "Le mot de passe actuel est obligatoire.");
        Validation.validatePassword(password, confirmation, "password", "confirmation", errors);

        if (!errors.isEmpty()) {
            showForm(req, resp, errors);
            return;
        }

        try {
            userService.changePassword(user(req), oldPassword, password);
        } catch (ValidationException e) {
            errors.putAll(e.getErrors());
            showForm(req, resp, errors);
            return;
        }

        UserSession.flash(req, "Votre mot de passe a été modifié.", "success");
        redirect(req, resp, "/profile");
    }

    private void updateDetails(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 1. Vérification des saisies par la servlet ; on garde les valeurs nettoyées renvoyées par Validation
        Map<String, String> errors = new LinkedHashMap<>();
        UpdateProfileRequest form = new UpdateProfileRequest(
                Validation.phone(RequestParams.get(req, "phone"), errors),
                Validation.address(RequestParams.get(req, "address"), errors));

        if (!errors.isEmpty()) {
            showForm(req, resp, errors); // les champs ressaisis sont réaffichés via param.xxx dans profile.jsp
            return;
        }

        // 2. Enregistrement (le nom ne change pas : rien à mettre à jour dans la session)
        userService.updateInfos(user(req), form);

        UserSession.flash(req, "Vos informations ont été mises à jour.", "success");
        redirect(req, resp, "/profile");
    }

    /** Réaffiche la page avec les erreurs (et l'utilisateur, pour remplir le formulaire d'informations). */
    private void showForm(HttpServletRequest req, HttpServletResponse resp, Map<String, String> errors)
            throws ServletException, IOException {
        loadUser(req);
        FormView.show(req, resp, Views.PROFILE, errors);
    }

    /** Met l'utilisateur connecté en attribut "user", lu par profile.jsp. */
    private void loadUser(HttpServletRequest req) {
        SessionUser current = user(req);
        User user = userService.getById(current, current.id());
        req.setAttribute("user", user);
    }
}
