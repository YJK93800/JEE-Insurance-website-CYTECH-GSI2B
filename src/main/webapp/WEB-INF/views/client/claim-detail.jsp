<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/taglibs.jspf" %>
<%-- ClientClaimsServlet — GET /client/claims?id=93 (404 si le sinistre n'appartient pas au client connecté)
     Attributs : claim Claim (number, claimDate, declaredAt, type, description, estimatedAmount,
                 indemnifiedAmount, indemnificationDate, status, contract{number}, agent{firstName, lastName}) --%>
<c:set var="titre" value="Sinistre ${claim.number}" />
<c:set var="sin" value="${claim}" />
<!DOCTYPE html>
<html lang="fr">
<head><%@ include file="/WEB-INF/views/common/head.jspf" %></head>
<body>
<div class="app">
  <jsp:include page="/WEB-INF/views/common/sidebar.jsp"><jsp:param name="actif" value="sinistres" /></jsp:include>
  <main class="main">
    <div class="container">
      <header class="page-head">
        <div class="stack-sm">
          <a class="back" href="${ctx}/client/claims">← Mes sinistres</a>
          <h1><c:out value="${sin.number}" /></h1>
          <span class="muted"><ui:claimType value="${sin.type}" /> · contrat <c:out value="${sin.contract.number}" /></span>
        </div>
        <ui:status value="${sin.status}" />
      </header>
      <%@ include file="/WEB-INF/views/common/flash.jspf" %>

      <div class="row">
        <div class="col-main">
          <section class="card" aria-labelledby="h-desc">
            <h2 id="h-desc">Déclaration</h2>
            <div class="grid-3 small">
              <div class="stack-sm"><span class="muted">Date du sinistre</span><span class="strong"><ui:date value="${sin.claimDate}" /></span></div>
              <div class="stack-sm"><span class="muted">Montant estimé</span><span class="strong"><ui:amount value="${sin.estimatedAmount}" /></span></div>
              <div class="stack-sm"><span class="muted">Indemnisation</span><span class="strong">
                <c:choose><c:when test="${not empty sin.indemnifiedAmount}"><ui:amount value="${sin.indemnifiedAmount}" /></c:when><c:otherwise>—</c:otherwise></c:choose></span></div>
            </div>
            <p class="quote"><c:out value="${sin.description}" /></p>
          </section>
        </div>

        <aside class="col-side">
          <section class="card" aria-labelledby="h-suivi">
            <h2 id="h-suivi">Avancement</h2>
            <%@ include file="/WEB-INF/views/common/claim-timeline.jspf" %>
          </section>
        </aside>
      </div>
    </div>
  </main>
</div>
<script src="${ctx}/js/app.js"></script>
</body>
</html>
