<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/taglibs.jspf" %>
<%-- ClientPaymentsServlet — GET /client/payments[?filter=paid|pending|due][&contractId=12]
     Attributs : payments   List<Payment> de l'onglet (contrat chargé)
                 summary    PaymentSummary (tous les paiements visibles, avant l'onglet)
                 filters    PaymentFilter[] (onglets : param, label)   filter PaymentFilter sélectionné
                 contractId Long ou null (paiements d'un seul contrat) --%>
<c:set var="title" value="Paiements" />
<!DOCTYPE html>
<html lang="fr">
<head><%@ include file="/WEB-INF/views/common/head.jspf" %></head>
<body>
<div class="app">
  <jsp:include page="/WEB-INF/views/common/sidebar.jsp"><jsp:param name="active" value="payments" /></jsp:include>
  <main class="main">
    <div class="container">
      <header class="page-head">
        <div class="stack-sm">
          <c:if test="${not empty contractId}"><a class="back" href="${ctx}/client/contracts?id=${contractId}">← Retour au contrat</a></c:if>
          <span class="eyebrow">Espace client</span>
          <h1>Paiements<c:if test="${not empty contractId and not empty payments}"> · <c:out value="${payments[0].contract.number}" /></c:if></h1>
        </div>
      </header>
      <%@ include file="/WEB-INF/views/common/flash.jspf" %>

      <%@ include file="/WEB-INF/views/common/payment-kpis.jspf" %>

      <nav class="tabs" aria-label="Filtrer les paiements">
        <c:forEach items="${filters}" var="f">
          <c:url var="tabUrl" value="/client/payments"><c:param name="filter" value="${f.param}" />
            <c:if test="${not empty contractId}"><c:param name="contractId" value="${contractId}" /></c:if></c:url>
          <a href="${fn:escapeXml(tabUrl)}" class="${filter == f ? 'active' : ''}"><c:out value="${f.label}" /></a>
        </c:forEach>
      </nav>

      <section class="card">
        <c:if test="${filter == 'PENDING' and summary.lateCount > 0}">
          <p class="small muted">Les échéances en retard sont à régler auprès de votre agent.</p>
        </c:if>
        <c:set var="paymentList" value="${payments}" />
        <%@ include file="/WEB-INF/views/client/payments-table.jspf" %>
      </section>
    </div>
  </main>
</div>
<script src="${ctx}/js/app.js"></script>
</body>
</html>
