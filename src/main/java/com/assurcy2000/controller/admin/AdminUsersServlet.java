package com.assurcy2000.controller.admin;

import com.assurcy2000.controller.BaseServlet;
import com.assurcy2000.dto.SessionUser;
import com.assurcy2000.service.UserService;
import com.assurcy2000.service.exceptions.ValidationException;
import com.assurcy2000.util.Views;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

@WebServlet("/admin/users")
public class AdminUsersServlet extends BaseServlet {

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
            switch (param(req, "action")) {
                case "active" -> userService.setActive(admin, id, Boolean.parseBoolean(param(req, "value")));
                case "role" -> userService.changeRole(admin, id, param(req, "role"));
                default -> throw new ValidationException(Map.of("action", "Action inconnue."));
            }
            redirect(req, resp, "/admin/users");
        } catch (ValidationException e) {
            req.setAttribute("errors", e.getErrors());
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
