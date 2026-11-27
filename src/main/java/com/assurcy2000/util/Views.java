package com.assurcy2000.util;

public final class Views {
    private static final String BASE = "/WEB-INF/views/";

    public static final String LOGIN = BASE + "auth/login.jsp";
    public static final String REGISTER = BASE + "auth/register.jsp";

    public static final String PROFILE = BASE + "common/profile.jsp";

    public static final String CLIENT_CONTRACTS = BASE + "client/contracts.jsp";
    public static final String CLIENT_CONTRACT = BASE + "client/contract-detail.jsp";
    public static final String CLIENT_CLAIMS = BASE + "client/claims.jsp";
    public static final String CLIENT_CLAIM = BASE + "client/claim-detail.jsp";
    public static final String CLIENT_CLAIM_FORM = BASE + "client/claim-form.jsp";
    public static final String CLIENT_PAYMENTS = BASE + "client/payments.jsp";
    public static final String CLIENT_GUARANTEES = BASE + "client/guarantees.jsp";

    public static final String AGENT_CLIENTS = BASE + "agent/clients.jsp";
    public static final String AGENT_CONTRACTS = BASE + "agent/contracts.jsp";
    public static final String AGENT_CONTRACT = BASE + "agent/contract-form.jsp";
    public static final String AGENT_CONTRACT_DETAIL = BASE + "agent/contract-detail.jsp";
    public static final String AGENT_CONTRACT_TERMINATE = BASE + "agent/contract-terminate.jsp";
    public static final String AGENT_CLAIMS = BASE + "agent/claims.jsp";
    public static final String AGENT_CLAIM = BASE + "agent/claims.jsp";
    public static final String AGENT_PAYMENTS = BASE + "agent/payments.jsp";
    public static final String AGENT_GUARANTEES = BASE + "agent/guarantees.jsp";

    public static final String ADMIN_USERS = BASE + "admin/users.jsp";
    public static final String ADMIN_USER_EDIT = BASE + "admin/user-edit.jsp";
    public static final String ADMIN_TYPES = BASE + "admin/types.jsp";
    public static final String STATS = BASE + "admin/stats.jsp";

    public static final String CLIENT_ASSISTANT = BASE + "client/assistant.jsp";

    private Views() { }
}
