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
       newClientCount     long        clients inscrits depuis 30 jours
       clientsByType      Map<String, Long>  clients ayant un contrat en cours, par type d'assurance
       averageProcessingDays BigDecimal  délai moyen déclaration -> indemnisation (null si aucun sinistre indemnisé)
       months             List<String>  mois ("2026-11") de l'axe commun des graphiques
       revenueByMonth     Map<String, BigDecimal>  ("2026-11" -> encaissements du mois, triés par mois)
       indemnitiesByMonth Map<String, BigDecimal>  indemnisations versées par mois
       contractsByMonth   Map<String, Long>        contrats qui commencent chaque mois
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
<%-- Clients par type : plus grand compteur (largeur des barres) --%>
<c:set var="maxType" value="${0}" />
<c:forEach items="${stats.clientsByType}" var="e"><c:if test="${e.value > maxType}"><c:set var="maxType" value="${e.value}" /></c:if></c:forEach>
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
        <div class="kpi"><span class="kpi-label">Clients</span><span class="kpi-value"><fmt:formatNumber value="${stats.clientCount}" /></span><span class="kpi-sub">dont ${stats.newClientCount} nouveau(x) ces 30 derniers jours</span></div>
        <div class="kpi"><span class="kpi-label">Contrats actifs</span><span class="kpi-value"><fmt:formatNumber value="${empty activeCount ? 0 : activeCount}" /></span><span class="kpi-sub">sur ${contractCount} contrats</span></div>
        <div class="kpi"><span class="kpi-label">Contrats expirés</span><span class="kpi-value"><fmt:formatNumber value="${empty expiredCount ? 0 : expiredCount}" /></span></div>
        <div class="kpi"><span class="kpi-label">Contrats résiliés</span><span class="kpi-value"><fmt:formatNumber value="${empty terminatedCount ? 0 : terminatedCount}" /></span></div>
      </section>

      <h2>Finances</h2>
      <section class="grid-3" aria-label="Statistiques financières">
        <div class="kpi"><span class="kpi-label">Paiements encaissés</span><span class="kpi-value"><ui:amount value="${stats.paymentsReceived}" /></span></div>
        <div class="kpi"><span class="kpi-label">Primes des contrats actifs</span><span class="kpi-value"><ui:amount value="${stats.activePremiums}" /></span><span class="kpi-sub">par mois</span></div>
        <div class="kpi"><span class="kpi-label">Indemnisations versées</span><span class="kpi-value"><ui:amount value="${stats.indemnitiesPaid}" /></span></div>
      </section>

      <%-- Indicateurs de performance : réservés à l'admin (énoncé 2.1) --%>
      <c:if test="${isAdmin}">
        <h2 id="indicators">Indicateurs de performance</h2>
        <section class="grid-3" aria-label="Indicateurs de performance">
          <div class="kpi"><span class="kpi-label">Ratio indemnisations / encaissements</span>
            <span class="kpi-value"><c:choose><c:when test="${stats.paymentsReceived > 0}"><fmt:formatNumber value="${stats.indemnitiesPaid / stats.paymentsReceived}" type="percent" /></c:when><c:otherwise>—</c:otherwise></c:choose></span>
            <span class="kpi-sub">Objectif &lt; 60 %</span></div>
          <div class="kpi"><span class="kpi-label">Sinistres déclarés</span><span class="kpi-value"><fmt:formatNumber value="${claimCount}" /></span></div>
          <div class="kpi"><span class="kpi-label">Taux d'acceptation des sinistres</span>
            <span class="kpi-value"><c:choose><c:when test="${acceptedCount + refusedCount > 0}"><fmt:formatNumber value="${acceptedCount / (acceptedCount + refusedCount)}" type="percent" /></c:when><c:otherwise>—</c:otherwise></c:choose></span>
            <span class="kpi-sub">sur les sinistres acceptés ou refusés</span></div>
          <div class="kpi"><span class="kpi-label">Délai moyen de traitement</span>
            <span class="kpi-value"><c:choose><c:when test="${not empty stats.averageProcessingDays}"><fmt:formatNumber value="${stats.averageProcessingDays}" maxFractionDigits="1" /> j</c:when><c:otherwise>—</c:otherwise></c:choose></span>
            <span class="kpi-sub">de la déclaration à l'indemnisation</span></div>
        </section>
      </c:if>

      <div class="row stretch">
        <section class="card col-main" aria-labelledby="h-rev">
          <h2 id="h-rev">Encaissements et indemnisations par mois</h2>
          <c:choose>
            <c:when test="${empty stats.months}"><p class="empty">Aucun paiement encaissé pour le moment.</p></c:when>
            <c:otherwise><div class="chart-box"><canvas id="chart-revenue" role="img" aria-label="Paiements encaissés par mois"></canvas></div></c:otherwise>
          </c:choose>
        </section>
        <section class="card col-side" aria-labelledby="h-statuts">
          <h2 id="h-statuts">Contrats par statut</h2>
          <div class="chart-box"><canvas id="chart-statuses" role="img" aria-label="Répartition des contrats par statut"></canvas></div>
        </section>
      </div>

      <div class="row stretch">
        <section class="card col-main" aria-labelledby="h-evo">
          <h2 id="h-evo">Évolution des contrats</h2>
          <c:choose>
            <c:when test="${empty stats.contractsByMonth}"><p class="empty">Aucun contrat commencé pour le moment.</p></c:when>
            <c:otherwise><div class="chart-box"><canvas id="chart-contracts" role="img" aria-label="Contrats qui commencent chaque mois"></canvas></div></c:otherwise>
          </c:choose>
        </section>
        <section class="card col-side" aria-labelledby="h-type">
          <h2 id="h-type">Clients par type d'assurance</h2>
          <c:forEach items="${stats.clientsByType}" var="e">
            <div class="stack-sm">
              <div class="between small"><span><c:out value="${e.key}" /></span><span class="strong">${e.value}</span></div>
              <div class="bar-track"><div class="bar-fill" style="width: ${maxType > 0 ? e.value * 100 / maxType : 0}%"></div></div>
            </div>
          </c:forEach>
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

<%-- Données des graphiques, lues par stats.js : même axe "months" pour les trois séries (0 quand un mois n'a rien) --%>
<script type="application/json" id="stats-data">
{
  "months": [<c:forEach items="${stats.months}" var="m" varStatus="i">"${fn:substring(m, 5, 7)}/${fn:substring(m, 0, 4)}"${i.last ? '' : ','}</c:forEach>],
  "premiums": [<c:forEach items="${stats.months}" var="m" varStatus="i">${empty stats.revenueByMonth[m] ? 0 : stats.revenueByMonth[m]}${i.last ? '' : ','}</c:forEach>],
  "indemnities": [<c:forEach items="${stats.months}" var="m" varStatus="i">${empty stats.indemnitiesByMonth[m] ? 0 : stats.indemnitiesByMonth[m]}${i.last ? '' : ','}</c:forEach>],
  "contracts": [<c:forEach items="${stats.months}" var="m" varStatus="i">${empty stats.contractsByMonth[m] ? 0 : stats.contractsByMonth[m]}${i.last ? '' : ','}</c:forEach>],
  "statuses": {<c:forEach items="${stats.contractsByStatus}" var="e" varStatus="i">"${e.key}": ${e.value}${i.last ? '' : ','}</c:forEach>}
}
</script>
<script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.1/dist/chart.umd.min.js"></script>
<script src="${ctx}/js/app.js"></script>
<script src="${ctx}/js/stats.js"></script>
</body>
</html>
