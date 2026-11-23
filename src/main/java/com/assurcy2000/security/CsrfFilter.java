package com.assurcy2000.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;

/**
 * Protection CSRF : toute requête qui modifie quelque chose (POST…) doit renvoyer le jeton de la session,
 * soit dans le champ caché du formulaire (csrf.jspf), soit dans l'en-tête X-CSRF-Token (appels fetch).
 * Le jeton est créé et stocké par UserSession.csrfToken ; ce filtre ne fait que le vérifier.
 */
public class CsrfFilter implements Filter {

    /** Nom du champ caché des formulaires : le même que l'attribut de session. */
    public static final String PARAMETER = UserSession.CSRF_TOKEN;
    public static final String HEADER = "X-CSRF-Token";
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        if (req.getServletPath().startsWith("/assets")) {
            chain.doFilter(request, response);
            return;
        }

        String token = UserSession.csrfToken(req);

        if (!SAFE_METHODS.contains(req.getMethod())) {
            String sent = req.getParameter(PARAMETER);
            if (sent == null) sent = req.getHeader(HEADER);
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
