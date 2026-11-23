<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/taglibs.jspf" %>
<%-- GET /client/dashboard — PAS ENCORE DE SERVLET (vue prête pour le modèle anglais)
     Attributs attendus :
       contracts            List<Contract>  (les 3 plus récents, avec insuranceType et guarantees chargés)
       contractCount        int             activeContractCount int   pendingContractCount int
       totalPremiums        BigDecimal      somme des primes mensuelles des contrats actifs
       openClaimCount       int             lastClaim   Claim (peut être null, avec contract.insuranceType chargé)
       nextPayment          Payment         (peut être null, avec contract.insuranceType chargé)
       recentPayments       List<Payment>   (les 5 plus récents) --%>
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
          <a class="btn btn-primary" href="${ctx}/client/claims?new=1">+ Déclarer un sinistre</a>
        </div>
      </header>

      <%@ include file="/WEB-INF/views/common/flash.jspf" %>

      <c:if test="${not empty nextPayment}">
        <div class="alert alert-warn" role="status">
          <span><strong>Échéance le <ui:date value="${nextPayment.paymentDate}" /></strong>
            · <c:out value="${nextPayment.contract.insuranceType.name}" /> <c:out value="${nextPayment.contract.number}" />
            · <ui:amount value="${nextPayment.amount}" /></span>
          <a href="${ctx}/client/payments?filtre=echeances">Voir l'échéancier</a>
        </div>
      </c:if>

      <section class="grid-3" aria-label="Résumé">
        <div class="kpi">
          <span class="kpi-label">Contrats actifs</span>
          <span class="kpi-value">${activeContractCount} <small>/ ${contractCount}</small></span>
          <span class="kpi-sub">${pendingContractCount} contrat(s) en attente de validation</span>
        </div>
        <div class="kpi">
          <span class="kpi-label">Sinistres en cours</span>
          <span class="kpi-value">${openClaimCount}</span>
          <span class="kpi-sub">
            <c:choose>
              <c:when test="${not empty lastClaim}"><c:out value="${lastClaim.number}" /> · <ui:status value="${lastClaim.status}" /></c:when>
              <c:otherwise>Aucun sinistre déclaré</c:otherwise>
            </c:choose>
          </span>
        </div>
        <div class="kpi">
          <span class="kpi-label">Total des primes / mois</span>
          <span class="kpi-value"><ui:amount value="${totalPremiums}" /></span>
          <span class="kpi-sub">Contrats actifs uniquement</span>
        </div>
      </section>

      <div class="row">
        <section class="card col-main" aria-labelledby="h-contrats">
          <div class="card-head">
            <h2 id="h-contrats">Mes contrats</h2>
            <a href="${ctx}/client/contracts">Tout voir</a>
          </div>
          <c:forEach items="${contracts}" var="ct">
            <article class="item">
              <div class="between">
                <div class="stack-sm">
                  <a class="item-title" href="${ctx}/client/contracts?id=${ct.id}"><c:out value="${ct.insuranceType.name}" /></a>
                  <span class="small muted"><c:out value="${ct.number}" /> · <ui:date value="${ct.startDate}" /> → <ui:date value="${ct.endDate}" /></span>
                </div>
                <div class="inline">
                  <span class="amount"><ui:amount value="${ct.premium}" /> <small>/mois</small></span>
                  <ui:status value="${ct.status}" />
                </div>
              </div>
              <div class="inline">
                <c:forEach items="${ct.guarantees}" var="g"><span class="chip"><c:out value="${g.name}" /></span></c:forEach>
              </div>
            </article>
          </c:forEach>
          <c:if test="${empty contracts}">
            <p class="empty">Vous n'avez pas encore de contrat. Votre agent pourra vous en proposer un.</p>
          </c:if>
        </section>

        <div class="col-side">
          <c:if test="${not empty lastClaim}">
            <section class="card" aria-labelledby="h-suivi">
              <div class="stack-sm">
                <h2 id="h-suivi">Suivi de mon sinistre</h2>
                <span class="small muted"><c:out value="${lastClaim.number}" /> · <ui:claimType value="${lastClaim.type}" /> · <c:out value="${lastClaim.contract.insuranceType.name}" /></span>
              </div>
              <c:set var="sin" value="${lastClaim}" />
              <%@ include file="/WEB-INF/views/common/claim-timeline.jspf" %>
              <a class="strong small" href="${ctx}/client/claims?id=${lastClaim.id}">Voir le détail et les échanges</a>
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
          <a href="${ctx}/client/payments">Historique complet</a>
        </div>
        <c:set var="listePaiements" value="${recentPayments}" />
        <%@ include file="/WEB-INF/views/client/payments-table.jspf" %>
      </section>
    </div>
  </main>
</div>
<script src="${ctx}/js/app.js"></script>
</body>
</html>
