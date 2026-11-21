package com.assurcy2000.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Set;

public class CsrfFilter implements Filter {

    public static final String ATTRIBUTE = "csrfToken";
    public static final String PARAMETER = UserSession.CSRF_TOKEN;
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS");
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        if (req.getServletPath().startsWith("/assets")) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = req.getSession(true);
        String token = (String) session.getAttribute(ATTRIBUTE);
        if (token == null) {
            byte[] bytes = new byte[32];
            RANDOM.nextBytes(bytes);
            token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            session.setAttribute(ATTRIBUTE, token);
        }
        req.setAttribute(ATTRIBUTE, token);

        if (!SAFE_METHODS.contains(req.getMethod())) {
            String sent = req.getParameter(PARAMETER);
            if (sent == null) sent = req.getHeader("X-CSRF-Token");
            boolean ok = sent != null && MessageDigest.isEqual(
                    token.getBytes(StandardCharsets.UTF_8), sent.getBytes(StandardCharsets.UTF_8));
            if (!ok) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Jeton CSRF invalide ou manquant.");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
