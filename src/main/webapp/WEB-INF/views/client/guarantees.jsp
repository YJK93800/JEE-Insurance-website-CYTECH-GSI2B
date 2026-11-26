<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/taglibs.jspf" %>
<%-- GET /client/guarantees — ClientGuaranteesServlet
     Attributs : contracts List<Contract> (contrats ACTIF, SUSPENDU et EN_ATTENTE du client, avec insuranceType et guarantees chargés) --%>
<c:set var="title" value="Mes garanties" />
<!DOCTYPE html>
<html lang="fr">
<head><%@ include file="/WEB-INF/views/common/head.jspf" %></head>
<body>
<div class="app">
  <jsp:include page="/WEB-INF/views/common/sidebar.jsp"><jsp:param name="active" value="guarantees" /></jsp:include>
  <main class="main">
    <div class="container">
      <header class="page-head"><div class="stack-sm"><span class="eyebrow">Espace client</span><h1>Mes garanties</h1></div></header>

      <c:forEach items="${contracts}" var="ct">
        <section class="card" aria-label="Garanties du contrat ${fn:escapeXml(ct.number)}">
          <div class="card-head">
            <div class="stack-sm">
              <h2><c:out value="${ct.insuranceType.name}" /></h2>
              <a class="small muted" href="${ctx}/client/contracts?id=${ct.id}"><c:out value="${ct.number}" /></a>
            </div>
            <ui:status value="${ct.status}" label="${ct.status.label}" />
          </div>
          <div class="table-wrap">
            <table class="table">
              <thead><tr><th>Garantie</th><th>Ce qui est couvert</th><th class="right">Plafond</th><th class="right">Franchise</th></tr></thead>
              <tbody>
                <c:forEach items="${ct.guarantees}" var="g">
                  <tr>
                    <td class="strong"><c:out value="${g.name}" /></td>
                    <td class="muted"><c:out value="${g.description}" /></td>
                    <td class="right"><c:choose><c:when test="${not empty g.coverageLimit}"><ui:amount value="${g.coverageLimit}" /></c:when><c:otherwise><span class="muted">—</span></c:otherwise></c:choose></td>
                    <td class="right"><c:choose><c:when test="${not empty g.deductible}"><ui:amount value="${g.deductible}" /></c:when><c:otherwise><span class="muted">Aucune</span></c:otherwise></c:choose></td>
                  </tr>
                </c:forEach>
                <c:if test="${empty ct.guarantees}"><tr><td colspan="4" class="empty">Aucune garantie sur ce contrat.</td></tr></c:if>
              </tbody>
            </table>
          </div>
        </section>
      </c:forEach>
      <c:if test="${empty contracts}"><div class="card"><p class="empty">Aucune garantie : vous n'avez pas de contrat en cours.</p></div></c:if>
    </div>
  </main>
</div>
<script src="${ctx}/js/app.js"></script>
</body>
</html>
