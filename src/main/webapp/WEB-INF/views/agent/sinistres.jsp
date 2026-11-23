<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/taglibs.jspf" %>
<%-- AgentClaimsServlet (Views.AGENT_CLAIMS et Views.AGENT_CLAIM)
     GET  /agent/claims[?statut=EN_ANALYSE&q=texte]  -> liste : attribut claims List<Claim>
     GET  /agent/claims?id=93                        -> détail : attribut claim Claim
     POST /agent/claims  id, action=status, status=EN_ANALYSE|ACCEPTE|REFUSE  -> ClaimService.changeStatus
     POST /agent/claims  id, action=indemnify, amount                         -> ClaimService.indemnify
     En cas d'erreur, le détail est réaffiché avec erreurs Map<String,String> (clés : status, amount, action).
     Aussi utilisée par l'admin (/admin/claims) en consultation seule : les formulaires ne sont pas affichés.
     Le filtre par statut et la recherche sont faits ici, sur la liste complète renvoyée par le service.
     Claim : contract, contract.client et agent sont chargés (pas contract.insuranceType). --%>
<c:set var="titre" value="Sinistres" />
<c:set var="estAgent" value="${sessionScope.utilisateur.role == 'AGENT'}" />
<c:set var="base" value="${estAgent ? '/agent/claims' : '/admin/claims'}" />
<c:set var="q" value="${fn:toLowerCase(fn:trim(param.q))}" />
<!DOCTYPE html>
<html lang="fr">
<head><%@ include file="/WEB-INF/views/common/head.jspf" %></head>
<body>
<div class="app">
  <jsp:include page="/WEB-INF/views/common/sidebar.jsp"><jsp:param name="actif" value="sinistres" /></jsp:include>
  <main class="main">
    <div class="container">
