<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/taglibs.jspf" %>
<%-- AgentGuaranteesServlet
     GET  /agent/guarantees[?id=4]  -> liste + formulaire (création, ou modification si id)
     POST /agent/guarantees         action=save   (id vide = création ; name, description, coverageLimit, deductible)
                                    action=delete (id) -> refusé si la garantie est utilisée par un contrat
     Attributs : guarantees List<GuaranteeRow>{id, name, description, coverageLimit, deductible, contractCount, deletable}
                 guarantee  Guarantee en cours de modification (absent en création)   errors Map<String,String>
     L'association garantie <-> contrat se fait dans le formulaire de contrat. --%>
<c:set var="title" value="Garanties" />
<c:set var="g0" value="${guarantee}" />
<c:set var="posted" value="${pageContext.request.method == 'POST'}" />
<!DOCTYPE html>
<html lang="fr">
<head><%@ include file="/WEB-INF/views/common/head.jspf" %></head>
<body>
<div class="app">
  <jsp:include page="/WEB-INF/views/common/sidebar.jsp"><jsp:param name="active" value="guarantees" /></jsp:include>
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
                <c:forEach items="${guarantees}" var="g">
                  <tr class="${g0.id == g.id ? 'selected' : ''}">
                    <td class="strong"><c:out value="${g.name}" /></td>
                    <td class="muted small"><c:out value="${g.description}" /></td>
                    <td class="right"><c:choose><c:when test="${not empty g.coverageLimit}"><ui:amount value="${g.coverageLimit}" /></c:when><c:otherwise><span class="muted">—</span></c:otherwise></c:choose></td>
                    <td class="right"><c:choose><c:when test="${not empty g.deductible}"><ui:amount value="${g.deductible}" /></c:when><c:otherwise><span class="muted">Aucune</span></c:otherwise></c:choose></td>
                    <td class="right">${g.contractCount}</td>
                    <td><div class="actions">
                      <a class="btn btn-sm" href="${ctx}/agent/guarantees?id=${g.id}">Modifier</a>
                      <c:choose>
                        <c:when test="${g.deletable}">
                          <form method="post" action="${ctx}/agent/guarantees" data-confirm="Supprimer « ${fn:escapeXml(g.name)} » ?"
                                data-confirm-detail="Elle sera aussi retirée des types d'assurance qui la proposent. Cette action est irréversible.">
                            <%@ include file="/WEB-INF/views/common/csrf.jspf" %>
                            <input type="hidden" name="action" value="delete">
                            <input type="hidden" name="id" value="${g.id}">
                            <button class="btn-link danger" type="submit">Supprimer</button>
                          </form>
                        </c:when>
                        <c:otherwise>
                          <%-- Un <span> plutôt qu'un bouton disabled : l'infobulle s'affiche sur tous les navigateurs --%>
                          <span class="btn-link is-disabled" role="button" aria-disabled="true"
                                title="Indisponible : utilisée par ${g.contractCount} contrat(s)">Supprimer</span>
                        </c:otherwise>
                      </c:choose>
                    </div></td>
                  </tr>
                </c:forEach>
                <c:if test="${empty guarantees}"><tr><td colspan="6" class="empty">Aucune garantie.</td></tr></c:if>
              </tbody>
            </table>
          </div>
        </section>

        <%-- Valeurs du formulaire : celles envoyées après une erreur, sinon celles de la garantie modifiée --%>
        <c:set var="vName" value="${posted ? param.name : g0.name}" />
        <c:set var="vDescription" value="${posted ? param.description : g0.description}" />
        <c:set var="vLimit" value="${posted ? param.coverageLimit : g0.coverageLimit}" />
        <c:set var="vDeductible" value="${posted ? param.deductible : g0.deductible}" />
        <form class="card form col-side" method="post" action="${ctx}/agent/guarantees" novalidate data-validate>
          <%@ include file="/WEB-INF/views/common/csrf.jspf" %>
          <input type="hidden" name="action" value="save">
          <input type="hidden" name="id" value="${g0.id}">
          <h2>${empty g0 ? 'Nouvelle garantie' : 'Modifier la garantie'}</h2>
          <c:if test="${not empty g0 and not posted}">
            <c:forEach items="${guarantees}" var="g"><c:if test="${g.id == g0.id and g.contractCount > 0}">
              <p class="small muted">Utilisée par ${g.contractCount} contrat(s) : les modifications s'appliquent aussi à ces contrats.</p>
            </c:if></c:forEach>
          </c:if>
          <ui:field name="name" label="Nom *" required="true" value="${vName}" extra="maxlength='100' placeholder='Ex. : Bris de glace'" />
          <label class="field ${not empty errors.description ? 'has-error' : ''}">Description
            <textarea name="description" rows="3" maxlength="500"><c:out value="${vDescription}" /></textarea>
            <c:if test="${not empty errors.description}"><span class="field-error"><c:out value="${errors.description}" /></span></c:if>
          </label>
          <div class="grid-2">
            <ui:field name="coverageLimit" label="Plafond (€)" type="number" value="${vLimit}" help="Vide = pas de plafond" extra="min='0.01' step='0.01'" />
            <ui:field name="deductible" label="Franchise (€)" type="number" value="${vDeductible}" help="Vide = pas de franchise" extra="min='0.01' step='0.01'" />
          </div>
          <div class="inline">
            <button type="submit" class="btn btn-primary">${empty g0 ? 'Créer' : 'Enregistrer'}</button>
            <c:if test="${not empty g0}"><a class="btn" href="${ctx}/agent/guarantees">Annuler</a></c:if>
          </div>
        </form>
      </div>
    </div>
  </main>
</div>
<script src="${ctx}/js/app.js"></script>
</body>
</html>
