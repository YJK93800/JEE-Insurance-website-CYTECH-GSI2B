<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/taglibs.jspf" %>
<%-- GET  /agent/paiements?filtre=effectues|attente|echeances&q=CTR-…
     POST /agent/paiements (contratId, montant, datePaiement, modePaiement, statut) -> enregistrer un paiement
     Attributs : paiements List<Paiement>  contratsActifs List<Contrat>  modesPaiement ModePaiement[]
                 statutsPaiement StatutPaiement[]  erreurs Map<String,String> --%>
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
            <c:when test="${empty paiements}"><p class="empty">Aucun paiement.</p></c:when>
            <c:otherwise>
              <div class="table-wrap">
                <table class="table">
                  <thead><tr><th>Date</th><th>Client</th><th>Contrat</th><th>Mode</th><th class="right">Montant</th><th>Statut</th></tr></thead>
                  <tbody>
                    <c:forEach items="${paiements}" var="p">
                      <tr>
                        <td><ui:date valeur="${p.datePaiement}" /></td>
                        <td><c:out value="${p.contrat.client.prenom} ${p.contrat.client.nom}" /></td>
                        <td class="muted"><c:out value="${p.contrat.numero}" /></td>
                        <td class="muted"><c:out value="${p.modePaiement}" /></td>
                        <td class="right strong"><ui:montant valeur="${p.montant}" /></td>
                        <td><ui:statut valeur="${p.statut}" /></td>
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
              <c:forEach items="${contratsActifs}" var="ct">
                <option value="${ct.id}" data-prime="${ct.prime}" ${param.contratId == fn:trim(ct.id) ? 'selected' : ''}><c:out value="${ct.numero}" /> · <c:out value="${ct.client.nom}" /></option>
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
                <c:forEach items="${modesPaiement}" var="m"><option value="${m}" ${param.modePaiement == m.name() ? 'selected' : ''}>${m}</option></c:forEach>
              </select>
            </label>
            <label class="field">Statut *
              <select name="statut" required>
                <c:forEach items="${statutsPaiement}" var="st"><option value="${st}" ${param.statut == st.name() ? 'selected' : ''}>${st}</option></c:forEach>
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
