package com.assurcy2000.controller.admin;

import com.assurcy2000.controller.BaseServlet;
import com.assurcy2000.dto.SessionUser;
import com.assurcy2000.model.Role;
import com.assurcy2000.service.UserService;
import com.assurcy2000.service.exceptions.ValidationException;
import com.assurcy2000.util.FormView;
import com.assurcy2000.util.RequestParams;
import com.assurcy2000.util.Views;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

@WebServlet("/admin/users")
public class AdminUsersServlet extends BaseServlet {

    private static final Set<String> ROLES = Set.of(Role.CLIENT, Role.AGENT, Role.ADMIN);

    private final UserService userService = new UserService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        showList(req, resp, user(req));
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        SessionUser admin = user(req);
        try {
            Long id = idParam(req, "id");
            switch (RequestParams.get(req, "action")) {
                case "active" -> {
                    // Vérification de la saisie par la servlet : uniquement true ou false
                    String value = RequestParams.get(req, "value");
                    if (!value.equals("true") && !value.equals("false")) {
                        throw new ValidationException(Map.of("value", "Valeur invalide."));
                    }
                    userService.setActive(admin, id, Boolean.parseBoolean(value));
                }
                case "role" -> {
                    // Vérification de la saisie par la servlet : un des trois rôles
                    String role = RequestParams.get(req, "role");
                    if (!ROLES.contains(role)) throw new ValidationException(Map.of("role", "Rôle inconnu."));
                    userService.changeRole(admin, id, role);
                }
                default -> throw new ValidationException(Map.of("action", "Action inconnue."));
            }
            redirect(req, resp, "/admin/users");
        } catch (ValidationException e) {
            req.setAttribute(FormView.ERRORS, e.getErrors());
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            showList(req, resp, admin);
        }
    }

    private void showList(HttpServletRequest req, HttpServletResponse resp, SessionUser admin)
            throws ServletException, IOException {
        req.setAttribute("users", userService.listAll(admin));
        render(req, resp, Views.ADMIN_USERS);
    }
}
