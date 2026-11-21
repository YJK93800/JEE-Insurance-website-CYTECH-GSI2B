<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/taglibs.jspf" %>
<%-- GET /client/dashboard
     Attributs fournis par la servlet :
       contrats            List<Contrat>   (les 3 plus récents)
       nbContrats          int             nbContratsActifs  int   nbContratsEnAttente int
       totalPrimes         BigDecimal      somme des primes mensuelles des contrats actifs
       nbSinistresEnCours  int             dernierSinistre   Sinistre (peut être null)
       prochaineEcheance   Paiement        (peut être null)
       derniersPaiements   List<Paiement>  (les 5 plus récents) --%>
<c:set var="titre" value="Tableau de bord" />
<!DOCTYPE html>
<html lang="fr">
<head><%@ include file="/WEB-INF/views/common/head.jspf" %></head>
<body>
<div class="app">
  <jsp:include page="/WEB-INF/views/common/sidebar.jsp"><jsp:param name="actif" value="dashboard" /></jsp:include>

  <main class="main">
    <div class="container">
      <header class="page-head">
        <div class="stack-sm">
          <span class="eyebrow">Espace client</span>
          <h1>Bonjour <c:out value="${sessionScope.utilisateur.prenom}" /></h1>
        </div>
        <div class="inline">
          <a class="btn" href="${ctx}/client/assistant">Poser une question</a>
          <a class="btn btn-primary" href="${ctx}/client/sinistres/declarer">+ Déclarer un sinistre</a>
        </div>
      </header>

      <%@ include file="/WEB-INF/views/common/flash.jspf" %>

      <c:if test="${not empty prochaineEcheance}">
        <div class="alert alert-warn" role="status">
          <span><strong>Échéance le <ui:date valeur="${prochaineEcheance.datePaiement}" /></strong>
            · <c:out value="${prochaineEcheance.contrat.typeAssurance.nom}" /> <c:out value="${prochaineEcheance.contrat.numero}" />
            · <ui:montant valeur="${prochaineEcheance.montant}" /></span>
          <a href="${ctx}/client/paiements?filtre=echeances">Voir l'échéancier</a>
        </div>
      </c:if>

      <section class="grid-3" aria-label="Résumé">
        <div class="kpi">
          <span class="kpi-label">Contrats actifs</span>
          <span class="kpi-value">${nbContratsActifs} <small>/ ${nbContrats}</small></span>
          <span class="kpi-sub">${nbContratsEnAttente} contrat(s) en attente de validation</span>
        </div>
        <div class="kpi">
          <span class="kpi-label">Sinistres en cours</span>
          <span class="kpi-value">${nbSinistresEnCours}</span>
          <span class="kpi-sub">
            <c:choose>
              <c:when test="${not empty dernierSinistre}"><c:out value="${dernierSinistre.numero}" /> · <ui:statut valeur="${dernierSinistre.statut}" /></c:when>
              <c:otherwise>Aucun sinistre déclaré</c:otherwise>
            </c:choose>
          </span>
        </div>
        <div class="kpi">
          <span class="kpi-label">Total des primes / mois</span>
          <span class="kpi-value"><ui:montant valeur="${totalPrimes}" /></span>
          <span class="kpi-sub">Contrats actifs uniquement</span>
        </div>
      </section>

      <div class="row">
        <section class="card col-main" aria-labelledby="h-contrats">
          <div class="card-head">
            <h2 id="h-contrats">Mes contrats</h2>
            <a href="${ctx}/client/contrats">Tout voir</a>
          </div>
          <c:forEach items="${contrats}" var="ct">
            <article class="item">
              <div class="between">
                <div class="stack-sm">
                  <a class="item-title" href="${ctx}/client/contrats/detail?id=${ct.id}"><c:out value="${ct.typeAssurance.nom}" /></a>
                  <span class="small muted"><c:out value="${ct.numero}" /> · <ui:date valeur="${ct.dateDebut}" /> → <ui:date valeur="${ct.dateFin}" /></span>
                </div>
                <div class="inline">
                  <span class="amount"><ui:montant valeur="${ct.prime}" /> <small>/mois</small></span>
                  <ui:statut valeur="${ct.statut}" />
                </div>
              </div>
              <div class="inline">
                <c:forEach items="${ct.garanties}" var="g"><span class="chip"><c:out value="${g.nom}" /></span></c:forEach>
              </div>
            </article>
          </c:forEach>
          <c:if test="${empty contrats}">
            <p class="empty">Vous n'avez pas encore de contrat. Votre agent pourra vous en proposer un.</p>
          </c:if>
        </section>

        <div class="col-side">
          <c:if test="${not empty dernierSinistre}">
            <section class="card" aria-labelledby="h-suivi">
              <div class="stack-sm">
                <h2 id="h-suivi">Suivi de mon sinistre</h2>
                <span class="small muted"><c:out value="${dernierSinistre.numero}" /> · <c:out value="${dernierSinistre.typeSinistre.libelle}" /> · <c:out value="${dernierSinistre.contrat.typeAssurance.nom}" /></span>
              </div>
              <c:set var="sin" value="${dernierSinistre}" />
              <%@ include file="/WEB-INF/views/common/suivi-sinistre.jspf" %>
              <a class="strong small" href="${ctx}/client/sinistres/detail?id=${dernierSinistre.id}">Voir le détail et les échanges</a>
            </section>
          </c:if>

          <section class="card dark" aria-labelledby="h-ia">
            <h2 id="h-ia">Une question ?</h2>
            <p class="small muted">L'assistant connaît vos contrats, garanties et sinistres.</p>
            <div class="stack">
              <c:forEach items="${['Où en est mon dernier sinistre ?', 'Quels sont mes contrats actifs ?', 'Comment déclarer un sinistre ?']}" var="question">
                <c:url var="lienQ" value="/client/assistant"><c:param name="q" value="${question}" /></c:url>
                <a class="quick-q" href="${lienQ}">${question}</a>
              </c:forEach>
            </div>
          </section>
        </div>
      </div>

      <section class="card" aria-labelledby="h-paiements">
        <div class="card-head">
          <h2 id="h-paiements">Derniers paiements</h2>
          <a href="${ctx}/client/paiements">Historique complet</a>
        </div>
        <c:set var="listePaiements" value="${derniersPaiements}" />
        <%@ include file="/WEB-INF/views/client/table-paiements.jspf" %>
      </section>
    </div>
  </main>
</div>
<script src="${ctx}/js/app.js"></script>
</body>
</html>
