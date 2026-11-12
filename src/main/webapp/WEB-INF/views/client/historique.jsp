<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/taglibs.jspf" %>
<%-- GET /client/historique
     Attributs : demandes List<Demande> triées de la plus récente à la plus ancienne
                 Demande : date (LocalDate), type (ex. "Déclaration de sinistre", "Ajout de pièce", "Souscription"),
                           reference (n° de sinistre ou de contrat), lien (URL relative, peut être null), statut --%>
<c:set var="titre" value="Historique des demandes" />
<!DOCTYPE html>
<html lang="fr">
<head><%@ include file="/WEB-INF/views/common/head.jspf" %></head>
<body>
<div class="app">
  <jsp:include page="/WEB-INF/views/common/sidebar.jsp"><jsp:param name="actif" value="historique" /></jsp:include>
  <main class="main">
    <div class="container">
      <header class="page-head"><div class="stack-sm"><span class="eyebrow">Espace client</span><h1>Historique des demandes</h1></div></header>
      <section class="card flush">
        <c:choose>
          <c:when test="${empty demandes}"><p class="empty">Aucune demande pour le moment.</p></c:when>
          <c:otherwise>
            <div class="table-wrap">
              <table class="table">
                <thead><tr><th>Date</th><th>Demande</th><th>Référence</th><th>Statut</th></tr></thead>
                <tbody>
                  <c:forEach items="${demandes}" var="d">
                    <tr>
                      <td><ui:date valeur="${d.date}" /></td>
                      <td><c:out value="${d.type}" /></td>
                      <td class="strong">
                        <c:choose>
                          <c:when test="${not empty d.lien}"><a href="${ctx}${d.lien}"><c:out value="${d.reference}" /></a></c:when>
                          <c:otherwise><c:out value="${d.reference}" /></c:otherwise>
                        </c:choose>
                      </td>
                      <td><c:if test="${not empty d.statut}"><ui:statut valeur="${d.statut}" /></c:if></td>
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
