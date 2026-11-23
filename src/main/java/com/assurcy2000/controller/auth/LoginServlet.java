package com.assurcy2000.controller.auth;

import com.assurcy2000.controller.BaseServlet;
import com.assurcy2000.model.User;
import com.assurcy2000.dto.SessionUser;
import com.assurcy2000.security.RoleFilter;
import com.assurcy2000.service.AuthService;
import com.assurcy2000.service.exceptions.AuthException;
import com.assurcy2000.security.UserSession;
import com.assurcy2000.util.RequestParams;
import com.assurcy2000.util.Views;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/login")
public class LoginServlet extends BaseServlet {

    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        SessionUser current = UserSession.currentUser(req);
        if (current != null) { redirect(req, resp, current.homePath()); return; }
        render(req, resp, Views.LOGIN);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String email = RequestParams.get(req, "email");
        try {
            User user = authService.login(email, RequestParams.raw(req, "motDePasse"));
            SessionUser sessionUser = SessionUser.from(user);
            String saved = UserSession.login(req, sessionUser);
            String target = (saved != null && RoleFilter.isAllowed(saved, sessionUser)) ? saved : sessionUser.homePath();
            redirect(req, resp, target);
        } catch (AuthException e) {
            req.setAttribute("erreurGlobale", e.getMessage());
            req.setAttribute("email", email);
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            render(req, resp, Views.LOGIN);
        }
    }
}