<c:choose>
  <%-- ===================== Détail d'un sinistre ===================== --%>
  <c:when test="${not empty claim}">
      <c:set var="sin" value="${claim}" />
      <header class="page-head">
        <div class="stack-sm">
          <a class="back" href="${ctx}${base}">← Tous les sinistres</a>
          <h1><c:out value="${sin.number}" /></h1>
          <span class="muted"><c:out value="${sin.contract.client.firstName} ${sin.contract.client.lastName}" /> · contrat <c:out value="${sin.contract.number}" /> · <ui:typeSinistre valeur="${sin.type}" /></span>
        </div>
        <ui:statut valeur="${sin.status}" />
      </header>
      <%@ include file="/WEB-INF/views/common/flash.jspf" %>
      <c:if test="${not empty erreurs.action}"><div class="alert alert-error" role="alert"><span><c:out value="${erreurs.action}" /></span></div></c:if>

      <div class="row">
        <div class="col-main">
          <section class="card" aria-labelledby="h-decl">
            <h2 id="h-decl">Déclaration</h2>
            <div class="grid-3 small">
              <div class="stack-sm"><span class="muted">Survenu le</span><span class="strong"><ui:date valeur="${sin.claimDate}" /></span></div>
              <div class="stack-sm"><span class="muted">Déclaré le</span><span class="strong"><ui:date valeur="${sin.declaredAt}" /></span></div>
              <div class="stack-sm"><span class="muted">Montant estimé</span><span class="strong"><ui:montant valeur="${sin.estimatedAmount}" /></span></div>
            </div>
            <p class="quote">« <c:out value="${sin.description}" /> »</p>
            <div class="grid-2 small">
              <div class="stack-sm"><span class="muted">Client</span><span class="strong"><c:out value="${sin.contract.client.firstName} ${sin.contract.client.lastName}" /></span><span class="muted"><c:out value="${sin.contract.client.email}" /></span></div>
              <div class="stack-sm"><span class="muted">Agent en charge</span><span class="strong">
                <c:choose><c:when test="${not empty sin.agent}"><c:out value="${sin.agent.firstName} ${sin.agent.lastName}" /></c:when><c:otherwise>—</c:otherwise></c:choose></span></div>
            </div>
          </section>

          <c:if test="${estAgent}">
            <%-- Transitions autorisées par ClaimService : DECLARE -> EN_ANALYSE ; EN_ANALYSE -> ACCEPTE | REFUSE --%>
            <c:if test="${sin.status == 'DECLARE' or sin.status == 'EN_ANALYSE'}">
              <section class="card" aria-labelledby="h-statut">
                <h2 id="h-statut">Changer le statut</h2>
                <form class="form" method="post" action="${ctx}/agent/claims" novalidate data-validate>
                  <%@ include file="/WEB-INF/views/common/csrf.jspf" %>
                  <input type="hidden" name="id" value="${sin.id}">
                  <input type="hidden" name="action" value="status">
                  <label class="field ${not empty erreurs.status ? 'has-error' : ''}">Nouveau statut
                    <select name="status" required>
                      <c:choose>
                        <c:when test="${sin.status == 'DECLARE'}"><option value="EN_ANALYSE">Passer en analyse</option></c:when>
                        <c:otherwise>
                          <option value="ACCEPTE">Accepter</option>
                          <option value="REFUSE">Refuser</option>
                        </c:otherwise>
                      </c:choose>
                    </select>
                    <c:if test="${not empty erreurs.status}"><span class="field-error"><c:out value="${erreurs.status}" /></span></c:if>
                  </label>
                  <button type="submit" class="btn btn-primary">Enregistrer</button>
                </form>
              </section>
            </c:if>

            <c:if test="${sin.status == 'ACCEPTE'}">
              <section class="card" aria-labelledby="h-indem">
                <h2 id="h-indem">Indemniser</h2>
                <form class="form" method="post" action="${ctx}/agent/claims" novalidate data-validate>
                  <%@ include file="/WEB-INF/views/common/csrf.jspf" %>
                  <input type="hidden" name="id" value="${sin.id}">
                  <input type="hidden" name="action" value="indemnify">
                  <ui:champ nom="amount" libelle="Montant de l'indemnisation (€) *" type="number" requis="true"
                            extra="min='0.01' step='0.01' inputmode='decimal'"
                            aide="Le sinistre passera au statut INDEMNISÉ." />
                  <button type="submit" class="btn btn-primary">Indemniser</button>
                </form>
              </section>
            </c:if>
          </c:if>
        </div>

        <aside class="col-side">
          <section class="card" aria-labelledby="h-suivi">
            <h2 id="h-suivi">Avancement</h2>
            <%@ include file="/WEB-INF/views/common/suivi-sinistre.jspf" %>
          </section>
        </aside>
      </div>
  </c:when>

  <%-- ===================== Liste des sinistres ===================== --%>
  <c:otherwise>
      <header class="page-head">
        <div class="stack-sm">
          <span class="eyebrow">${estAgent ? 'Espace agent' : 'Administration'}</span>
          <h1>${estAgent ? 'Sinistres à traiter' : 'Tous les sinistres'}</h1>
        </div>
        <form class="search" method="get" action="${ctx}${base}">
          <c:if test="${not empty param.statut}"><input type="hidden" name="statut" value="${fn:escapeXml(param.statut)}"></c:if>
          <label class="field">Rechercher
            <input type="search" name="q" value="${fn:escapeXml(param.q)}" placeholder="N° de sinistre, client, contrat…">
          </label>
          <button class="btn" type="submit">Rechercher</button>
        </form>
      </header>
      <%@ include file="/WEB-INF/views/common/flash.jspf" %>

      <%-- Sinistres qui correspondent à la recherche (avant le filtre par statut, pour les compteurs des onglets) --%>
      <jsp:useBean id="trouves" class="java.util.ArrayList" />
      <c:forEach items="${claims}" var="si">
        <c:set var="texte" value="${fn:toLowerCase(si.number)} ${fn:toLowerCase(si.contract.number)} ${fn:toLowerCase(si.contract.client.firstName)} ${fn:toLowerCase(si.contract.client.lastName)}" />
        <c:if test="${empty q or fn:contains(texte, q)}"><c:set var="ignore" value="${trouves.add(si)}" /></c:if>
      </c:forEach>
      <jsp:useBean id="compteurs" class="java.util.HashMap" />
      <c:forEach items="${trouves}" var="si">
        <c:set var="cle" value="${si.status.name()}" />
        <c:set var="ignore" value="${compteurs.put(cle, (empty compteurs[cle] ? 0 : compteurs[cle]) + 1)}" />
      </c:forEach>

      <nav class="tabs" aria-label="Filtrer par statut">
        <c:url var="lienTous" value="${base}"><c:param name="q" value="${param.q}" /></c:url>
        <a href="${lienTous}" class="${empty param.statut ? 'active' : ''}">Tous · ${fn:length(trouves)}</a>
        <c:forEach items="DECLARE,EN_ANALYSE,ACCEPTE,REFUSE,INDEMNISE" var="st">
          <c:url var="lienSt" value="${base}"><c:param name="statut" value="${st}" /><c:param name="q" value="${param.q}" /></c:url>
          <a href="${lienSt}" class="${param.statut == st ? 'active' : ''}"><ui:statut valeur="${st}" /> ${empty compteurs[st] ? 0 : compteurs[st]}</a>
        </c:forEach>
      </nav>

      <section class="card flush" aria-label="Liste des sinistres">
        <c:set var="affiches" value="0" />
        <div class="table-wrap">
          <table class="table">
            <thead><tr><th>N° sinistre</th><th>Client</th><th>Contrat</th><th>Type</th><th>Déclaré le</th><th class="right">Estimé</th><th>Statut</th></tr></thead>
            <tbody>
              <c:forEach items="${trouves}" var="si">
                <c:if test="${empty param.statut or si.status.name() == param.statut}">
                  <c:set var="affiches" value="${affiches + 1}" />
                  <tr>
                    <td class="strong"><a href="${ctx}${base}?id=${si.id}"><c:out value="${si.number}" /></a></td>
                    <td><c:out value="${si.contract.client.firstName} ${si.contract.client.lastName}" /></td>
                    <td class="muted"><c:out value="${si.contract.number}" /></td>
                    <td class="muted"><ui:typeSinistre valeur="${si.type}" /></td>
                    <td class="muted"><ui:date valeur="${si.declaredAt}" /></td>
                    <td class="right strong"><ui:montant valeur="${si.estimatedAmount}" /></td>
                    <td><ui:statut valeur="${si.status}" /></td>
                  </tr>
                </c:if>
              </c:forEach>
              <c:if test="${affiches == 0}"><tr><td colspan="7" class="empty">Aucun sinistre pour ce filtre.</td></tr></c:if>
            </tbody>
          </table>
        </div>
      </section>
  </c:otherwise>
</c:choose>
    </div>
  </main>
</div>
<script src="${ctx}/js/app.js"></script>
</body>
</html>
