<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/taglibs.jspf" %>
<%-- ClientContractsServlet — GET /client/contracts[?status=ACTIF]
     Attributs : contracts      List<Contract> du client connecté, filtrée par statut
                                (client, insuranceType et agent chargés ; les garanties ne sont chargées que sur le détail)
                 statuses       ContractStatus[] (onglets)   selectedStatus ContractStatus ou null (Tous)
                 totalCount     nombre total de contrats du client --%>
<c:set var="title" value="Mes contrats" />
<!DOCTYPE html>
<html lang="fr">
<head><%@ include file="/WEB-INF/views/common/head.jspf" %></head>
<body>
<div class="app">
  <jsp:include page="/WEB-INF/views/common/sidebar.jsp"><jsp:param name="active" value="contracts" /></jsp:include>
  <main class="main">
    <div class="container">
      <header class="page-head">
        <div class="stack-sm"><span class="eyebrow">Espace client</span><h1>Mes contrats</h1></div>
      </header>
      <%@ include file="/WEB-INF/views/common/flash.jspf" %>

      <c:if test="${totalCount > 0}">
        <nav class="tabs" aria-label="Filtrer par statut">
          <a href="${ctx}/client/contracts" class="${empty selectedStatus ? 'active' : ''}">Tous</a>
          <c:forEach items="${statuses}" var="st">
            <a href="${ctx}/client/contracts?status=${st}" class="${selectedStatus == st ? 'active' : ''}"><c:out value="${st.label}" /></a>
          </c:forEach>
        </nav>
      </c:if>

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
        <c:if test="${empty contracts}">
          <div class="card"><p class="empty">
            <c:choose>
              <c:when test="${totalCount == 0}">Vous n'avez aucun contrat.</c:when>
              <c:otherwise>Aucun contrat « <c:out value="${selectedStatus.label}" /> ».</c:otherwise>
            </c:choose>
          </p></div>
        </c:if>
      </section>
    </div>
  </main>
</div>
<script src="${ctx}/js/app.js"></script>
</body>
</html>
