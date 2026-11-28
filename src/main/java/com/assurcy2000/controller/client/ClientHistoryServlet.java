package com.assurcy2000.controller.client;

import com.assurcy2000.controller.BaseServlet;
import com.assurcy2000.dto.SessionUser;
import com.assurcy2000.service.HistoryService;
import com.assurcy2000.util.Views;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;


@WebServlet("/client/history")
public class ClientHistoryServlet extends BaseServlet {

    private final HistoryService historyService = new HistoryService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        SessionUser user = user(req);
        req.setAttribute("conversation", historyService.conversation(user));
        req.setAttribute("requests", historyService.requests(user));
        render(req, resp, Views.CLIENT_HISTORY);
    }
}
