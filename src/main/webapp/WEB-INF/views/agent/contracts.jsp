<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/taglibs.jspf" %>
<%-- AgentContractsServlet
     GET  /agent/contracts?q=texte&status=ACTIF&type=3   (recherche de contrats)
     POST /agent/contracts/action (contractId, action = activate | suspend | renew) -> ContractActionServlet
     GET  /agent/contracts/terminate?id=12   -> page de résiliation (date + motif)
     Attributs : contracts List<Contract>  statuses ContractStatus[]  insuranceTypes List<InsuranceType>
     Aussi utilisée par l'admin (/admin/contracts) en consultation seule. --%>
<c:set var="title" value="Contrats" />
<c:set var="isAgent" value="${sessionScope.currentUser.role == 'AGENT'}" />
<c:set var="base" value="${isAgent ? '/agent/contracts' : '/admin/contracts'}" />
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
          <span class="eyebrow">${isAgent ? 'Espace agent' : 'Administration'}</span>
          <h1>${isAgent ? 'Contrats' : 'Tous les contrats'}</h1>
        </div>
        <c:if test="${isAgent}"><a class="btn btn-primary" href="${ctx}/agent/contracts/edit">+ Nouveau contrat</a></c:if>
      </header>
      <%@ include file="/WEB-INF/views/common/flash.jspf" %>

      <form class="card search" method="get" action="${ctx}${base}" role="search">
        <label class="field">N° de contrat ou client
          <input type="search" name="q" value="${fn:escapeXml(param.q)}" placeholder="CTR-2026-…, nom du client">
        </label>
        <label class="field">Statut
          <select name="status">
            <option value="">Tous</option>
            <c:forEach items="${statuses}" var="st"><option value="${st}" ${param.status == st.name() ? 'selected' : ''}>${st.label}</option></c:forEach>
          </select>
        </label>
        <label class="field">Type d'assurance
          <select name="type">
            <option value="">Tous</option>
            <c:forEach items="${insuranceTypes}" var="it"><option value="${it.id}" ${param.type == fn:trim(it.id) ? 'selected' : ''}><c:out value="${it.name}" /></option></c:forEach>
          </select>
        </label>
        <button class="btn btn-primary" type="submit">Rechercher</button>
        <a class="btn" href="${ctx}${base}">Réinitialiser</a>
      </form>

      <section class="card flush" aria-label="Résultats">
        <c:choose>
          <c:when test="${empty contracts}"><p class="empty">Aucun contrat ne correspond à la recherche.</p></c:when>
          <c:otherwise>
            <div class="table-wrap">
              <table class="table">
                <thead><tr><th>N° contrat</th><th>Client</th><th>Type</th><th>Période</th><th class="right">Prime</th><th>Statut</th><th class="right">Actions</th></tr></thead>
                <tbody>
                  <c:forEach items="${contracts}" var="ct">
                    <tr>
                      <td class="strong"><c:out value="${ct.number}" /></td>
                      <td><c:out value="${ct.client.firstName} ${ct.client.lastName}" /></td>
                      <td class="muted"><c:out value="${ct.insuranceType.name}" /></td>
                      <td class="muted small"><ui:date value="${ct.startDate}" /> → <ui:date value="${ct.endDate}" /></td>
                      <td class="right strong"><ui:amount value="${ct.premium}" /></td>
                      <td><ui:status value="${ct.status}" label="${ct.status.label}" /></td>
                      <td>
                        <div class="actions">
                          <c:if test="${isAgent}">
                            <c:set var="s" value="${ct.status}" />
                            <%-- Un contrat résilié ou expiré n'est plus modifiable (règle de ContractService.update) --%>
                            <c:if test="${s != 'RESILIE' and s != 'EXPIRE'}">
                              <a class="btn btn-sm" href="${ctx}/agent/contracts/edit?id=${ct.id}">Modifier</a>
                            </c:if>
                            <c:if test="${s == 'EN_ATTENTE' or s == 'SUSPENDU'}">
                              <form method="post" action="${ctx}/agent/contracts/action"><%@ include file="/WEB-INF/views/common/csrf.jspf" %><input type="hidden" name="contractId" value="${ct.id}"><input type="hidden" name="action" value="activate"><button class="btn-link" type="submit">Activer</button></form>
                            </c:if>
                            <c:if test="${s == 'ACTIF'}">
                              <form method="post" action="${ctx}/agent/contracts/action"><%@ include file="/WEB-INF/views/common/csrf.jspf" %><input type="hidden" name="contractId" value="${ct.id}"><input type="hidden" name="action" value="suspend"><button class="btn-link" type="submit">Suspendre</button></form>
                            </c:if>
                            <c:if test="${s == 'ACTIF' or s == 'EXPIRE'}">
                              <form method="post" action="${ctx}/agent/contracts/action" data-confirm="Créer le contrat de renouvellement de ${fn:escapeXml(ct.number)} ?"><%@ include file="/WEB-INF/views/common/csrf.jspf" %><input type="hidden" name="contractId" value="${ct.id}"><input type="hidden" name="action" value="renew"><button class="btn-link" type="submit">Renouveler</button></form>
                            </c:if>
                            <c:if test="${s != 'RESILIE' and s != 'EXPIRE'}">
                              <%-- La résiliation demande une date et un motif : page de confirmation dédiée --%>
                              <a class="btn-link danger" href="${ctx}/agent/contracts/terminate?id=${ct.id}">Résilier</a>
                            </c:if>
                          </c:if>
                        </div>
                      </td>
                    </tr>
                  </c:forEach>
                </tbody>
              </table>
            </div>
          </c:otherwise>
        </c:choose>
      </section>
    </div>
  </main>
</div>
<script src="${ctx}/js/app.js"></script>
</body>
</html>
