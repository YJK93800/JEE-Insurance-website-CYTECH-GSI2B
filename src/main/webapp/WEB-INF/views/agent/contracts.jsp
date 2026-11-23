<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/taglibs.jspf" %>
<%-- AgentContractsServlet
     GET  /agent/contracts?q=texte&statut=ACTIF&type=3   (recherche de contrats)
     POST /agent/contracts/action (contratId, action = suspendre | resilier | renouveler | activer) -> à brancher
     Attributs : contrats List<Contract>  statuts ContractStatus[]  typesAssurance List<InsuranceType>
     Aussi utilisée par l'admin (/admin/contracts) en consultation seule. --%>
<c:set var="titre" value="Contrats" />
<c:set var="estAgent" value="${sessionScope.utilisateur.role == 'AGENT'}" />
<c:set var="base" value="${estAgent ? '/agent/contracts' : '/admin/contracts'}" />
<!DOCTYPE html>
<html lang="fr">
<head><%@ include file="/WEB-INF/views/common/head.jspf" %></head>
<body>
<div class="app">
  <jsp:include page="/WEB-INF/views/common/sidebar.jsp"><jsp:param name="actif" value="contrats" /></jsp:include>
  <main class="main">
    <div class="container">
      <header class="page-head">
        <div class="stack-sm">
          <span class="eyebrow">${estAgent ? 'Espace agent' : 'Administration'}</span>
          <h1>${estAgent ? 'Contrats' : 'Tous les contrats'}</h1>
        </div>
        <c:if test="${estAgent}"><a class="btn btn-primary" href="${ctx}/agent/contracts/edit">+ Nouveau contrat</a></c:if>
      </header>
      <%@ include file="/WEB-INF/views/common/flash.jspf" %>

      <form class="card search" method="get" action="${ctx}${base}" role="search">
        <label class="field">N° de contrat ou client
          <input type="search" name="q" value="${fn:escapeXml(param.q)}" placeholder="CTR-2026-…, nom du client">
        </label>
        <label class="field">Statut
          <select name="statut">
            <option value="">Tous</option>
            <c:forEach items="${statuts}" var="st"><option value="${st}" ${param.statut == st.name() ? 'selected' : ''}>${st.label}</option></c:forEach>
          </select>
        </label>
        <label class="field">Type d'assurance
          <select name="type">
            <option value="">Tous</option>
            <c:forEach items="${typesAssurance}" var="ta"><option value="${ta.id}" ${param.type == fn:trim(ta.id) ? 'selected' : ''}><c:out value="${ta.name}" /></option></c:forEach>
          </select>
        </label>
        <button class="btn btn-primary" type="submit">Rechercher</button>
        <a class="btn" href="${ctx}${base}">Réinitialiser</a>
      </form>

      <section class="card flush" aria-label="Résultats">
        <c:choose>
          <c:when test="${empty contrats}"><p class="empty">Aucun contrat ne correspond à la recherche.</p></c:when>
          <c:otherwise>
            <div class="table-wrap">
              <table class="table">
                <thead><tr><th>N° contrat</th><th>Client</th><th>Type</th><th>Période</th><th class="right">Prime</th><th>Statut</th><th class="right">Actions</th></tr></thead>
                <tbody>
                  <c:forEach items="${contrats}" var="ct">
                    <tr>
                      <td class="strong"><c:out value="${ct.number}" /></td>
                      <td><c:out value="${ct.client.firstName} ${ct.client.lastName}" /></td>
                      <td class="muted"><c:out value="${ct.insuranceType.name}" /></td>
                      <td class="muted small"><ui:date value="${ct.startDate}" /> → <ui:date value="${ct.endDate}" /></td>
                      <td class="right strong"><ui:amount value="${ct.premium}" /></td>
                      <td><ui:status value="${ct.status}" label="${ct.status.label}" /></td>
                      <td>
                        <div class="actions">
                          <c:if test="${estAgent}">
                            <a class="btn btn-sm" href="${ctx}/agent/contracts/edit?id=${ct.id}">Modifier</a>
                            <c:set var="s" value="${ct.status}" />
                            <c:if test="${s == 'EN_ATTENTE' or s == 'SUSPENDU'}">
                              <form method="post" action="${ctx}/agent/contracts/action"><%@ include file="/WEB-INF/views/common/csrf.jspf" %><input type="hidden" name="contratId" value="${ct.id}"><input type="hidden" name="action" value="activer"><button class="btn-link" type="submit">Activer</button></form>
                            </c:if>
                            <c:if test="${s == 'ACTIF'}">
                              <form method="post" action="${ctx}/agent/contracts/action"><%@ include file="/WEB-INF/views/common/csrf.jspf" %><input type="hidden" name="contratId" value="${ct.id}"><input type="hidden" name="action" value="suspendre"><button class="btn-link" type="submit">Suspendre</button></form>
                            </c:if>
                            <c:if test="${s == 'ACTIF' or s == 'EXPIRE'}">
                              <form method="post" action="${ctx}/agent/contracts/action"><%@ include file="/WEB-INF/views/common/csrf.jspf" %><input type="hidden" name="contratId" value="${ct.id}"><input type="hidden" name="action" value="renouveler"><button class="btn-link" type="submit">Renouveler</button></form>
                            </c:if>
                            <c:if test="${s != 'RESILIE' and s != 'EXPIRE'}">
                              <form method="post" action="${ctx}/agent/contracts/action" data-confirm="Résilier le contrat ${fn:escapeXml(ct.number)} ? Cette action est définitive."><%@ include file="/WEB-INF/views/common/csrf.jspf" %><input type="hidden" name="contratId" value="${ct.id}"><input type="hidden" name="action" value="resilier"><button class="btn-link danger" type="submit">Résilier</button></form>
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
