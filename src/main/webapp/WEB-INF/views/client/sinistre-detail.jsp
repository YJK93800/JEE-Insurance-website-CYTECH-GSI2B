<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/taglibs.jspf" %>
<%-- GET /client/sinistres/detail?id=93 (la servlet vérifie que le sinistre appartient au client)
     POST /client/sinistres/pieces (multipart : sinistreId, pieces) pour ajouter un justificatif
     Attributs : sinistre Sinistre (numero, dateSinistre, dateDeclaration, typeSinistre, description, montantEstime,
                 montantIndemnisation, statut, contrat, agent, pieces List<PieceJustificative>{id, nomFichier},
                 commentaires List<Commentaire>{date, auteur{prenom, nom}, texte} – seuls les commentaires visibles client) --%>
<c:set var="titre" value="Sinistre ${sinistre.numero}" />
<c:set var="sin" value="${sinistre}" />
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
          <a class="back" href="${ctx}/client/sinistres">← Mes sinistres</a>
          <h1><c:out value="${sin.numero}" /></h1>
          <span class="muted"><c:out value="${sin.typeSinistre.libelle}" /> · <c:out value="${sin.contrat.typeAssurance.nom}" /> <c:out value="${sin.contrat.numero}" /></span>
        </div>
        <ui:statut valeur="${sin.statut}" />
      </header>
      <%@ include file="/WEB-INF/views/common/flash.jspf" %>

      <div class="row">
        <div class="col-main">
          <section class="card" aria-labelledby="h-desc">
            <h2 id="h-desc">Déclaration</h2>
            <div class="grid-3 small">
              <div class="stack-sm"><span class="muted">Date du sinistre</span><span class="strong"><ui:date valeur="${sin.dateSinistre}" /></span></div>
              <div class="stack-sm"><span class="muted">Montant estimé</span><span class="strong"><ui:montant valeur="${sin.montantEstime}" /></span></div>
              <div class="stack-sm"><span class="muted">Indemnisation</span><span class="strong">
                <c:choose><c:when test="${not empty sin.montantIndemnisation}"><ui:montant valeur="${sin.montantIndemnisation}" /></c:when><c:otherwise>—</c:otherwise></c:choose></span></div>
            </div>
            <p class="quote"><c:out value="${sin.description}" /></p>
          </section>

          <section class="card" aria-labelledby="h-pieces">
            <h2 id="h-pieces">Pièces justificatives (${fn:length(sin.pieces)})</h2>
            <div class="inline">
              <c:forEach items="${sin.pieces}" var="pj">
                <a class="btn btn-sm" href="${ctx}/fichiers/piece?id=${pj.id}"><c:out value="${pj.nomFichier}" /></a>
              </c:forEach>
            </div>
            <c:if test="${sin.statut == 'DECLARE' or sin.statut == 'EN_ANALYSE'}">
              <form class="inline" method="post" action="${ctx}/client/sinistres/pieces" enctype="multipart/form-data" data-validate>
                <%@ include file="/WEB-INF/views/common/csrf.jspf" %>
                <input type="hidden" name="sinistreId" value="${sin.id}">
                <input type="file" name="pieces" multiple accept=".pdf,.jpg,.jpeg,.png" required data-max-size="10485760">
                <button class="btn btn-sm btn-primary" type="submit">Ajouter</button>
              </form>
            </c:if>
          </section>

          <section class="card" aria-labelledby="h-echanges">
            <h2 id="h-echanges">Échanges avec votre agent</h2>
            <div class="history">
              <c:forEach items="${sin.commentaires}" var="cm">
                <span><strong><ui:date valeur="${cm.date}" /></strong> · <c:out value="${cm.auteur.prenom} ${cm.auteur.nom}" /> : <c:out value="${cm.texte}" /></span>
              </c:forEach>
              <c:if test="${empty sin.commentaires}"><span class="muted">Aucun message pour le moment.</span></c:if>
            </div>
          </section>
        </div>

        <aside class="col-side">
          <section class="card" aria-labelledby="h-suivi">
            <h2 id="h-suivi">Avancement</h2>
            <%@ include file="/WEB-INF/views/common/suivi-sinistre.jspf" %>
          </section>
        </aside>
      </div>
    </div>
  </main>
</div>
<script src="${ctx}/js/app.js"></script>
</body>
</html>
