<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/taglibs.jspf" %>
<%-- GET /client/paiements?filtre=effectues|attente|echeances   (sans filtre = historique complet)
     Attributs : paiements List<Paiement> (déjà filtrés)  totalPaye BigDecimal  totalEnAttente BigDecimal  prochaineEcheance Paiement --%>
<c:set var="titre" value="Paiements" />
<!DOCTYPE html>
<html lang="fr">
<head><%@ include file="/WEB-INF/views/common/head.jspf" %></head>
<body>
<div class="app">
  <jsp:include page="/WEB-INF/views/common/sidebar.jsp"><jsp:param name="actif" value="paiements" /></jsp:include>
  <main class="main">
    <div class="container">
      <header class="page-head"><div class="stack-sm"><span class="eyebrow">Espace client</span><h1>Paiements</h1></div></header>
      <%@ include file="/WEB-INF/views/common/flash.jspf" %>

      <section class="grid-3" aria-label="Résumé">
        <div class="kpi"><span class="kpi-label">Payé sur 12 mois</span><span class="kpi-value"><ui:montant valeur="${totalPaye}" /></span></div>
        <div class="kpi"><span class="kpi-label">En attente</span><span class="kpi-value"><ui:montant valeur="${totalEnAttente}" /></span></div>
        <div class="kpi"><span class="kpi-label">Prochaine échéance</span>
          <span class="kpi-value"><c:choose><c:when test="${not empty prochaineEcheance}"><ui:date valeur="${prochaineEcheance.datePaiement}" /></c:when><c:otherwise>—</c:otherwise></c:choose></span>
          <c:if test="${not empty prochaineEcheance}"><span class="kpi-sub"><ui:montant valeur="${prochaineEcheance.montant}" /> · <c:out value="${prochaineEcheance.contrat.numero}" /></span></c:if>
        </div>
      </section>

      <nav class="tabs" aria-label="Filtrer les paiements">
        <a href="${ctx}/client/paiements" class="${empty param.filtre ? 'active' : ''}">Historique</a>
        <a href="${ctx}/client/paiements?filtre=effectues" class="${param.filtre == 'effectues' ? 'active' : ''}">Effectués</a>
        <a href="${ctx}/client/paiements?filtre=attente" class="${param.filtre == 'attente' ? 'active' : ''}">En attente</a>
        <a href="${ctx}/client/paiements?filtre=echeances" class="${param.filtre == 'echeances' ? 'active' : ''}">Échéances</a>
      </nav>

      <section class="card">
        <c:set var="listePaiements" value="${paiements}" />
        <%@ include file="/WEB-INF/views/client/table-paiements.jspf" %>
      </section>
    </div>
  </main>
</div>
<script src="${ctx}/js/app.js"></script>
</body>
</html>
