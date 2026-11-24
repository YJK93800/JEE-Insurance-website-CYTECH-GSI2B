<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/taglibs.jspf" %>
<%-- AgentContractsServlet — GET /agent/contracts?id=12 (agent, avec les actions)
                           GET /admin/contracts?id=12 (admin, consultation seule)
     Attributs : contract  Contract (client, insuranceType, agent, guarantees, previousContract chargés)
                 renewal   Contract issu du renouvellement de celui-ci, ou null --%>
<c:set var="title" value="Contrat ${contract.number}" />
<c:set var="isAgent" value="${sessionScope.currentUser.role == 'AGENT'}" />
<c:set var="base" value="${isAgent ? '/agent/contracts' : '/admin/contracts'}" />
<c:set var="s" value="${contract.status}" />
<!DOCTYPE html>
<html lang="fr">
<head><%@ include file="/WEB-INF/views/common/head.jspf" %></head>
<body>
<div class="app">
  <jsp:include page="/WEB-INF/views/common/sidebar.jsp"><jsp:param name="active" value="contracts" /></jsp:include>
  <main class="main">
    <div class="container">
      <header class="page-head">
        <div class="stack-sm">
          <a class="back" href="${ctx}${base}">← Contrats</a>
          <h1><c:out value="${contract.number}" /></h1>
          <span class="muted"><c:out value="${contract.insuranceType.name}" /> · <ui:status value="${s}" label="${s.label}" /></span>
        </div>

        <%-- Mêmes actions et mêmes règles que dans la liste (vérifiées à nouveau par ContractService) --%>
        <c:if test="${isAgent}">
          <div class="inline">
            <c:if test="${s != 'RESILIE' and s != 'EXPIRE'}">
              <a class="btn" href="${ctx}/agent/contracts/edit?id=${contract.id}">Modifier</a>
            </c:if>
            <c:if test="${s == 'EN_ATTENTE' or s == 'SUSPENDU'}">
              <form method="post" action="${ctx}/agent/contracts/action"><%@ include file="/WEB-INF/views/common/csrf.jspf" %><input type="hidden" name="contractId" value="${contract.id}"><input type="hidden" name="action" value="activate"><button class="btn btn-primary" type="submit">Activer</button></form>
            </c:if>
            <c:if test="${s == 'ACTIF'}">
              <form method="post" action="${ctx}/agent/contracts/action"><%@ include file="/WEB-INF/views/common/csrf.jspf" %><input type="hidden" name="contractId" value="${contract.id}"><input type="hidden" name="action" value="suspend"><button class="btn" type="submit">Suspendre</button></form>
            </c:if>
            <c:if test="${(s == 'ACTIF' or s == 'EXPIRE') and empty renewal}">
              <form method="post" action="${ctx}/agent/contracts/action" data-confirm="Créer le contrat de renouvellement de ${fn:escapeXml(contract.number)} ?"><%@ include file="/WEB-INF/views/common/csrf.jspf" %><input type="hidden" name="contractId" value="${contract.id}"><input type="hidden" name="action" value="renew"><button class="btn" type="submit">Renouveler</button></form>
            </c:if>
            <c:if test="${s != 'RESILIE' and s != 'EXPIRE'}">
              <a class="btn btn-danger" href="${ctx}/agent/contracts/terminate?id=${contract.id}">Résilier</a>
            </c:if>
          </div>
        </c:if>
      </header>
      <%@ include file="/WEB-INF/views/common/flash.jspf" %>

      <section class="grid-3" aria-label="Informations du contrat">
        <div class="kpi"><span class="kpi-label">Période</span><span class="strong"><ui:date value="${contract.startDate}" /> → <ui:date value="${contract.endDate}" /></span></div>
        <div class="kpi"><span class="kpi-label">Prime</span><span class="kpi-value"><ui:amount value="${contract.premium}" /></span></div>
        <div class="kpi"><span class="kpi-label">Type d'assurance</span><span class="strong"><c:out value="${contract.insuranceType.name}" /></span>
          <span class="kpi-sub">Prix de base : <ui:amount value="${contract.insuranceType.basePrice}" /></span></div>
      </section>

      <div class="row stretch">
        <section class="card col-main" aria-labelledby="h-gar">
          <h2 id="h-gar">Garanties souscrites</h2>
          <c:choose>
            <c:when test="${empty contract.guarantees}"><p class="empty">Aucune garantie sur ce contrat.</p></c:when>
            <c:otherwise>
              <div class="table-wrap">
                <table class="table">
                  <thead><tr><th>Garantie</th><th>Description</th></tr></thead>
                  <tbody>
                    <c:forEach items="${contract.guarantees}" var="g">
                      <tr><td class="strong"><c:out value="${g.name}" /></td><td class="muted"><c:out value="${g.description}" /></td></tr>
                    </c:forEach>
                  </tbody>
                </table>
              </div>
            </c:otherwise>
          </c:choose>
        </section>

        <div class="col-side">
          <section class="card" aria-labelledby="h-client">
            <h2 id="h-client">Client</h2>
            <p class="strong"><c:out value="${contract.client.firstName} ${contract.client.lastName}" /></p>
            <div class="between small"><span class="muted">Email</span><span><c:out value="${contract.client.email}" /></span></div>
            <c:if test="${not empty contract.client.phone}">
              <div class="between small"><span class="muted">Téléphone</span><span><c:out value="${contract.client.phone}" /></span></div>
            </c:if>
            <c:if test="${not empty contract.agent}">
              <div class="between small"><span class="muted">Agent gestionnaire</span><span><c:out value="${contract.agent.firstName} ${contract.agent.lastName}" /></span></div>
            </c:if>
          </section>

          <section class="card" aria-labelledby="h-hist">
            <h2 id="h-hist">Historique</h2>
            <c:if test="${not empty contract.previousContract}">
              <div class="between small"><span class="muted">Renouvelle</span>
                <a href="${ctx}${base}?id=${contract.previousContract.id}"><c:out value="${contract.previousContract.number}" /></a></div>
            </c:if>
            <c:if test="${not empty renewal}">
              <div class="between small"><span class="muted">Renouvelé par</span>
                <a href="${ctx}${base}?id=${renewal.id}"><c:out value="${renewal.number}" /></a></div>
            </c:if>
            <c:if test="${s == 'RESILIE'}">
              <div class="between small"><span class="muted">Résilié le</span><span><ui:date value="${contract.terminationDate}" /></span></div>
              <p class="small"><span class="muted">Motif :</span> <c:out value="${contract.terminationReason}" /></p>
            </c:if>
            <c:if test="${empty contract.previousContract and empty renewal and s != 'RESILIE'}">
              <p class="small muted">Contrat d'origine, ni renouvelé ni résilié.</p>
            </c:if>
          </section>
        </div>
      </div>
    </div>
  </main>
</div>
<script src="${ctx}/js/app.js"></script>
</body>
</html>
