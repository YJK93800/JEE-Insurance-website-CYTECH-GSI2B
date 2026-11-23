<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/taglibs.jspf" %>
<%-- GET  /admin/types[?id=2] — PAS ENCORE DE SERVLET (vue prête pour le modèle anglais)
     POST /admin/types            (id vide = création ; nom, description, prixBase, garantieIds multiples)
     POST /admin/types/supprimer  (id) -> refusé par la servlet si des contrats utilisent ce type
     Attributs attendus : types List<…>{id, name, description, basePrice, guarantees, contractCount}
                          type InsuranceType en cours de modification (null en création)
                          guarantees List<Guarantee>   errors Map<String,String>
     NB : contractCount n'existe pas dans InsuranceType (DTO ou requête de comptage à prévoir).
     Ajouter un nouveau type d'assurance = une ligne en base, aucun code à modifier. --%>
<c:set var="title" value="Types d'assurance" />
<c:set var="t0" value="${type}" />
<!DOCTYPE html>
<html lang="fr">
<head><%@ include file="/WEB-INF/views/common/head.jspf" %></head>
<body>
<div class="app">
  <jsp:include page="/WEB-INF/views/common/sidebar.jsp"><jsp:param name="active" value="types" /></jsp:include>
  <main class="main">
    <div class="container">
      <header class="page-head"><div class="stack-sm"><span class="eyebrow">Administration</span><h1>Types d'assurance</h1></div></header>
      <%@ include file="/WEB-INF/views/common/flash.jspf" %>

      <div class="row">
        <section class="col-main stack" aria-label="Liste des types">
          <c:forEach items="${types}" var="it">
            <article class="card ${t0.id == it.id ? 'selected' : ''}">
              <div class="between">
                <div class="stack-sm">
                  <h2><c:out value="${it.name}" /></h2>
                  <span class="small muted"><c:out value="${it.description}" /></span>
                </div>
                <div class="inline">
                  <span class="amount"><ui:amount value="${it.basePrice}" /> <small>/mois</small></span>
                  <span class="small muted">${it.contractCount} contrat(s)</span>
                </div>
              </div>
              <div class="inline"><c:forEach items="${it.guarantees}" var="g"><span class="chip"><c:out value="${g.name}" /></span></c:forEach></div>
              <div class="inline">
                <a class="btn btn-sm" href="${ctx}/admin/types?id=${it.id}">Modifier</a>
                <c:if test="${it.contractCount == 0}">
                  <form method="post" action="${ctx}/admin/types/supprimer" data-confirm="Supprimer le type « ${fn:escapeXml(it.name)} » ?">
                    <%@ include file="/WEB-INF/views/common/csrf.jspf" %>
                    <input type="hidden" name="id" value="${it.id}">
                    <button class="btn btn-sm btn-danger" type="submit">Supprimer</button>
                  </form>
                </c:if>
              </div>
            </article>
          </c:forEach>
          <c:if test="${empty types}"><div class="card"><p class="empty">Aucun type d'assurance.</p></div></c:if>
        </section>

        <form class="card form col-side" method="post" action="${ctx}/admin/types" novalidate data-validate>
          <%@ include file="/WEB-INF/views/common/csrf.jspf" %>
          <input type="hidden" name="id" value="${t0.id}">
          <h2>${empty t0 ? 'Nouveau type' : 'Modifier le type'}</h2>
          <ui:field name="name" label="Nom *" required="true" value="${not empty param.name ? param.name : t0.name}" extra="maxlength='80' placeholder='Ex. : Assurance habitation'" />
          <label class="field ${not empty errors.description ? 'has-error' : ''}">Description
            <textarea name="description" rows="3" maxlength="500"><c:out value="${not empty param.description ? param.description : t0.description}" /></textarea>
            <c:if test="${not empty errors.description}"><span class="field-error"><c:out value="${errors.description}" /></span></c:if>
          </label>
          <ui:field name="basePrice" label="Prix de base mensuel (€) *" type="number" required="true" value="${not empty param.basePrice ? param.basePrice : t0.basePrice}" extra="min='0.01' step='0.01'" />
          <fieldset>
            <legend>Garanties disponibles</legend>
            <c:forEach items="${guarantees}" var="g">
              <c:set var="checked" value="false" />
              <c:forEach items="${paramValues.guaranteeIds}" var="pid"><c:if test="${pid == fn:trim(g.id)}"><c:set var="checked" value="true" /></c:if></c:forEach>
              <c:if test="${empty paramValues.guaranteeIds}">
                <c:forEach items="${t0.guarantees}" var="tg"><c:if test="${tg.id == g.id}"><c:set var="checked" value="true" /></c:if></c:forEach>
              </c:if>
              <label class="choice small"><input type="checkbox" name="guaranteeIds" value="${g.id}" ${checked ? 'checked' : ''}> <c:out value="${g.name}" /></label>
            </c:forEach>
            <c:if test="${not empty errors.guaranteeIds}"><span class="field-error"><c:out value="${errors.guaranteeIds}" /></span></c:if>
          </fieldset>
          <div class="inline">
            <button type="submit" class="btn btn-primary">${empty t0 ? 'Créer' : 'Enregistrer'}</button>
            <c:if test="${not empty t0}"><a class="btn" href="${ctx}/admin/types">Annuler</a></c:if>
          </div>
        </form>
      </div>
    </div>
  </main>
</div>
<script src="${ctx}/js/app.js"></script>
</body>
</html>
