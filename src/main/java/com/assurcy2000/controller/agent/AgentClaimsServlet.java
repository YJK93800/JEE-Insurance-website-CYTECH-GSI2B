package com.assurcy2000.controller.agent;

import com.assurcy2000.controller.BaseServlet;
import com.assurcy2000.dto.SessionUser;
import com.assurcy2000.service.ClaimService;
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

@WebServlet({"/agent/claims", "/admin/claims"})
public class AgentClaimsServlet extends BaseServlet {

    private final ClaimService claimService = new ClaimService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        SessionUser user = user(req);
        if (req.getParameter("id") != null) {
            showDetail(req, resp, user, idParam(req, "id"));
        } else {
            req.setAttribute("claims", claimService.listFor(user));
            render(req, resp, Views.AGENT_CLAIMS);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        SessionUser user = user(req);
        Long id = idParam(req, "id");
        try {
            switch (RequestParams.get(req, "action")) {
                case "status" -> claimService.changeStatus(user, id, claimStatusParam(req, "status"));
                case "indemnify" -> claimService.indemnify(user, id, RequestParams.get(req, "amount"));
                case "comment" -> {
                    claimService.addComment(user, id, RequestParams.get(req, "content"));
                    redirect(req, resp, "/agent/claims?id=" + id + "#comments");
                    return;
                }
                default -> throw new ValidationException(Map.of("action", "Action inconnue."));
            }
            redirect(req, resp, "/agent/claims?id=" + id);
        } catch (ValidationException e) {
            req.setAttribute(FormView.ERRORS, e.getErrors());
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            showDetail(req, resp, user, id);
        }
    }

    private void showDetail(HttpServletRequest req, HttpServletResponse resp, SessionUser user, Long id)
            throws ServletException, IOException {
        req.setAttribute("claim", claimService.get(user, id));
        req.setAttribute("history", claimService.history(user, id));
        req.setAttribute("comments", claimService.comments(user, id));
        render(req, resp, Views.AGENT_CLAIM);
    }
}
