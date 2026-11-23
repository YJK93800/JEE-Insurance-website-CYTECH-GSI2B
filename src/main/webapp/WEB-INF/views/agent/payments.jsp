<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/taglibs.jspf" %>
<%-- GET  /agent/paiements?filter=paid|attente|echeances&q=CTR-… — PAS ENCORE DE SERVLET
     POST /agent/paiements (contratId, montant, datePaiement, modePaiement, statut) -> enregistrer un paiement
     Attributs attendus : payments List<Payment> (contract et contract.client chargés)   activeContracts List<Contract>
                          paymentMethods PaymentMethod[]   paymentStatuses PaymentStatus[]   errors Map<String,String> --%>
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
        <div class="stack-sm"><span class="eyebrow">Espace agent</span><h1>Paiements</h1></div>
        <form class="search" method="get" action="${ctx}/agent/paiements" role="search">
          <input type="hidden" name="filter" value="${fn:escapeXml(param.filter)}">
          <label class="field">N° de contrat ou client<input type="search" name="q" value="${fn:escapeXml(param.q)}"></label>
          <button class="btn" type="submit">Rechercher</button>
        </form>
      </header>
      <%@ include file="/WEB-INF/views/common/flash.jspf" %>

      <nav class="tabs" aria-label="Filtrer les paiements">
        <a href="${ctx}/agent/paiements" class="${empty param.filter ? 'active' : ''}">Historique</a>
        <a href="${ctx}/agent/paiements?filter=paid" class="${param.filter == 'paid' ? 'active' : ''}">Effectués</a>
        <a href="${ctx}/agent/paiements?filter=pending" class="${param.filter == 'pending' ? 'active' : ''}">En attente</a>
        <a href="${ctx}/agent/paiements?filter=due" class="${param.filter == 'due' ? 'active' : ''}">Échéances</a>
      </nav>

      <div class="row">
        <section class="card flush col-main" aria-label="Liste des paiements">
          <c:choose>
            <c:when test="${empty payments}"><p class="empty">Aucun paiement.</p></c:when>
            <c:otherwise>
              <div class="table-wrap">
                <table class="table">
                  <thead><tr><th>Date</th><th>Client</th><th>Contrat</th><th>Mode</th><th class="right">Montant</th><th>Statut</th></tr></thead>
                  <tbody>
                    <c:forEach items="${payments}" var="p">
                      <tr>
                        <td><ui:date value="${p.paymentDate}" /></td>
                        <td><c:out value="${p.contract.client.firstName} ${p.contract.client.lastName}" /></td>
                        <td class="muted"><c:out value="${p.contract.number}" /></td>
                        <td class="muted"><c:out value="${p.method}" /></td>
                        <td class="right strong"><ui:amount value="${p.amount}" /></td>
                        <td><ui:status value="${p.status}" /></td>
                      </tr>
                    </c:forEach>
                  </tbody>
                </table>
              </div>
            </c:otherwise>
          </c:choose>
        </section>

        <form class="card form col-side" method="post" action="${ctx}/agent/paiements" novalidate data-validate>
          <%@ include file="/WEB-INF/views/common/csrf.jspf" %>
          <h2>Enregistrer un paiement</h2>
          <label class="field ${not empty errors.contractId ? 'has-error' : ''}">Contrat *
            <select name="contractId" required data-contract-premium>
              <option value="">— Choisir —</option>
              <c:forEach items="${activeContracts}" var="ct">
                <option value="${ct.id}" data-premium="${ct.premium}" ${param.contractId == fn:trim(ct.id) ? 'selected' : ''}><c:out value="${ct.number}" /> · <c:out value="${ct.client.lastName}" /></option>
              </c:forEach>
            </select>
            <c:if test="${not empty errors.contractId}"><span class="field-error"><c:out value="${errors.contractId}" /></span></c:if>
          </label>
          <div class="grid-2">
            <ui:field name="amount" label="Montant (€) *" type="number" required="true" extra="min='0.01' step='0.01' data-premium-target" />
            <ui:field name="paymentDate" label="Date *" type="date" required="true" />
          </div>
          <div class="grid-2">
            <label class="field">Mode *
              <select name="paymentMethod" required>
                <c:forEach items="${paymentMethods}" var="m"><option value="${m}" ${param.paymentMethod == m.name() ? 'selected' : ''}>${m}</option></c:forEach>
              </select>
            </label>
            <label class="field">Statut *
              <select name="status" required>
                <c:forEach items="${paymentStatuses}" var="st"><option value="${st}" ${param.status == st.name() ? 'selected' : ''}>${st}</option></c:forEach>
              </select>
            </label>
          </div>
          <button type="submit" class="btn btn-primary btn-block">Enregistrer</button>
        </form>
      </div>
    </div>
  </main>
</div>
<script src="${ctx}/js/app.js"></script>
</body>
</html>
