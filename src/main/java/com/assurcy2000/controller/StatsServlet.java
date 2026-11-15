package com.assurcy2000.controller;

import com.assurcy2000.service.StatsService;
import com.assurcy2000.util.Views;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet({"/agent/stats", "/admin/stats"})
public class StatsServlet extends BaseServlet {

    private final StatsService statsService = new StatsService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setAttribute("stats", statsService.dashboard(user(req)));
        render(req, resp, Views.STATS);
    }
}
