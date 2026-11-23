<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/taglibs.jspf" %>
<%-- ClientContractsServlet — GET /client/contracts?id=12 (404 si le contrat n'appartient pas au client connecté)
     Attributs : contract Contract (number, insuranceType{name, description}, startDate, endDate, premium, status,
                                   agent{firstName, lastName}, guarantees Set<Guarantee>{name, description}) --%>
<c:set var="titre" value="Contrat ${contract.number}" />
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
          <a class="back" href="${ctx}/client/contracts">← Mes contrats</a>
          <h1><c:out value="${contract.insuranceType.name}" /></h1>
          <span class="muted"><c:out value="${contract.number}" /></span>
        </div>
        <c:if test="${contract.status == 'ACTIF'}">
          <a class="btn btn-primary" href="${ctx}/client/claims?new=1&amp;contractId=${contract.id}">+ Déclarer un sinistre</a>
        </c:if>
      </header>

      <section class="grid-3" aria-label="Informations du contrat">
        <div class="kpi"><span class="kpi-label">Statut</span><span><ui:status value="${contract.status}" label="${contract.status.label}" /></span></div>
        <div class="kpi"><span class="kpi-label">Période</span><span class="strong"><ui:date value="${contract.startDate}" /> → <ui:date value="${contract.endDate}" /></span></div>
        <div class="kpi"><span class="kpi-label">Prime mensuelle</span><span class="kpi-value"><ui:amount value="${contract.premium}" /></span></div>
      </section>

      <div class="row stretch">
        <section class="card col-main" aria-labelledby="h-gar">
          <h2 id="h-gar">Garanties incluses</h2>
          <c:choose>
            <c:when test="${empty contract.guarantees}"><p class="empty">Aucune garantie sur ce contrat.</p></c:when>
            <c:otherwise>
              <div class="table-wrap">
                <table class="table">
                  <thead><tr><th>Garantie</th><th>Description</th></tr></thead>
                  <tbody>
                    <c:forEach items="${contract.guarantees}" var="g">
                      <tr>
                        <td class="strong"><c:out value="${g.name}" /></td>
                        <td class="muted"><c:out value="${g.description}" /></td>
                      </tr>
                    </c:forEach>
                  </tbody>
                </table>
              </div>
            </c:otherwise>
          </c:choose>
        </section>

        <section class="card col-side" aria-labelledby="h-info">
          <h2 id="h-info">Votre contrat</h2>
          <p class="small muted"><c:out value="${contract.insuranceType.description}" /></p>
          <c:if test="${not empty contract.agent}">
            <div class="between small"><span class="muted">Agent</span><span class="strong"><c:out value="${contract.agent.firstName} ${contract.agent.lastName}" /></span></div>
          </c:if>
          <a class="btn btn-block" href="${ctx}/client/payments?contractId=${contract.id}">Voir les paiements de ce contrat</a>
          <a class="btn btn-block" href="${ctx}/client/claims">Voir mes sinistres</a>
        </section>
      </div>
    </div>
  </main>
</div>
<script src="${ctx}/js/app.js"></script>
</body>
</html>
