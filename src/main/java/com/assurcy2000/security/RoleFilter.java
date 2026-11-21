package com.assurcy2000.security;

import com.assurcy2000.dto.SessionUser;
import com.assurcy2000.model.Role;
import com.assurcy2000.security.UserSession;
import jakarta.servlet.*;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.Map;
import java.util.Set;


public class RoleFilter implements Filter {

    private static final Map<String, Set<String>> RULES = Map.of(
            "/client/", Set.of(Role.CLIENT),
            "/agent/", Set.of(Role.AGENT, Role.ADMIN),
            "/admin/", Set.of(Role.ADMIN));

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        String path = UserSession.currentPath(req) + "/";
        SessionUser user = UserSession.currentUser(req);

        for (var rule : RULES.entrySet()) {
            if (path.startsWith(rule.getKey())) {
                if (user == null || !rule.getValue().contains(user.role())) {
                    resp.sendError(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }
                break;
            }
        }
        chain.doFilter(request, response);
    }
}
