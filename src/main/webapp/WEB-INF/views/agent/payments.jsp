<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/taglibs.jspf" %>
<%-- AgentPaymentsServlet — GET  /agent/payments[?filter=paid|pending|due][&q=…][&contractId=12][&pay=ID]
                          POST /agent/payments (paymentId, paymentMethod, paymentDate) -> encaisser une échéance
                          GET  /admin/payments[…] admin : consultation seule
     Attributs : payments   List<Payment> de l'onglet (contract et contract.client chargés)
                 summary    PaymentSummary   filters PaymentFilter[]   filter PaymentFilter   contractId Long ou null
                 today      LocalDate (date proposée dans le formulaire)
                 canWrite   true pour l'agent : formulaire et boutons « Encaisser »
                 payable    List<Payment> échéances proposées dans le formulaire   selectedPaymentId Long ou null
                 paymentMethods PaymentMethod[]   errors Map<String,String> --%>
<c:set var="title" value="Paiements" />
<c:set var="base" value="${canWrite ? '/agent/payments' : '/admin/payments'}" />
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
          <c:if test="${not empty contractId}"><a class="back" href="${ctx}${canWrite ? '/agent/contracts' : '/admin/contracts'}?id=${contractId}">← Retour au contrat</a></c:if>
          <span class="eyebrow">${canWrite ? 'Espace agent' : 'Administration'}</span>
          <h1>Paiements<c:if test="${not empty contractId and not empty payments}"> · <c:out value="${payments[0].contract.number}" /></c:if></h1>
        </div>
        <form class="search" method="get" action="${ctx}${base}" role="search">
          <input type="hidden" name="filter" value="${filter.param}">
          <c:if test="${not empty contractId}"><input type="hidden" name="contractId" value="${contractId}"></c:if>
          <label class="field">N° de contrat ou client<input type="search" name="q" value="${fn:escapeXml(param.q)}"></label>
          <button class="btn" type="submit">Rechercher</button>
          <c:if test="${not empty param.q or not empty contractId}"><a class="btn" href="${ctx}${base}?filter=${filter.param}">Réinitialiser</a></c:if>
        </form>
      </header>
      <%@ include file="/WEB-INF/views/common/flash.jspf" %>

      <%@ include file="/WEB-INF/views/common/payment-kpis.jspf" %>

      <nav class="tabs" aria-label="Filtrer les paiements">
        <%-- Chaque onglet garde la recherche et le contrat en cours --%>
        <c:forEach items="${filters}" var="f">
          <c:url var="tabUrl" value="${base}"><c:param name="filter" value="${f.param}" />
            <c:if test="${not empty contractId}"><c:param name="contractId" value="${contractId}" /></c:if>
            <c:if test="${not empty param.q}"><c:param name="q" value="${param.q}" /></c:if></c:url>
          <a href="${fn:escapeXml(tabUrl)}" class="${filter == f ? 'active' : ''}"><c:out value="${f.label}" /></a>
        </c:forEach>
      </nav>

      <div class="row">
        <section class="card flush col-main" aria-label="Liste des paiements">
          <c:choose>
            <c:when test="${empty payments}"><p class="empty">Aucun paiement.</p></c:when>
            <c:otherwise>
              <div class="table-wrap">
                <table class="table">
                  <thead><tr><th>Échéance</th><th>Client</th><th>Contrat</th><th class="right">Montant</th><th>Statut</th><th>Payé le</th><th>Mode</th>
                    <c:if test="${canWrite}"><th class="right">Action</th></c:if></tr></thead>
                  <tbody>
                    <c:forEach items="${payments}" var="p">
                      <c:set var="unpaid" value="${p.status == 'EN_ATTENTE' or p.status == 'EN_RETARD'}" />
                      <tr class="${p.id == selectedPaymentId ? 'selected' : ''}">
                        <td class="strong"><ui:date value="${p.dueDate}" /></td>
                        <td><c:out value="${p.contract.client.firstName} ${p.contract.client.lastName}" /></td>
                        <td class="muted"><a href="${ctx}${canWrite ? '/agent/contracts' : '/admin/contracts'}?id=${p.contract.id}"><c:out value="${p.contract.number}" /></a></td>
                        <td class="right strong"><ui:amount value="${p.amount}" /></td>
                        <td><ui:status value="${p.status}" label="${p.status.label}" /></td>
                        <td><ui:date value="${p.paymentDate}" /></td>
                        <td class="muted"><c:out value="${empty p.method ? '—' : p.method.label}" /></td>
                        <c:if test="${canWrite}">
                          <td class="right"><c:if test="${unpaid}">
                            <c:url var="payUrl" value="/agent/payments"><c:param name="filter" value="${filter.param}" />
                              <c:if test="${not empty contractId}"><c:param name="contractId" value="${contractId}" /></c:if>
                              <c:if test="${not empty param.q}"><c:param name="q" value="${param.q}" /></c:if>
                              <c:param name="pay" value="${p.id}" /></c:url>
                            <a class="btn btn-sm" href="${fn:escapeXml(payUrl)}#encaisser">Encaisser</a></c:if></td>
                        </c:if>
                      </tr>
                    </c:forEach>
                  </tbody>
                </table>
              </div>
            </c:otherwise>
          </c:choose>
        </section>

        <c:if test="${canWrite}">
          <%-- L'URL garde l'onglet, la recherche et le contrat : en cas d'erreur, la liste réaffichée est la même --%>
          <c:url var="postUrl" value="/agent/payments"><c:param name="filter" value="${filter.param}" />
            <c:if test="${not empty contractId}"><c:param name="contractId" value="${contractId}" /></c:if>
            <c:if test="${not empty param.q}"><c:param name="q" value="${param.q}" /></c:if></c:url>
          <form id="encaisser" class="card form col-side" method="post" action="${fn:escapeXml(postUrl)}" novalidate data-validate>
            <%@ include file="/WEB-INF/views/common/csrf.jspf" %>
            <h2>Enregistrer un paiement</h2>
            <label class="field ${not empty errors.paymentId ? 'has-error' : ''}">Échéance *
              <select name="paymentId" required>
                <option value="">— Choisir —</option>
                <c:forEach items="${payable}" var="p">
                  <option value="${p.id}" ${p.id == selectedPaymentId ? 'selected' : ''}><c:out value="${p.contract.number}" /> · <c:out value="${p.contract.client.lastName}" /> · <ui:date value="${p.dueDate}" /> · <ui:amount value="${p.amount}" /><c:if test="${p.status == 'EN_RETARD'}"> · en retard</c:if></option>
                </c:forEach>
              </select>
              <c:choose>
                <c:when test="${not empty errors.paymentId}"><span class="field-error"><c:out value="${errors.paymentId}" /></span></c:when>
                <c:otherwise><span class="field-help">Échéances non payées jusqu'au mois prochain. Pour une autre, utilisez « Encaisser » dans la liste.</span></c:otherwise>
              </c:choose>
            </label>
            <ui:field name="paymentDate" label="Payé le *" type="date" required="true" value="${empty param.paymentDate ? today : param.paymentDate}" extra="max='${today}'" />
            <label class="field ${not empty errors.paymentMethod ? 'has-error' : ''}">Mode de paiement *
              <select name="paymentMethod" required>
                <option value="">— Choisir —</option>
                <c:forEach items="${paymentMethods}" var="m"><option value="${m}" ${param.paymentMethod == m.name() ? 'selected' : ''}><c:out value="${m.label}" /></option></c:forEach>
              </select>
              <c:if test="${not empty errors.paymentMethod}"><span class="field-error"><c:out value="${errors.paymentMethod}" /></span></c:if>
            </label>
            <button type="submit" class="btn btn-primary btn-block">Enregistrer le paiement</button>
          </form>
        </c:if>
      </div>
    </div>
  </main>
</div>
<script src="${ctx}/js/app.js"></script>
</body>
</html>
