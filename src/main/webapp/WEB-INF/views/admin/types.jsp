<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/taglibs.jspf" %>
<%-- AdminTypesServlet
     GET  /admin/types[?id=2]  -> liste + formulaire (création, ou modification si id)
     POST /admin/types         action=save   (id vide = création ; name, description, basePrice, guaranteeIds multiples)
                               action=active (id, value=true|false)
                               action=delete (id) -> refusé si des contrats utilisent ce type
     Attributs : types      List<InsuranceTypeRow>{id, name, description, basePrice, active, guarantees, contractCount, deletable}
                 type       InsuranceType en cours de modification (absent en création)
                 guarantees List<Guarantee> (cases à cocher)   errors Map<String,String>
     Ajouter un nouveau type d'assurance = une ligne en base, aucun code à modifier. --%>
<c:set var="title" value="Types d'assurance" />
<c:set var="t0" value="${type}" />
<c:set var="posted" value="${pageContext.request.method == 'POST'}" />
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
                  <span class="badge ${it.active ? 'badge-actif' : 'badge-suspendu'}">${it.active ? 'ACTIF' : 'DÉSACTIVÉ'}</span>
                  <span class="amount"><ui:amount value="${it.basePrice}" /> <small>/mois</small></span>
                  <span class="small muted">${it.contractCount} contrat(s)</span>
                </div>
              </div>
              <div class="inline"><c:forEach items="${it.guarantees}" var="g"><span class="chip"><c:out value="${g.name}" /></span></c:forEach></div>
              <div class="inline">
                <a class="btn btn-sm" href="${ctx}/admin/types?id=${it.id}">Modifier</a>
                <form method="post" action="${ctx}/admin/types"
                      <c:if test="${it.active}">data-confirm="Désactiver « ${fn:escapeXml(it.name)} » ? Il ne sera plus proposé pour un nouveau contrat."</c:if>>
                  <%@ include file="/WEB-INF/views/common/csrf.jspf" %>
                  <input type="hidden" name="action" value="active">
                  <input type="hidden" name="id" value="${it.id}">
                  <input type="hidden" name="value" value="${not it.active}">
                  <button class="btn btn-sm" type="submit">${it.active ? 'Désactiver' : 'Activer'}</button>
                </form>
                <c:if test="${it.deletable}">
                  <form method="post" action="${ctx}/admin/types" data-confirm="Supprimer le type « ${fn:escapeXml(it.name)} » ?">
                    <%@ include file="/WEB-INF/views/common/csrf.jspf" %>
                    <input type="hidden" name="action" value="delete">
                    <input type="hidden" name="id" value="${it.id}">
                    <button class="btn btn-sm btn-danger" type="submit">Supprimer</button>
                  </form>
                </c:if>
              </div>
            </article>
          </c:forEach>
          <c:if test="${empty types}"><div class="card"><p class="empty">Aucun type d'assurance.</p></div></c:if>
        </section>

        <%-- Valeurs du formulaire : celles envoyées après une erreur, sinon celles du type modifié --%>
        <c:set var="vName" value="${posted ? param.name : t0.name}" />
        <c:set var="vDescription" value="${posted ? param.description : t0.description}" />
        <c:set var="vPrice" value="${posted ? param.basePrice : t0.basePrice}" />
        <form class="card form col-side" method="post" action="${ctx}/admin/types" novalidate data-validate>
          <%@ include file="/WEB-INF/views/common/csrf.jspf" %>
          <input type="hidden" name="action" value="save">
          <input type="hidden" name="id" value="${t0.id}">
          <h2>${empty t0 ? 'Nouveau type' : 'Modifier le type'}</h2>
          <c:if test="${not empty t0}">
            <p class="small muted">Les contrats existants gardent leur prime et leurs garanties ; les changements valent pour les prochains contrats.</p>
          </c:if>
          <ui:field name="name" label="Nom *" required="true" value="${vName}" extra="maxlength='100' placeholder='Ex. : Assurance animaux'" />
          <label class="field ${not empty errors.description ? 'has-error' : ''}">Description
            <textarea name="description" rows="3" maxlength="500"><c:out value="${vDescription}" /></textarea>
            <c:if test="${not empty errors.description}"><span class="field-error"><c:out value="${errors.description}" /></span></c:if>
          </label>
          <ui:field name="basePrice" label="Prix de base mensuel (€) *" type="number" required="true" value="${vPrice}" extra="min='0.01' step='0.01'" />
          <fieldset class="${not empty errors.guaranteeIds ? 'has-error' : ''}">
            <legend>Garanties disponibles *</legend>
            <c:forEach items="${guarantees}" var="g">
              <c:set var="checked" value="false" />
              <c:choose>
                <c:when test="${posted}">
                  <c:forEach items="${paramValues.guaranteeIds}" var="pid"><c:if test="${pid == g.id}"><c:set var="checked" value="true" /></c:if></c:forEach>
                </c:when>
                <c:otherwise>
                  <c:forEach items="${t0.guarantees}" var="tg"><c:if test="${tg.id == g.id}"><c:set var="checked" value="true" /></c:if></c:forEach>
                </c:otherwise>
              </c:choose>
              <label class="choice small"><input type="checkbox" name="guaranteeIds" value="${g.id}" ${checked ? 'checked' : ''}> <c:out value="${g.name}" /></label>
            </c:forEach>
            <c:if test="${empty guarantees}"><p class="small muted">Aucune garantie : créez-en d'abord depuis l'espace agent.</p></c:if>
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
