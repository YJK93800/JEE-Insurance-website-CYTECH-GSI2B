<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/taglibs.jspf" %>
<%-- ClientClaimsServlet — GET /client/claims
     Attributs : claims List<Claim> du client connecté (number, claimDate, type, contract{number}, estimatedAmount, status) --%>
<c:set var="title" value="Mes sinistres" />
<!DOCTYPE html>
<html lang="fr">
<head><%@ include file="/WEB-INF/views/common/head.jspf" %></head>
<body>
<div class="app">
  <jsp:include page="/WEB-INF/views/common/sidebar.jsp"><jsp:param name="active" value="claims" /></jsp:include>
  <main class="main">
    <div class="container">
      <header class="page-head">
        <div class="stack-sm"><span class="eyebrow">Espace client</span><h1>Mes sinistres</h1></div>
        <a class="btn btn-primary" href="${ctx}/client/claims?new=1">+ Déclarer un sinistre</a>
      </header>
      <%@ include file="/WEB-INF/views/common/flash.jspf" %>

      <section class="card flush" aria-label="Liste des sinistres">
        <c:choose>
          <c:when test="${empty claims}"><p class="empty">Vous n'avez déclaré aucun sinistre.</p></c:when>
          <c:otherwise>
            <div class="table-wrap">
              <table class="table">
                <thead><tr><th>N° sinistre</th><th>Date</th><th>Type</th><th>Contrat</th><th class="right">Estimé</th><th>Statut</th><th></th></tr></thead>
                <tbody>
                  <c:forEach items="${claims}" var="clm">
                    <tr>
                      <td class="strong"><c:out value="${clm.number}" /></td>
                      <td><ui:date value="${clm.claimDate}" /></td>
                      <td><ui:claimType value="${clm.type}" /></td>
                      <td class="muted"><c:out value="${clm.contract.number}" /></td>
                      <td class="right strong"><ui:amount value="${clm.estimatedAmount}" /></td>
                      <td><ui:status value="${clm.status}" /></td>
                      <td class="right"><a class="btn btn-sm" href="${ctx}/client/claims?id=${clm.id}">Suivre</a></td>
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
