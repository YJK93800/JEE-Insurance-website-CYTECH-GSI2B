<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/taglibs.jspf" %>
<%-- GET  /client/sinistres/declarer[?contrat=12]  -> cette vue
     POST /client/sinistres/declarer (multipart/form-data) -> DeclarerSinistreServlet (@MultipartConfig)
     Paramètres : contratId, typeSinistre, dateSinistre (aaaa-mm-jj), montantEstime, description, pieces (fichiers)
     Attributs : contratsActifs  List<Contrat>   (seuls les contrats ACTIF du client)
                 typesSinistre   TypeSinistre[]  (enum avec getLibelle() et getDocumentsRequis() : List<String>)
                 erreurs         Map<String,String>  en cas d'échec de validation côté servlet --%>
<c:set var="titre" value="Déclarer un sinistre" />
<c:set var="contratChoisi" value="${not empty param.contratId ? param.contratId : param.contrat}" />
<!DOCTYPE html>
<html lang="fr">
<head><%@ include file="/WEB-INF/views/common/head.jspf" %></head>
<body>
<div class="app">
  <jsp:include page="/WEB-INF/views/common/sidebar.jsp"><jsp:param name="actif" value="sinistres" /></jsp:include>
  <main class="main">
    <div class="container">
      <header class="stack-sm">
        <a class="back" href="${ctx}/client/sinistres">← Mes sinistres</a>
        <h1>Déclarer un sinistre</h1>
        <p class="muted">Environ 5 minutes. Les champs marqués * sont obligatoires.</p>
      </header>

      <%@ include file="/WEB-INF/views/common/flash.jspf" %>
      <c:if test="${not empty erreurs}">
        <div class="alert alert-error" role="alert"><span>Le formulaire contient ${fn:length(erreurs)} erreur(s). Corrigez les champs signalés.</span></div>
      </c:if>

      <c:choose>
        <c:when test="${empty contratsActifs}">
          <div class="card"><p class="empty">Vous n'avez aucun contrat actif : il n'est pas possible de déclarer un sinistre pour le moment.</p></div>
        </c:when>
        <c:otherwise>
      <div class="row">
        <form class="card form col-main" method="post" action="${ctx}/client/sinistres/declarer"
              enctype="multipart/form-data" novalidate data-validate id="form-sinistre">
          <%@ include file="/WEB-INF/views/common/csrf.jspf" %>

          <fieldset class="${not empty erreurs.contratId ? 'has-error' : ''}">
            <legend>Contrat concerné *</legend>
            <c:forEach items="${contratsActifs}" var="ct" varStatus="i">
              <label class="choice">
                <input type="radio" name="contratId" value="${ct.id}" required
                       data-numero="${fn:escapeXml(ct.numero)}"
                       ${contratChoisi == fn:trim(ct.id) or (empty contratChoisi and i.first) ? 'checked' : ''}>
                <span class="stack-sm">
                  <span class="strong"><c:out value="${ct.typeAssurance.nom}" /></span>
                  <span class="small muted"><c:out value="${ct.numero}" /> ·
                    <c:forEach items="${ct.garanties}" var="g" varStatus="gs"><c:out value="${g.nom}" />${gs.last ? '' : ', '}</c:forEach></span>
                </span>
              </label>
            </c:forEach>
            <c:if test="${not empty erreurs.contratId}"><span class="field-error"><c:out value="${erreurs.contratId}" /></span></c:if>
            <span class="field-help">Seuls vos contrats actifs sont proposés.</span>
          </fieldset>

          <fieldset>
            <legend>Type de sinistre *</legend>
            <div class="pills" role="radiogroup">
              <c:forEach items="${typesSinistre}" var="t" varStatus="i">
                <c:set var="docs"><c:forEach items="${t.documentsRequis}" var="d" varStatus="ds">${d}${ds.last ? '' : '|'}</c:forEach></c:set>
                <label class="pill">
                  <input type="radio" name="typeSinistre" value="${t}" required
                         data-libelle="${fn:escapeXml(t.libelle)}"
                         data-docs="${fn:escapeXml(docs)}"
                         ${param.typeSinistre == t.name() or (empty param.typeSinistre and i.first) ? 'checked' : ''}>
                  <span><c:out value="${t.libelle}" /></span>
                </label>
              </c:forEach>
            </div>
            <c:if test="${not empty erreurs.typeSinistre}"><span class="field-error"><c:out value="${erreurs.typeSinistre}" /></span></c:if>
          </fieldset>

          <div class="grid-2">
            <ui:champ nom="dateSinistre" libelle="Date du sinistre *" type="date" requis="true" extra="data-max-today" />
            <ui:champ nom="montantEstime" libelle="Montant estimé (€) *" type="number" requis="true"
                      extra="min='0.01' step='0.01' inputmode='decimal'" />
          </div>

          <label class="field ${not empty erreurs.description ? 'has-error' : ''}">Description de l'événement *
            <textarea name="description" rows="5" maxlength="1000" required data-counter="compteur-desc"
                      placeholder="Que s'est-il passé ? Où ? Quels biens sont endommagés ?"><c:out value="${param.description}" /></textarea>
            <c:if test="${not empty erreurs.description}"><span class="field-error"><c:out value="${erreurs.description}" /></span></c:if>
            <span class="field-help" id="compteur-desc">0 / 1000 caractères</span>
          </label>

          <div class="stack">
            <span class="strong">Pièces justificatives</span>
            <label class="dropzone" data-dropzone>
              <svg width="28" height="28" viewBox="0 0 24 24" aria-hidden="true" fill="none" stroke="#0F6E6E" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 16V4"/><path d="M7 9l5-5 5 5"/><path d="M4 16v3a1 1 0 0 0 1 1h14a1 1 0 0 0 1-1v-3"/></svg>
              <span class="strong">Glissez vos fichiers ici ou <u>parcourez</u></span>
              <span class="xsmall muted">PDF, JPG ou PNG · 10 Mo max par fichier · 5 fichiers max</span>
              <input type="file" name="pieces" multiple accept=".pdf,.jpg,.jpeg,.png" data-max-size="10485760" data-max-files="5">
            </label>
            <c:if test="${not empty erreurs.pieces}"><span class="field-error"><c:out value="${erreurs.pieces}" /></span></c:if>
            <ul class="file-list" data-file-list aria-live="polite"></ul>
          </div>

          <div class="form-actions">
            <a class="btn" href="${ctx}/client/sinistres">Annuler</a>
            <button type="submit" class="btn btn-primary">Envoyer la déclaration</button>
          </div>
        </form>

        <aside class="col-side">
          <section class="card" aria-labelledby="h-docs">
            <span><span class="badge badge-ai">CONSEIL</span></span>
            <h2 id="h-docs">Documents à fournir · <span data-type-libelle></span></h2>
            <ul class="stack small" data-docs-list></ul>
            <a class="small strong" href="${ctx}/client/assistant">Demander à l'assistant</a>
          </section>

          <section class="card" aria-labelledby="h-recap">
            <h2 id="h-recap">Votre déclaration</h2>
            <div class="between small"><span class="muted">Contrat</span><span class="strong" data-recap="contrat">—</span></div>
            <div class="between small"><span class="muted">Type</span><span class="strong" data-type-libelle>—</span></div>
            <div class="between small"><span class="muted">Date</span><span class="strong" data-recap="date">—</span></div>
            <div class="between small"><span class="muted">Pièces jointes</span><span class="strong" data-recap="pieces">0</span></div>
            <div class="between small"><span class="muted">Statut à l'envoi</span><ui:statut valeur="DECLARE" /></div>
          </section>
        </aside>
      </div>
        </c:otherwise>
      </c:choose>
    </div>
  </main>
</div>
<script src="${ctx}/js/app.js"></script>
</body>
</html>
