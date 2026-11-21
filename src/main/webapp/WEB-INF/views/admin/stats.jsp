<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/taglibs.jspf" %>
<%-- GET /admin/stats?periode=12m|annee|mois   (même vue pour GET /agent/stats)
     POST /admin/stats/analyse -> regénère la synthèse IA (envoie uniquement des agrégats à l'API)
     Attributs :
       nbClients, nbNouveauxClients                         long
       nbContratsActifs, nbContratsExpires, nbContratsResilies long
       chiffreAffaires, montantPrimes, montantIndemnisations  BigDecimal
       ratioSinistresPrimes (double, ex. 0.53)  delaiMoyenTraitement (double, jours)  tauxAcceptation (double, ex. 0.78)
       evolution          List<StatMois>{mois "nov. 2026", primes, indemnisations, nbContrats}
       clientsParType     Map<String,Long>  (LinkedHashMap, nom du type -> nb de clients)
       contratsParStatut  Map<String,Long>  (nom de l'enum -> nombre)
       maxClientsParType  long  (plus grande valeur de clientsParType, pour la largeur des barres)
       analyseIA          List<String> (points de la synthèse, peut être vide) --%>
<c:set var="titre" value="Statistiques" />
<c:set var="estAdmin" value="${sessionScope.utilisateur.role == 'ADMIN'}" />
<c:set var="base" value="${estAdmin ? '/admin/stats' : '/agent/stats'}" />
<!DOCTYPE html>
<html lang="fr">
<head><%@ include file="/WEB-INF/views/common/head.jspf" %></head>
<body>
<div class="app">
  <jsp:include page="/WEB-INF/views/common/sidebar.jsp"><jsp:param name="actif" value="stats" /></jsp:include>
  <main class="main">
    <div class="container">
      <header class="page-head">
        <div class="stack-sm">
          <span class="eyebrow">${estAdmin ? 'Administration' : 'Espace agent'}</span>
          <h1>${estAdmin ? 'Tableau de bord global' : 'Statistiques'}</h1>
        </div>
        <form class="search" method="get" action="${ctx}${base}">
          <label class="field">Période
            <select name="periode" data-autosubmit>
              <option value="12m"   ${empty param.periode or param.periode == '12m' ? 'selected' : ''}>12 derniers mois</option>
              <option value="annee" ${param.periode == 'annee' ? 'selected' : ''}>Année en cours</option>
              <option value="mois"  ${param.periode == 'mois' ? 'selected' : ''}>Ce mois-ci</option>
            </select>
          </label>
          <noscript><button class="btn" type="submit">Appliquer</button></noscript>
        </form>
      </header>

      <h2>Clients et contrats</h2>
      <section class="grid-3" aria-label="Statistiques clients et contrats">
        <div class="kpi"><span class="kpi-label">Clients</span><span class="kpi-value"><fmt:formatNumber value="${nbClients}" /></span><span class="delta-up">+${nbNouveauxClients} nouveaux sur la période</span></div>
        <div class="kpi"><span class="kpi-label">Contrats actifs</span><span class="kpi-value"><fmt:formatNumber value="${nbContratsActifs}" /></span></div>
        <div class="kpi"><span class="kpi-label">Contrats expirés / résiliés</span><span class="kpi-value">${nbContratsExpires} <small>/ ${nbContratsResilies}</small></span></div>
      </section>

      <h2>Finances</h2>
      <section class="grid-3" aria-label="Statistiques financières">
        <div class="kpi"><span class="kpi-label">Chiffre d'affaires</span><span class="kpi-value"><ui:montant valeur="${chiffreAffaires}" /></span></div>
        <div class="kpi"><span class="kpi-label">Montant des primes</span><span class="kpi-value"><ui:montant valeur="${montantPrimes}" /></span></div>
        <div class="kpi"><span class="kpi-label">Indemnisations versées</span><span class="kpi-value"><ui:montant valeur="${montantIndemnisations}" /></span></div>
      </section>

      <c:if test="${estAdmin}">
        <h2 id="indicateurs">Indicateurs de performance</h2>
        <section class="grid-3" aria-label="Indicateurs de performance">
          <div class="kpi"><span class="kpi-label">Ratio sinistres / primes</span><span class="kpi-value"><fmt:formatNumber value="${ratioSinistresPrimes}" type="percent" /></span><span class="kpi-sub">Objectif &lt; 60 %</span></div>
          <div class="kpi"><span class="kpi-label">Délai moyen de traitement</span><span class="kpi-value"><fmt:formatNumber value="${delaiMoyenTraitement}" maxFractionDigits="1" /> <small>jours</small></span></div>
          <div class="kpi"><span class="kpi-label">Taux d'acceptation des sinistres</span><span class="kpi-value"><fmt:formatNumber value="${tauxAcceptation}" type="percent" /></span></div>
        </section>
      </c:if>

      <div class="row stretch">
        <section class="card col-main" aria-labelledby="h-rev">
          <h2 id="h-rev">Évolution des revenus et des indemnisations</h2>
          <div class="chart-box"><canvas id="chart-revenus" role="img" aria-label="Primes encaissées et indemnisations par mois"></canvas></div>
        </section>
        <section class="card col-side" aria-labelledby="h-types">
          <h2 id="h-types">Clients par type d'assurance</h2>
          <c:forEach items="${clientsParType}" var="e">
            <div class="stack-sm">
              <div class="between small"><span><c:out value="${e.key}" /></span><span class="strong">${e.value}</span></div>
              <div class="bar-track"><div class="bar-fill" style="width: ${maxClientsParType > 0 ? e.value * 100 / maxClientsParType : 0}%"></div></div>
            </div>
          </c:forEach>
        </section>
      </div>

      <div class="row stretch">
        <section class="card col-main" aria-labelledby="h-evol">
          <h2 id="h-evol">Évolution du nombre de contrats</h2>
          <div class="chart-box"><canvas id="chart-contrats" role="img" aria-label="Nombre de contrats par mois"></canvas></div>
        </section>
        <section class="card col-side" aria-labelledby="h-statuts">
          <h2 id="h-statuts">Contrats par statut</h2>
          <div class="chart-box"><canvas id="chart-statuts" role="img" aria-label="Répartition des contrats par statut"></canvas></div>
        </section>
      </div>

      <c:if test="${estAdmin}">
        <section class="card dark" aria-labelledby="h-ia">
          <div class="card-head">
            <h2 id="h-ia">Analyse générée par IA</h2>
            <form method="post" action="${ctx}/admin/stats/analyse">
              <%@ include file="/WEB-INF/views/common/csrf.jspf" %>
              <input type="hidden" name="periode" value="${fn:escapeXml(param.periode)}">
              <button type="submit" class="btn btn-sm">Régénérer</button>
            </form>
          </div>
          <c:choose>
            <c:when test="${empty analyseIA}"><p class="small muted">Aucune analyse pour cette période. Cliquez sur « Régénérer ».</p></c:when>
            <c:otherwise><ul class="stack small"><c:forEach items="${analyseIA}" var="pt"><li><c:out value="${pt}" /></li></c:forEach></ul></c:otherwise>
          </c:choose>
          <span class="xsmall muted">Synthèse calculée sur les agrégats affichés, sans données personnelles.</span>
        </section>
      </c:if>
    </div>
  </main>
</div>

<%-- Données des graphiques, lues par stats.js --%>
<script type="application/json" id="stats-data">
{
  "mois": [<c:forEach items="${evolution}" var="m" varStatus="i">"${fn:escapeXml(m.mois)}"${i.last ? '' : ','}</c:forEach>],
  "primes": [<c:forEach items="${evolution}" var="m" varStatus="i">${empty m.primes ? 0 : m.primes}${i.last ? '' : ','}</c:forEach>],
  "indemnisations": [<c:forEach items="${evolution}" var="m" varStatus="i">${empty m.indemnisations ? 0 : m.indemnisations}${i.last ? '' : ','}</c:forEach>],
  "contrats": [<c:forEach items="${evolution}" var="m" varStatus="i">${empty m.nbContrats ? 0 : m.nbContrats}${i.last ? '' : ','}</c:forEach>],
  "statuts": {<c:forEach items="${contratsParStatut}" var="e" varStatus="i">"${fn:escapeXml(e.key)}": ${e.value}${i.last ? '' : ','}</c:forEach>}
}
</script>
<script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.1/dist/chart.umd.min.js"></script>
<script src="${ctx}/js/app.js"></script>
<script src="${ctx}/js/stats.js"></script>
</body>
</html>
