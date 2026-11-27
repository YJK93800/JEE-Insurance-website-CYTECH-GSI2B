package com.assurcy2000.controller.client;

import com.assurcy2000.ai.AiClient;
import com.assurcy2000.ai.GeminiAiClient;
import com.assurcy2000.controller.BaseServlet;
import com.assurcy2000.dto.SessionUser;
import com.assurcy2000.security.Permission;
import com.assurcy2000.security.Policy;
import com.assurcy2000.service.AssistantService;
import com.assurcy2000.service.exceptions.ForbiddenException;
import com.assurcy2000.service.exceptions.NotFoundException;
import com.assurcy2000.service.exceptions.ValidationException;
import com.assurcy2000.util.Views;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Map;

@WebServlet({"/client/assistant", "/client/assistant/message"})
public class AssistantServlet extends BaseServlet {

    private static final String MESSAGE_PATH = "/client/assistant/message";
    private static final int MAX_BODY_BYTES = 4096;
    private static final long MIN_INTERVAL_MS = 2000;
    private static final String LAST_CALL = "assistantLastCall";
    private static final ObjectMapper JSON = new ObjectMapper();

    private final AssistantService assistant = new AssistantService(new GeminiAiClient());

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (isMessageRoute(req)) {
            resp.setHeader("Allow", "POST");
            resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        SessionUser user = user(req);
        AssistantService.Overview overview = assistant.overview(user);
        req.setAttribute("contractCount", overview.contractCount());
        req.setAttribute("guaranteeCount", overview.guaranteeCount());
        req.setAttribute("claimCount", overview.claimCount());
        req.setAttribute("paymentCount", overview.paymentCount());
        req.setAttribute("referentAgent", overview.referentAgent());
        render(req, resp, Views.CLIENT_ASSISTANT);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!isMessageRoute(req)) {
            resp.setHeader("Allow", "GET");
            resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        SessionUser user = user(req);
        Policy.require(user, Permission.AI_ASSISTANT);

        String message = readMessage(req);
        if (message == null) {
            json(resp, HttpServletResponse.SC_BAD_REQUEST, "error", "Message invalide.");
            return;
        }
        if (tooFast(req)) {
            json(resp, 429, "error", "Merci de patienter quelques secondes entre deux messages.");
            return;
        }

        try {
            json(resp, HttpServletResponse.SC_OK, "reply", assistant.ask(user, message));
        } catch (ValidationException e) {
            json(resp, HttpServletResponse.SC_BAD_REQUEST, "error", e.getErrors().values().iterator().next());
        } catch (AiClient.NotConfiguredException e) {
            getServletContext().log("Assistant IA : " + e.getMessage());
            json(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "error",
                    "L'assistant n'est pas encore disponible.");
        } catch (ForbiddenException | NotFoundException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            getServletContext().log("Assistant IA : échec", e);
            json(resp, HttpServletResponse.SC_BAD_GATEWAY, "error",
                    "L'assistant est indisponible pour le moment. Réessayez plus tard.");
        }
    }

    private static boolean isMessageRoute(HttpServletRequest req) {
        return MESSAGE_PATH.equals(req.getServletPath());
    }

    private static String readMessage(HttpServletRequest req) throws IOException {
        byte[] body = req.getInputStream().readNBytes(MAX_BODY_BYTES + 1);
        if (body.length == 0 || body.length > MAX_BODY_BYTES) return null;
        try {
            JsonNode message = JSON.readTree(body).get("message");
            return message != null && message.isTextual() ? message.asText() : null;
        } catch (IOException e) {
            return null;
        }
    }

    private static boolean tooFast(HttpServletRequest req) {
        HttpSession session = req.getSession();
        long now = System.currentTimeMillis();
        Long last = (Long) session.getAttribute(LAST_CALL);
        if (last != null && now - last < MIN_INTERVAL_MS) return true;
        session.setAttribute(LAST_CALL, now);
        return false;
    }

    private static void json(HttpServletResponse resp, int status, String key, String value) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(JSON.writeValueAsString(Map.of(key, value)));
    }
}