<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/taglibs.jspf" %>
<%-- GET /client/garanties — PAS ENCORE DE SERVLET (vue prête pour le modèle anglais)
     Attributs attendus : contracts List<Contract> (contrats ACTIF et EN_ATTENTE du client, avec insuranceType et guarantees chargés)
     NB : la vue affiche aussi g.plafond et g.franchise, qui n'existent pas encore dans Guarantee. --%>
<c:set var="titre" value="Mes garanties" />
<!DOCTYPE html>
<html lang="fr">
<head><%@ include file="/WEB-INF/views/common/head.jspf" %></head>
<body>
<div class="app">
  <jsp:include page="/WEB-INF/views/common/sidebar.jsp"><jsp:param name="actif" value="garanties" /></jsp:include>
  <main class="main">
    <div class="container">
      <header class="page-head"><div class="stack-sm"><span class="eyebrow">Espace client</span><h1>Mes garanties</h1></div></header>

      <c:forEach items="${contracts}" var="ct">
        <section class="card" aria-label="Garanties du contrat ${fn:escapeXml(ct.number)}">
          <div class="card-head">
            <div class="stack-sm">
              <h2><c:out value="${ct.insuranceType.name}" /></h2>
              <span class="small muted"><c:out value="${ct.number}" /></span>
            </div>
            <ui:status value="${ct.status}" />
          </div>
          <div class="table-wrap">
            <table class="table">
              <thead><tr><th>Garantie</th><th>Ce qui est couvert</th><th class="right">Plafond</th><th class="right">Franchise</th></tr></thead>
              <tbody>
                <c:forEach items="${ct.guarantees}" var="g">
                  <tr>
                    <td class="strong"><c:out value="${g.name}" /></td>
                    <td class="muted"><c:out value="${g.description}" /></td>
                    <td class="right"><ui:amount value="${g.plafond}" /></td>
                    <td class="right"><ui:amount value="${g.franchise}" /></td>
                  </tr>
                </c:forEach>
              </tbody>
            </table>
          </div>
        </section>
      </c:forEach>
      <c:if test="${empty contracts}"><div class="card"><p class="empty">Aucune garantie : vous n'avez pas de contrat actif.</p></div></c:if>
    </div>
  </main>
</div>
<script src="${ctx}/js/app.js"></script>
</body>
</html>
