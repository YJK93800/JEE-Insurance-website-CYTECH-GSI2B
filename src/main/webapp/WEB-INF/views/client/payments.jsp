<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/taglibs.jspf" %>
<%-- ClientPaymentsServlet — GET /client/payments            -> tous mes paiements
                             GET /client/payments?contractId=12 -> ceux d'un de mes contrats
     Attributs : payments List<Payment> (le contrat de chaque paiement est chargé) --%>
<c:set var="title" value="Paiements" />
<%-- Totaux calculés dans la page à partir de la liste --%>
<c:set var="totalPaid" value="${0}" />
<c:set var="totalPending" value="${0}" />
<c:forEach items="${payments}" var="p">
  <c:if test="${p.status == 'EFFECTUE'}"><c:set var="totalPaid" value="${totalPaid + p.amount}" /></c:if>
  <c:if test="${p.status == 'EN_ATTENTE' or p.status == 'EN_RETARD'}"><c:set var="totalPending" value="${totalPending + p.amount}" /></c:if>
</c:forEach>
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
          <c:if test="${not empty param.contractId}"><a class="back" href="${ctx}/client/contracts?id=${fn:escapeXml(param.contractId)}">← Retour au contrat</a></c:if>
          <span class="eyebrow">Espace client</span>
          <h1>Paiements</h1>
        </div>
      </header>
      <%@ include file="/WEB-INF/views/common/flash.jspf" %>

      <section class="grid-3" aria-label="Résumé">
        <div class="kpi"><span class="kpi-label">Total payé</span><span class="kpi-value"><ui:amount value="${totalPaid}" /></span></div>
        <div class="kpi"><span class="kpi-label">En attente</span><span class="kpi-value"><ui:amount value="${totalPending}" /></span></div>
        <div class="kpi"><span class="kpi-label">Nombre de paiements</span><span class="kpi-value">${fn:length(payments)}</span></div>
      </section>

      <section class="card">
        <c:set var="paymentList" value="${payments}" />
        <%@ include file="/WEB-INF/views/client/payments-table.jspf" %>
      </section>
    </div>
  </main>
</div>
<script src="${ctx}/js/app.js"></script>
</body>
</html>
