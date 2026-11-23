<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/taglibs.jspf" %>
<%-- GET  /agent/paiements?filtre=effectues|attente|echeances&q=CTR-… — PAS ENCORE DE SERVLET
     POST /agent/paiements (contratId, montant, datePaiement, modePaiement, statut) -> enregistrer un paiement
     Attributs attendus : payments List<Payment> (contract et contract.client chargés)   activeContracts List<Contract>
                          paymentMethods PaymentMethod[]   paymentStatuses PaymentStatus[]   erreurs Map<String,String> --%>
<c:set var="titre" value="Paiements" />
<!DOCTYPE html>
<html lang="fr">
<head><%@ include file="/WEB-INF/views/common/head.jspf" %></head>
<body>
<div class="app">
  <jsp:include page="/WEB-INF/views/common/sidebar.jsp"><jsp:param name="actif" value="paiements" /></jsp:include>
  <main class="main">
    <div class="container">
      <header class="page-head">
        <div class="stack-sm"><span class="eyebrow">Espace agent</span><h1>Paiements</h1></div>
        <form class="search" method="get" action="${ctx}/agent/paiements" role="search">
          <input type="hidden" name="filtre" value="${fn:escapeXml(param.filtre)}">
          <label class="field">N° de contrat ou client<input type="search" name="q" value="${fn:escapeXml(param.q)}"></label>
          <button class="btn" type="submit">Rechercher</button>
        </form>
      </header>
      <%@ include file="/WEB-INF/views/common/flash.jspf" %>

      <nav class="tabs" aria-label="Filtrer les paiements">
        <a href="${ctx}/agent/paiements" class="${empty param.filtre ? 'active' : ''}">Historique</a>
        <a href="${ctx}/agent/paiements?filtre=effectues" class="${param.filtre == 'effectues' ? 'active' : ''}">Effectués</a>
        <a href="${ctx}/agent/paiements?filtre=attente" class="${param.filtre == 'attente' ? 'active' : ''}">En attente</a>
        <a href="${ctx}/agent/paiements?filtre=echeances" class="${param.filtre == 'echeances' ? 'active' : ''}">Échéances</a>
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
                        <td><ui:date valeur="${p.paymentDate}" /></td>
                        <td><c:out value="${p.contract.client.firstName} ${p.contract.client.lastName}" /></td>
                        <td class="muted"><c:out value="${p.contract.number}" /></td>
                        <td class="muted"><c:out value="${p.method}" /></td>
                        <td class="right strong"><ui:montant valeur="${p.amount}" /></td>
                        <td><ui:statut valeur="${p.status}" /></td>
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
          <label class="field ${not empty erreurs.contratId ? 'has-error' : ''}">Contrat *
            <select name="contratId" required data-contrat-prime>
              <option value="">— Choisir —</option>
              <c:forEach items="${activeContracts}" var="ct">
                <option value="${ct.id}" data-prime="${ct.premium}" ${param.contratId == fn:trim(ct.id) ? 'selected' : ''}><c:out value="${ct.number}" /> · <c:out value="${ct.client.lastName}" /></option>
              </c:forEach>
            </select>
            <c:if test="${not empty erreurs.contratId}"><span class="field-error"><c:out value="${erreurs.contratId}" /></span></c:if>
          </label>
          <div class="grid-2">
            <ui:champ nom="montant" libelle="Montant (€) *" type="number" requis="true" extra="min='0.01' step='0.01' data-prime-cible" />
            <ui:champ nom="datePaiement" libelle="Date *" type="date" requis="true" />
          </div>
          <div class="grid-2">
            <label class="field">Mode *
              <select name="modePaiement" required>
                <c:forEach items="${paymentMethods}" var="m"><option value="${m}" ${param.modePaiement == m.name() ? 'selected' : ''}>${m}</option></c:forEach>
              </select>
            </label>
            <label class="field">Statut *
              <select name="statut" required>
                <c:forEach items="${paymentStatuses}" var="st"><option value="${st}" ${param.statut == st.name() ? 'selected' : ''}>${st}</option></c:forEach>
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
