<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/taglibs.jspf" %>
<%-- StatsServlet — GET /admin/stats et GET /agent/stats (même vue)
     Attribut : stats StatsView
       clientCount        long
       contractsByStatus  Map<ContractStatus, Long>  (tous les statuts, 0 compris)
       claimsByStatus     Map<ClaimStatus, Long>     (tous les statuts, 0 compris)
       activePremiums     BigDecimal  somme des primes des contrats actifs
       paymentsReceived   BigDecimal  somme des paiements effectués
       indemnitiesPaid    BigDecimal  somme des indemnisations versées
       revenueByMonth     Map<String, BigDecimal>  ("2026-11" -> encaissements du mois, triés par mois)
     Les indicateurs dérivés (ratio, taux d'acceptation…) sont calculés ici à partir de ces valeurs.
     Les clés des Map sont des enums : on les parcourt avec c:forEach plutôt que map['ACTIF']. --%>
<c:set var="title" value="Statistiques" />
<c:set var="isAdmin" value="${sessionScope.currentUser.role == 'ADMIN'}" />

<%-- Contrats : actifs, expirés, résiliés, total --%>
<c:set var="contractCount" value="${0}" />
<c:forEach items="${stats.contractsByStatus}" var="e">
  <c:set var="contractCount" value="${contractCount + e.value}" />
  <c:if test="${e.key == 'ACTIF'}"><c:set var="activeCount" value="${e.value}" /></c:if>
  <c:if test="${e.key == 'EXPIRE'}"><c:set var="expiredCount" value="${e.value}" /></c:if>
  <c:if test="${e.key == 'RESILIE'}"><c:set var="terminatedCount" value="${e.value}" /></c:if>
</c:forEach>
<%-- Sinistres : total, plus grand compteur (largeur des barres), décisions prises --%>
<c:set var="claimCount" value="${0}" />
<c:set var="maxClaims" value="${0}" />
<c:set var="acceptedCount" value="${0}" />
<c:set var="refusedCount" value="${0}" />
<c:forEach items="${stats.claimsByStatus}" var="e">
  <c:set var="claimCount" value="${claimCount + e.value}" />
  <c:if test="${e.value > maxClaims}"><c:set var="maxClaims" value="${e.value}" /></c:if>
  <c:if test="${e.key == 'ACCEPTE' or e.key == 'INDEMNISE'}"><c:set var="acceptedCount" value="${acceptedCount + e.value}" /></c:if>
  <c:if test="${e.key == 'REFUSE'}"><c:set var="refusedCount" value="${refusedCount + e.value}" /></c:if>
</c:forEach>
<!DOCTYPE html>
<html lang="fr">
<head><%@ include file="/WEB-INF/views/common/head.jspf" %></head>
<body>
<div class="app">
  <jsp:include page="/WEB-INF/views/common/sidebar.jsp"><jsp:param name="active" value="stats" /></jsp:include>
  <main class="main">
    <div class="container">
      <header class="page-head">
        <div class="stack-sm">
          <span class="eyebrow">${isAdmin ? 'Administration' : 'Espace agent'}</span>
          <h1>${isAdmin ? 'Tableau de bord global' : 'Statistiques'}</h1>
        </div>
      </header>

      <h2>Clients et contrats</h2>
      <section class="grid-3" aria-label="Statistiques clients et contrats">
        <div class="kpi"><span class="kpi-label">Clients</span><span class="kpi-value"><fmt:formatNumber value="${stats.clientCount}" /></span></div>
        <div class="kpi"><span class="kpi-label">Contrats actifs</span><span class="kpi-value"><fmt:formatNumber value="${empty activeCount ? 0 : activeCount}" /></span><span class="kpi-sub">sur ${contractCount} contrats</span></div>
        <div class="kpi"><span class="kpi-label">Contrats expirés / résiliés</span><span class="kpi-value">${empty expiredCount ? 0 : expiredCount} <small>/ ${empty terminatedCount ? 0 : terminatedCount}</small></span></div>
      </section>

      <h2>Finances</h2>
      <section class="grid-3" aria-label="Statistiques financières">
        <div class="kpi"><span class="kpi-label">Paiements encaissés</span><span class="kpi-value"><ui:amount value="${stats.paymentsReceived}" /></span></div>
        <div class="kpi"><span class="kpi-label">Primes des contrats actifs</span><span class="kpi-value"><ui:amount value="${stats.activePremiums}" /></span><span class="kpi-sub">par mois</span></div>
        <div class="kpi"><span class="kpi-label">Indemnisations versées</span><span class="kpi-value"><ui:amount value="${stats.indemnitiesPaid}" /></span></div>
      </section>

      <h2 id="indicators">Indicateurs de performance</h2>
      <section class="grid-3" aria-label="Indicateurs de performance">
        <div class="kpi"><span class="kpi-label">Ratio indemnisations / encaissements</span>
          <span class="kpi-value"><c:choose><c:when test="${stats.paymentsReceived > 0}"><fmt:formatNumber value="${stats.indemnitiesPaid / stats.paymentsReceived}" type="percent" /></c:when><c:otherwise>—</c:otherwise></c:choose></span>
          <span class="kpi-sub">Objectif &lt; 60 %</span></div>
        <div class="kpi"><span class="kpi-label">Sinistres déclarés</span><span class="kpi-value"><fmt:formatNumber value="${claimCount}" /></span></div>
        <div class="kpi"><span class="kpi-label">Taux d'acceptation des sinistres</span>
          <span class="kpi-value"><c:choose><c:when test="${acceptedCount + refusedCount > 0}"><fmt:formatNumber value="${acceptedCount / (acceptedCount + refusedCount)}" type="percent" /></c:when><c:otherwise>—</c:otherwise></c:choose></span>
          <span class="kpi-sub">sur les sinistres acceptés ou refusés</span></div>
      </section>

      <div class="row stretch">
        <section class="card col-main" aria-labelledby="h-rev">
          <h2 id="h-rev">Encaissements par mois</h2>
          <c:choose>
            <c:when test="${empty stats.revenueByMonth}"><p class="empty">Aucun paiement encaissé pour le moment.</p></c:when>
            <c:otherwise><div class="chart-box"><canvas id="chart-revenue" role="img" aria-label="Paiements encaissés par mois"></canvas></div></c:otherwise>
          </c:choose>
        </section>
        <section class="card col-side" aria-labelledby="h-statuts">
          <h2 id="h-statuts">Contrats par statut</h2>
          <div class="chart-box"><canvas id="chart-statuses" role="img" aria-label="Répartition des contrats par statut"></canvas></div>
        </section>
      </div>

      <section class="card" aria-labelledby="h-sin">
        <h2 id="h-sin">Sinistres par statut</h2>
        <c:forEach items="${stats.claimsByStatus}" var="e">
          <div class="stack-sm">
            <div class="between small"><ui:status value="${e.key}" /><span class="strong">${e.value}</span></div>
            <div class="bar-track"><div class="bar-fill" style="width: ${maxClaims > 0 ? e.value * 100 / maxClaims : 0}%"></div></div>
          </div>
        </c:forEach>
      </section>
    </div>
  </main>
</div>

<%-- Données des graphiques, lues par stats.js (les clés "contracts" et "indemnities" restent vides : pas de série mensuelle côté serveur) --%>
<script type="application/json" id="stats-data">
{
  "months": [<c:forEach items="${stats.revenueByMonth}" var="m" varStatus="i">"${fn:substring(m.key, 5, 7)}/${fn:substring(m.key, 0, 4)}"${i.last ? '' : ','}</c:forEach>],
  "premiums": [<c:forEach items="${stats.revenueByMonth}" var="m" varStatus="i">${empty m.value ? 0 : m.value}${i.last ? '' : ','}</c:forEach>],
  "indemnities": [],
  "contracts": [],
  "statuses": {<c:forEach items="${stats.contractsByStatus}" var="e" varStatus="i">"${e.key}": ${e.value}${i.last ? '' : ','}</c:forEach>}
}
</script>
<script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.1/dist/chart.umd.min.js"></script>
<script src="${ctx}/js/app.js"></script>
<script src="${ctx}/js/stats.js"></script>
</body>
</html>
