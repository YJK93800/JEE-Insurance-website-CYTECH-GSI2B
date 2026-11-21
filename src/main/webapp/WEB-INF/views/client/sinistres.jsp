<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/taglibs.jspf" %>
<%-- GET /client/sinistres
     Attributs : sinistres List<Sinistre> du client connecté (numero, dateSinistre, typeSinistre, contrat, montantEstime, statut) --%>
<c:set var="titre" value="Mes sinistres" />
<!DOCTYPE html>
<html lang="fr">
<head><%@ include file="/WEB-INF/views/common/head.jspf" %></head>
<body>
<div class="app">
  <jsp:include page="/WEB-INF/views/common/sidebar.jsp"><jsp:param name="actif" value="sinistres" /></jsp:include>
  <main class="main">
    <div class="container">
      <header class="page-head">
        <div class="stack-sm"><span class="eyebrow">Espace client</span><h1>Mes sinistres</h1></div>
        <a class="btn btn-primary" href="${ctx}/client/sinistres/declarer">+ Déclarer un sinistre</a>
      </header>
      <%@ include file="/WEB-INF/views/common/flash.jspf" %>

      <section class="card flush" aria-label="Liste des sinistres">
        <c:choose>
          <c:when test="${empty sinistres}"><p class="empty">Vous n'avez déclaré aucun sinistre.</p></c:when>
          <c:otherwise>
            <div class="table-wrap">
              <table class="table">
                <thead><tr><th>N° sinistre</th><th>Date</th><th>Type</th><th>Contrat</th><th class="right">Estimé</th><th>Statut</th><th></th></tr></thead>
                <tbody>
                  <c:forEach items="${sinistres}" var="si">
                    <tr>
                      <td class="strong"><c:out value="${si.numero}" /></td>
                      <td><ui:date valeur="${si.dateSinistre}" /></td>
                      <td><c:out value="${si.typeSinistre.libelle}" /></td>
                      <td class="muted"><c:out value="${si.contrat.typeAssurance.nom}" /> · <c:out value="${si.contrat.numero}" /></td>
                      <td class="right strong"><ui:montant valeur="${si.montantEstime}" /></td>
                      <td><ui:statut valeur="${si.statut}" /></td>
                      <td class="right"><a class="btn btn-sm" href="${ctx}/client/sinistres/detail?id=${si.id}">Suivre</a></td>
                    </tr>
                  </c:forEach>
                </tbody>
              </table>
            </div>
          </c:otherwise>
        </c:choose>
      </section>
    </div>
  </main>
</div>
<script src="${ctx}/js/app.js"></script>
</body>
</html>
