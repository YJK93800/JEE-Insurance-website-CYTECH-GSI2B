<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/taglibs.jspf" %>
<%-- ClientContractsServlet — GET /client/contracts
     Attributs : contracts List<Contract> du client connecté (client, insuranceType et agent chargés ;
                 les garanties ne sont chargées que sur le détail). --%>
<c:set var="titre" value="Mes contrats" />
<!DOCTYPE html>
<html lang="fr">
<head><%@ include file="/WEB-INF/views/common/head.jspf" %></head>
<body>
<div class="app">
  <jsp:include page="/WEB-INF/views/common/sidebar.jsp"><jsp:param name="actif" value="contrats" /></jsp:include>
  <main class="main">
    <div class="container">
      <header class="page-head">
        <div class="stack-sm"><span class="eyebrow">Espace client</span><h1>Mes contrats</h1></div>
      </header>
      <%@ include file="/WEB-INF/views/common/flash.jspf" %>

      <section class="stack" aria-label="Liste des contrats">
        <c:forEach items="${contracts}" var="ct">
          <article class="card">
            <div class="between">
              <div class="stack-sm">
                <h2><c:out value="${ct.insuranceType.name}" /></h2>
                <span class="small muted"><c:out value="${ct.number}" /> · du <ui:date value="${ct.startDate}" /> au <ui:date value="${ct.endDate}" /></span>
              </div>
              <div class="inline">
                <span class="amount"><ui:amount value="${ct.premium}" /> <small>/mois</small></span>
                <ui:status value="${ct.status}" label="${ct.status.label}" />
              </div>
            </div>
            <div class="inline">
              <a class="btn btn-sm" href="${ctx}/client/contracts?id=${ct.id}">Voir le détail</a>
              <a class="btn btn-sm" href="${ctx}/client/payments?contractId=${ct.id}">Paiements</a>
              <c:if test="${ct.status == 'ACTIF'}">
                <a class="btn btn-sm" href="${ctx}/client/claims?new=1&amp;contractId=${ct.id}">Déclarer un sinistre</a>
              </c:if>
            </div>
          </article>
        </c:forEach>
        <c:if test="${empty contracts}"><div class="card"><p class="empty">Vous n'avez aucun contrat.</p></div></c:if>
      </section>
    </div>
  </main>
</div>
<script src="${ctx}/js/app.js"></script>
</body>
</html>
