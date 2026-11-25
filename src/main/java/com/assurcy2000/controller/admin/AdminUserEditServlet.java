package com.assurcy2000.controller.admin;

import com.assurcy2000.controller.BaseServlet;
import com.assurcy2000.dto.SessionUser;
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
 * Correction du nom d'un utilisateur par l'admin (admin/user-edit.jsp), quel que soit son rôle.
 *   GET  /admin/users/edit?id=5                    formulaire pré-rempli
 *   POST /admin/users/edit  id, firstName, lastName -> UserService.updateName, retour à /admin/users
 * Son propre compte se modifie dans /profile (le nom y est verrouillé).
 */
@WebServlet("/admin/users/edit")
public class AdminUserEditServlet extends BaseServlet {

    private final UserService userService = new UserService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        SessionUser admin = user(req);
        Long id = idParam(req, "id");
        if (id.equals(admin.id())) {
            redirect(req, resp, "/profile");
            return;
        }
        req.setAttribute("target", userService.getById(admin, id)); // 404 si introuvable
        render(req, resp, Views.ADMIN_USER_EDIT);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        SessionUser admin = user(req);
        Long id = idParam(req, "id");

        // 1. Vérification des saisies par la servlet
        Map<String, String> errors = new LinkedHashMap<>();
        String firstName = Validation.firstName(RequestParams.get(req, "firstName"), errors);
        String lastName = Validation.lastName(RequestParams.get(req, "lastName"), errors);

        // 2. Le service vérifie les droits (admin, pas soi-même)
        if (errors.isEmpty()) {
            try {
                User updated = userService.updateName(admin, id, firstName, lastName);
                UserSession.flash(req, "Nom corrigé : " + updated.getFirstName() + " " + updated.getLastName() + ".", "success");
                redirect(req, resp, "/admin/users");
                return;
            } catch (ValidationException e) {
                errors.putAll(e.getErrors());
            }
        }

        req.setAttribute("target", userService.getById(admin, id));
        req.setAttribute(FormView.ERRORS, errors);
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        render(req, resp, Views.ADMIN_USER_EDIT); // la saisie est réaffichée via param.firstName / param.lastName
    }
}
