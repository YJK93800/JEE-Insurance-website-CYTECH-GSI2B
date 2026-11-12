<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/taglibs.jspf" %>
<%-- GET /client/contrats/detail?id=12  (la servlet vérifie que le contrat appartient bien au client connecté)
     Attributs : contrat   Contrat (numero, typeAssurance{nom, description}, dateDebut, dateFin, prime, statut,
                                    garanties List<Garantie>{nom, description, plafond, franchise})
                 paiements List<Paiement>  sinistres List<Sinistre> --%>
<c:set var="titre" value="Contrat ${contrat.numero}" />
<!DOCTYPE html>
<html lang="fr">
<head><%@ include file="/WEB-INF/views/common/head.jspf" %></head>
<body>
<div class="app">
  <jsp:include page="/WEB-INF/views/common/sidebar.jsp"><jsp:param name="actif" value="contrats" /></jsp:include>
  <main class="main">
    <div class="container">
      <header class="page-head">
        <div class="stack-sm">
          <a class="back" href="${ctx}/client/contrats">← Mes contrats</a>
          <h1><c:out value="${contrat.typeAssurance.nom}" /></h1>
          <span class="muted"><c:out value="${contrat.numero}" /></span>
        </div>
        <c:if test="${contrat.statut == 'ACTIF'}">
          <a class="btn btn-primary" href="${ctx}/client/sinistres/declarer?contrat=${contrat.id}">+ Déclarer un sinistre</a>
        </c:if>
      </header>

      <section class="grid-3" aria-label="Informations du contrat">
        <div class="kpi"><span class="kpi-label">Statut</span><span><ui:statut valeur="${contrat.statut}" /></span></div>
        <div class="kpi"><span class="kpi-label">Période</span><span class="strong"><ui:date valeur="${contrat.dateDebut}" /> → <ui:date valeur="${contrat.dateFin}" /></span></div>
        <div class="kpi"><span class="kpi-label">Prime mensuelle</span><span class="kpi-value"><ui:montant valeur="${contrat.prime}" /></span></div>
      </section>

      <section class="card" aria-labelledby="h-gar">
        <h2 id="h-gar">Garanties incluses</h2>
        <div class="table-wrap">
          <table class="table">
            <thead><tr><th>Garantie</th><th>Description</th><th class="right">Plafond</th><th class="right">Franchise</th></tr></thead>
            <tbody>
              <c:forEach items="${contrat.garanties}" var="g">
                <tr>
                  <td class="strong"><c:out value="${g.nom}" /></td>
                  <td class="muted"><c:out value="${g.description}" /></td>
                  <td class="right"><ui:montant valeur="${g.plafond}" /></td>
                  <td class="right"><ui:montant valeur="${g.franchise}" /></td>
                </tr>
              </c:forEach>
            </tbody>
          </table>
        </div>
      </section>

      <div class="row stretch">
        <section class="card col-main" aria-labelledby="h-pay">
          <h2 id="h-pay">Paiements de ce contrat</h2>
          <c:set var="listePaiements" value="${paiements}" />
          <%@ include file="/WEB-INF/views/client/table-paiements.jspf" %>
        </section>
        <section class="card col-side" aria-labelledby="h-sin">
          <h2 id="h-sin">Sinistres liés</h2>
          <c:forEach items="${sinistres}" var="si">
            <a class="between" href="${ctx}/client/sinistres/detail?id=${si.id}">
              <span class="small"><c:out value="${si.numero}" /> · <ui:date valeur="${si.dateSinistre}" /></span>
              <ui:statut valeur="${si.statut}" />
            </a>
          </c:forEach>
          <c:if test="${empty sinistres}"><p class="small muted">Aucun sinistre déclaré sur ce contrat.</p></c:if>
        </section>
      </div>
    </div>
  </main>
</div>
<script src="${ctx}/js/app.js"></script>
</body>
</html>
