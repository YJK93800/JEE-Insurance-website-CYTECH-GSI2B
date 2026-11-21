<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/taglibs.jspf" %>
<%-- GET  /agent/garanties[?id=4]  -> liste + formulaire (création, ou modification si id)
     POST /agent/garanties         (id vide = création ; nom, description, plafond, franchise)
     POST /agent/garanties/supprimer (id) -> refusé par la servlet si la garantie est utilisée par un contrat
     Attributs : garanties List<Garantie>{id, nom, description, plafond, franchise, nbContrats}
                 garantie  Garantie en cours de modification (null en création)   erreurs Map<String,String>
     L'association garantie <-> contrat se fait dans le formulaire de contrat. --%>
<c:set var="titre" value="Garanties" />
<c:set var="g0" value="${garantie}" />
<!DOCTYPE html>
<html lang="fr">
<head><%@ include file="/WEB-INF/views/common/head.jspf" %></head>
<body>
<div class="app">
  <jsp:include page="/WEB-INF/views/common/sidebar.jsp"><jsp:param name="actif" value="garanties" /></jsp:include>
  <main class="main">
    <div class="container">
      <header class="page-head"><div class="stack-sm"><span class="eyebrow">Espace agent</span><h1>Garanties</h1></div></header>
      <%@ include file="/WEB-INF/views/common/flash.jspf" %>

      <div class="row">
        <section class="card flush col-main" aria-label="Liste des garanties">
          <div class="table-wrap">
            <table class="table">
              <thead><tr><th>Garantie</th><th>Description</th><th class="right">Plafond</th><th class="right">Franchise</th><th class="right">Contrats</th><th class="right">Actions</th></tr></thead>
              <tbody>
                <c:forEach items="${garanties}" var="g">
                  <tr class="${g0.id == g.id ? 'selected' : ''}">
                    <td class="strong"><c:out value="${g.nom}" /></td>
                    <td class="muted small"><c:out value="${g.description}" /></td>
                    <td class="right"><ui:montant valeur="${g.plafond}" /></td>
                    <td class="right"><ui:montant valeur="${g.franchise}" /></td>
                    <td class="right">${g.nbContrats}</td>
                    <td><div class="actions">
                      <a class="btn btn-sm" href="${ctx}/agent/garanties?id=${g.id}">Modifier</a>
                      <form method="post" action="${ctx}/agent/garanties/supprimer" data-confirm="Supprimer la garantie « ${fn:escapeXml(g.nom)} » ?">
                        <%@ include file="/WEB-INF/views/common/csrf.jspf" %>
                        <input type="hidden" name="id" value="${g.id}">
                        <button class="btn-link danger" type="submit" ${g.nbContrats > 0 ? 'disabled title="Utilisée par des contrats"' : ''}>Supprimer</button>
                      </form>
                    </div></td>
                  </tr>
                </c:forEach>
                <c:if test="${empty garanties}"><tr><td colspan="6" class="empty">Aucune garantie.</td></tr></c:if>
              </tbody>
            </table>
          </div>
        </section>

        <form class="card form col-side" method="post" action="${ctx}/agent/garanties" novalidate data-validate>
          <%@ include file="/WEB-INF/views/common/csrf.jspf" %>
          <input type="hidden" name="id" value="${g0.id}">
          <h2>${empty g0 ? 'Nouvelle garantie' : 'Modifier la garantie'}</h2>
          <ui:champ nom="nom" libelle="Nom *" requis="true" valeur="${not empty param.nom ? param.nom : g0.nom}" extra="maxlength='80' placeholder='Ex. : Dégâts des eaux'" />
          <label class="field ${not empty erreurs.description ? 'has-error' : ''}">Description
            <textarea name="description" rows="3" maxlength="500"><c:out value="${not empty param.description ? param.description : g0.description}" /></textarea>
            <c:if test="${not empty erreurs.description}"><span class="field-error"><c:out value="${erreurs.description}" /></span></c:if>
          </label>
          <div class="grid-2">
            <ui:champ nom="plafond" libelle="Plafond (€)" type="number" valeur="${not empty param.plafond ? param.plafond : g0.plafond}" extra="min='0' step='0.01'" />
            <ui:champ nom="franchise" libelle="Franchise (€)" type="number" valeur="${not empty param.franchise ? param.franchise : g0.franchise}" extra="min='0' step='0.01'" />
          </div>
          <div class="inline">
            <button type="submit" class="btn btn-primary">${empty g0 ? 'Créer' : 'Enregistrer'}</button>
            <c:if test="${not empty g0}"><a class="btn" href="${ctx}/agent/garanties">Annuler</a></c:if>
          </div>
        </form>
      </div>
    </div>
  </main>
</div>
<script src="${ctx}/js/app.js"></script>
</body>
</html>
