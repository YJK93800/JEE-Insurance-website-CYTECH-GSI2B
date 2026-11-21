package com.assurcy2000.util;

public final class Views {
    private static final String BASE = "/WEB-INF/views/";

    public static final String LOGIN = BASE + "auth/login.jsp";
    public static final String REGISTER = BASE + "auth/register.jsp";

    public static final String PROFIL = BASE + "common/profil.jsp";

    public static final String CLIENT_CONTRACTS = BASE + "client/contrats.jsp";
    public static final String CLIENT_CONTRACT = BASE + "client/contrat-detail.jsp";
    public static final String CLIENT_CLAIMS = BASE + "client/sinistres.jsp";
    public static final String CLIENT_CLAIM = BASE + "client/sinistre-detail.jsp";
    public static final String CLIENT_CLAIM_FORM = BASE + "client/sinistre-form.jsp";
    public static final String CLIENT_PAYMENTS = BASE + "client/paiements.jsp";

    public static final String AGENT_CONTRACTS = BASE + "agent/contrats.jsp";
    public static final String AGENT_CONTRACT = BASE + "agent/contrat-form.jsp";
    public static final String AGENT_CLAIMS = BASE + "agent/sinistres.jsp";
    public static final String AGENT_CLAIM = BASE + "agent/claim.jsp";

    public static final String ADMIN_USERS = BASE + "admin/utilisateurs.jsp";
    public static final String STATS = BASE + "shared/stats.jsp";

    private Views() { }
}
