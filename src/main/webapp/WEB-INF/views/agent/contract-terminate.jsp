<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/taglibs.jspf" %>
<%-- ContractActionServlet
     GET  /agent/contracts/terminate?id=12   -> cette page
     POST /agent/contracts/terminate (id, terminationDate, terminationReason) -> résiliation, retour à la liste
     Attributs : contract                Contract à résilier
                 defaultTerminationDate  LocalDate (aujourd'hui, ramené dans la période du contrat)
                 errors                  Map<String,String> --%>
<c:set var="title" value="Résilier le contrat" />
<!DOCTYPE html>
<html lang="fr">
<head><%@ include file="/WEB-INF/views/common/head.jspf" %></head>
<body>
<div class="app">
  <jsp:include page="/WEB-INF/views/common/sidebar.jsp"><jsp:param name="active" value="contracts" /></jsp:include>
  <main class="main">
    <div class="container">
      <header class="stack-sm">
        <a class="back" href="${ctx}/agent/contracts">← Contrats</a>
        <h1>${title}</h1>
        <span class="muted"><c:out value="${contract.number}" /> · <ui:status value="${contract.status}" label="${contract.status.label}" /></span>
      </header>
      <%@ include file="/WEB-INF/views/common/flash.jspf" %>

      <form class="card form" method="post" action="${ctx}/agent/contracts/terminate" novalidate data-validate>
        <%@ include file="/WEB-INF/views/common/csrf.jspf" %>
        <input type="hidden" name="id" value="${contract.id}">

        <p>
          Contrat <strong><c:out value="${contract.insuranceType.name}" /></strong> de
          <strong><c:out value="${contract.client.firstName} ${contract.client.lastName}" /></strong>,
          du <ui:date value="${contract.startDate}" /> au <ui:date value="${contract.endDate}" />.
          <br><span class="muted">La résiliation est définitive : le contrat ne pourra plus être modifié, activé ni renouvelé.</span>
        </p>

        <c:if test="${not empty errors.status}"><p class="field-error"><c:out value="${errors.status}" /></p></c:if>

        <div class="grid-2">
          <ui:field name="terminationDate" label="Date de résiliation *" type="date" required="true"
                    value="${not empty param.terminationDate ? param.terminationDate : defaultTerminationDate}"
                    extra="min='${contract.startDate}' max='${contract.endDate}'"
                    help="Comprise entre le début et la fin du contrat." />
        </div>
        <ui:field name="terminationReason" label="Motif *" required="true"
                  extra="maxlength='255' placeholder='Ex. : vente du véhicule, demande du client…'" />

        <div class="form-actions">
          <a class="btn" href="${ctx}/agent/contracts">Annuler</a>
          <button type="submit" class="btn btn-danger">Résilier le contrat</button>
        </div>
      </form>
    </div>
  </main>
</div>
<script src="${ctx}/js/app.js"></script>
</body>
</html>
