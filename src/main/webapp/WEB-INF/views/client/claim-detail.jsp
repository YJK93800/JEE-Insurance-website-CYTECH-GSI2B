<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/taglibs.jspf" %>
<%-- ClientClaimsServlet — GET /client/claims?id=93 (404 si le sinistre n'appartient pas au client connecté)
     Attributs : claim Claim (number, claimDate, declaredAt, type, description, estimatedAmount,
                 indemnifiedAmount, indemnificationDate, status, contract{number}, agent{firstName, lastName})
                 history List<ClaimHistory> (changements de statut, du plus ancien au plus récent)
                 attachments List<ClaimAttachment> ; errors.attachments si l'ajout de pièces a échoué
     POST /client/claims  action=attachments, id, attachments (fichiers) : tant que DECLARE ou EN_ANALYSE, 5 pièces max --%>
<c:set var="title" value="Sinistre ${claim.number}" />
<!DOCTYPE html>
<html lang="fr">
<head><%@ include file="/WEB-INF/views/common/head.jspf" %></head>
<body>
<div class="app">
  <jsp:include page="/WEB-INF/views/common/sidebar.jsp"><jsp:param name="active" value="claims" /></jsp:include>
  <main class="main">
    <div class="container">
      <header class="page-head">
        <div class="stack-sm">
          <a class="back" href="${ctx}/client/claims">← Mes sinistres</a>
          <h1><c:out value="${claim.number}" /></h1>
          <span class="muted"><ui:claimType value="${claim.type}" /> · contrat <c:out value="${claim.contract.number}" /></span>
        </div>
        <ui:status value="${claim.status}" />
      </header>
      <%@ include file="/WEB-INF/views/common/flash.jspf" %>

      <div class="row">
        <div class="col-main">
          <section class="card" aria-labelledby="h-desc">
            <h2 id="h-desc">Déclaration</h2>
            <div class="grid-3 small">
              <div class="stack-sm"><span class="muted">Date du sinistre</span><span class="strong"><ui:date value="${claim.claimDate}" /></span></div>
              <div class="stack-sm"><span class="muted">Montant estimé</span><span class="strong"><ui:amount value="${claim.estimatedAmount}" /></span></div>
              <div class="stack-sm"><span class="muted">Indemnisation</span><span class="strong">
                <c:choose><c:when test="${not empty claim.indemnifiedAmount}"><ui:amount value="${claim.indemnifiedAmount}" /></c:when><c:otherwise>—</c:otherwise></c:choose></span></div>
            </div>
            <p class="quote"><c:out value="${claim.description}" /></p>
          </section>

          <section class="card" id="attachments" aria-labelledby="h-att">
            <h2 id="h-att">Pièces justificatives</h2>
            <c:set var="attachmentUrl" value="${ctx}/client/claims?attachment=" />
            <%@ include file="/WEB-INF/views/common/claim-attachments.jspf" %>

            <c:set var="remaining" value="${5 - fn:length(attachments)}" />
            <c:choose>
              <c:when test="${(claim.status == 'DECLARE' or claim.status == 'EN_ANALYSE') and remaining gt 0}">
                <form class="form" method="post" action="${ctx}/client/claims" enctype="multipart/form-data" novalidate data-validate>
                  <%@ include file="/WEB-INF/views/common/csrf.jspf" %>
                  <input type="hidden" name="id" value="${claim.id}">
                  <input type="hidden" name="action" value="attachments">
                  <label class="field ${not empty errors.attachments ? 'has-error' : ''}">Ajouter des pièces
                    <input type="file" name="attachments" multiple required
                           accept=".pdf,.jpg,.jpeg,.png,application/pdf,image/jpeg,image/png"
                           data-max-size="5242880" data-max-files="${remaining}"
                           <c:if test="${not empty errors.attachments}">aria-invalid="true" aria-describedby="e-attachments"</c:if>>
                    <c:choose>
                      <c:when test="${not empty errors.attachments}"><span class="field-error" id="e-attachments"><c:out value="${errors.attachments}" /></span></c:when>
                      <c:otherwise><span class="field-help">PDF, JPG ou PNG · 5 Mo maximum par fichier · encore ${remaining} fichier(s) possible(s)</span></c:otherwise>
                    </c:choose>
                  </label>
                  <button type="submit" class="btn btn-primary">Envoyer</button>
                </form>
              </c:when>
              <c:when test="${claim.status == 'DECLARE' or claim.status == 'EN_ANALYSE'}">
                <p class="xsmall muted">Vous avez atteint le maximum de 5 pièces pour ce sinistre.</p>
              </c:when>
            </c:choose>
          </section>
        </div>

        <aside class="col-side">
          <section class="card" aria-labelledby="h-suivi">
            <h2 id="h-suivi">Avancement</h2>
            <%@ include file="/WEB-INF/views/common/claim-timeline.jspf" %>
          </section>
          <section class="card" aria-labelledby="h-histo">
            <h2 id="h-histo">Historique</h2>
            <%@ include file="/WEB-INF/views/common/claim-history.jspf" %>
          </section>
        </aside>
      </div>
    </div>
  </main>
</div>
<script src="${ctx}/js/app.js"></script>
</body>
</html>
