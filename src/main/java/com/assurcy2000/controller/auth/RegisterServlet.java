package com.assurcy2000.controller.auth;

import com.assurcy2000.controller.BaseServlet;
import com.assurcy2000.dto.RegisterRequest;
import com.assurcy2000.security.Auth;
import com.assurcy2000.security.SessionUser;
import com.assurcy2000.service.AuthService;
import com.assurcy2000.service.exceptions.ValidationException;
import com.assurcy2000.util.Views;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/register")
public class RegisterServlet extends BaseServlet {

    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        SessionUser current = Auth.current(req);
        if (current != null) { redirect(req, resp, current.homePath()); return; }
        render(req, resp, Views.REGISTER);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        RegisterRequest form = new RegisterRequest(
                param(req, "firstName"), param(req, "lastName"), param(req, "email"), param(req, "phone"),
                param(req, "birthDate"), param(req, "address"), param(req, "password"), param(req, "passwordConfirm"));
        try {
            authService.register(form);
            redirect(req, resp, "/login?registered=1");
        } catch (ValidationException e) {
            req.setAttribute("errors", e.getErrors());         // ${errors.email}, ${errors.password}...
            req.setAttribute("form", new RegisterRequest(      // on réaffiche la saisie SANS les mots de passe
                    form.firstName(), form.lastName(), form.email(), form.phone(),
                    form.birthDate(), form.address(), "", ""));
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            render(req, resp, Views.REGISTER);
        }
    }
}
