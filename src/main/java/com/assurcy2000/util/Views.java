package com.assurcy2000.util;

public final class Views {
    private static final String BASE = "/WEB-INF/views/";

    public static final String LOGIN = BASE + "auth/login.jsp";
    public static final String REGISTER = BASE + "auth/register.jsp";

    public static final String CLIENT_CONTRACTS = BASE + "client/contracts.jsp";
    public static final String CLIENT_CONTRACT = BASE + "client/contract.jsp";
    public static final String CLIENT_CLAIMS = BASE + "client/claims.jsp";
    public static final String CLIENT_CLAIM = BASE + "client/claim.jsp";
    public static final String CLIENT_CLAIM_FORM = BASE + "client/claim-form.jsp";
    public static final String CLIENT_PAYMENTS = BASE + "client/payments.jsp";

    public static final String AGENT_CONTRACTS = BASE + "agent/contracts.jsp";
    public static final String AGENT_CONTRACT = BASE + "agent/contract.jsp";
    public static final String AGENT_CLAIMS = BASE + "agent/claims.jsp";
    public static final String AGENT_CLAIM = BASE + "agent/claim.jsp";

    public static final String ADMIN_USERS = BASE + "admin/users.jsp";
    public static final String STATS = BASE + "shared/stats.jsp";

    private Views() { }
}
