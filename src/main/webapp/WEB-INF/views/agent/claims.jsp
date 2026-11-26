<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/taglibs.jspf" %>
<%-- AgentClaimsServlet (Views.AGENT_CLAIMS et Views.AGENT_CLAIM)
     GET  /agent/claims[?status=EN_ANALYSE&q=texte]  -> liste : attribut claims List<Claim>
     GET  /agent/claims?id=93                        -> détail : attributs claim Claim, history List<ClaimHistory>
     POST /agent/claims  id, action=status, status=EN_ANALYSE|ACCEPTE|REFUSE  -> ClaimService.changeStatus
     POST /agent/claims  id, action=indemnify, amount                         -> ClaimService.indemnify
     POST /agent/claims  id, action=comment, content                          -> ClaimService.addComment
     Détail : attribut attachments List<ClaimAttachment> (téléchargement : GET ${base}?attachment=ID)
     Détail : attribut comments List<ClaimComment> (content, createdAt, author{firstName, lastName}), notes internes
     En cas d'erreur, le détail est réaffiché avec errors Map<String,String> (clés : status, amount, content, action).
     Aussi utilisée par l'admin (/admin/claims) en consultation seule : les formulaires ne sont pas affichés.
     Le filtre par statut et la recherche sont faits ici, sur la liste complète renvoyée par le service.
     Claim : contract, contract.client et agent sont chargés (pas contract.insuranceType). --%>
<c:set var="title" value="Sinistres" />
<c:set var="isAgent" value="${sessionScope.currentUser.role == 'AGENT'}" />
<c:set var="base" value="${isAgent ? '/agent/claims' : '/admin/claims'}" />
<c:set var="q" value="${fn:toLowerCase(fn:trim(param.q))}" />
<!DOCTYPE html>
<html lang="fr">
<head><%@ include file="/WEB-INF/views/common/head.jspf" %></head>
<body>
<div class="app">
  <jsp:include page="/WEB-INF/views/common/sidebar.jsp"><jsp:param name="active" value="claims" /></jsp:include>
  <main class="main">
    <div class="container">
<c:choose>
  <%-- ===================== Détail d'un sinistre ===================== --%>
  <c:when test="${not empty claim}">
      <header class="page-head">
        <div class="stack-sm">
          <a class="back" href="${ctx}${base}">← Tous les sinistres</a>
          <h1><c:out value="${claim.number}" /></h1>
          <span class="muted"><c:out value="${claim.contract.client.firstName} ${claim.contract.client.lastName}" /> · contrat <c:out value="${claim.contract.number}" /> · <ui:claimType value="${claim.type}" /></span>
        </div>
        <ui:status value="${claim.status}" />
      </header>
      <%@ include file="/WEB-INF/views/common/flash.jspf" %>
      <c:if test="${not empty errors.action}"><div class="alert alert-error" role="alert"><span><c:out value="${errors.action}" /></span></div></c:if>

      <div class="row">
        <div class="col-main">
          <section class="card" aria-labelledby="h-decl">
            <h2 id="h-decl">Déclaration</h2>
            <div class="grid-3 small">
              <div class="stack-sm"><span class="muted">Survenu le</span><span class="strong"><ui:date value="${claim.claimDate}" /></span></div>
              <div class="stack-sm"><span class="muted">Déclaré le</span><span class="strong"><ui:date value="${claim.declaredAt}" /></span></div>
              <div class="stack-sm"><span class="muted">Montant estimé</span><span class="strong"><ui:amount value="${claim.estimatedAmount}" /></span></div>
            </div>
            <p class="quote">« <c:out value="${claim.description}" /> »</p>
            <div class="grid-2 small">
              <div class="stack-sm"><span class="muted">Client</span><span class="strong"><c:out value="${claim.contract.client.firstName} ${claim.contract.client.lastName}" /></span><span class="muted"><c:out value="${claim.contract.client.email}" /></span></div>
              <div class="stack-sm"><span class="muted">Agent en charge</span><span class="strong">
                <c:choose><c:when test="${not empty claim.agent}"><c:out value="${claim.agent.firstName} ${claim.agent.lastName}" /></c:when><c:otherwise>—</c:otherwise></c:choose></span></div>
            </div>
          </section>

          <section class="card" id="attachments" aria-labelledby="h-att">
            <h2 id="h-att">Pièces justificatives</h2>
            <c:set var="attachmentUrl" value="${ctx}${base}?attachment=" />
            <%@ include file="/WEB-INF/views/common/claim-attachments.jspf" %>
          </section>

          <c:if test="${isAgent}">
            <%-- Transitions autorisées par ClaimService : DECLARE -> EN_ANALYSE ; EN_ANALYSE -> ACCEPTE | REFUSE --%>
            <c:if test="${claim.status == 'DECLARE' or claim.status == 'EN_ANALYSE'}">
              <section class="card" aria-labelledby="h-statut">
                <h2 id="h-statut">Changer le statut</h2>
                <form class="form" method="post" action="${ctx}/agent/claims" novalidate data-validate>
                  <%@ include file="/WEB-INF/views/common/csrf.jspf" %>
                  <input type="hidden" name="id" value="${claim.id}">
                  <input type="hidden" name="action" value="status">
                  <label class="field ${not empty errors.status ? 'has-error' : ''}">Nouveau statut
                    <select name="status" required>
                      <c:choose>
                        <c:when test="${claim.status == 'DECLARE'}"><option value="EN_ANALYSE">Passer en analyse</option></c:when>
                        <c:otherwise>
                          <option value="ACCEPTE">Accepter</option>
                          <option value="REFUSE">Refuser</option>
                        </c:otherwise>
                      </c:choose>
                    </select>
                    <c:if test="${not empty errors.status}"><span class="field-error"><c:out value="${errors.status}" /></span></c:if>
                  </label>
                  <button type="submit" class="btn btn-primary">Enregistrer</button>
                </form>
              </section>
            </c:if>

            <c:if test="${claim.status == 'ACCEPTE'}">
              <section class="card" aria-labelledby="h-indem">
                <h2 id="h-indem">Indemniser</h2>
                <form class="form" method="post" action="${ctx}/agent/claims" novalidate data-validate>
                  <%@ include file="/WEB-INF/views/common/csrf.jspf" %>
                  <input type="hidden" name="id" value="${claim.id}">
                  <input type="hidden" name="action" value="indemnify">
                  <ui:field name="amount" label="Montant de l'indemnisation (€) *" type="number" required="true"
                            extra="min='0.01' step='0.01' inputmode='decimal'"
                            help="Le sinistre passera au statut INDEMNISÉ." />
                  <button type="submit" class="btn btn-primary">Indemniser</button>
                </form>
              </section>
            </c:if>
          </c:if>

          <section class="card" id="comments" aria-labelledby="h-comm">
            <h2 id="h-comm">Commentaires <span class="xsmall muted">(internes, non visibles par le client)</span></h2>
            <c:choose>
              <c:when test="${empty comments}"><p class="small muted">Aucun commentaire.</p></c:when>
              <c:otherwise>
                <ul class="stack-sm small" aria-label="Commentaires de l'agent">
                  <c:forEach var="cm" items="${comments}">
                    <li class="stack-sm">
                      <span class="xsmall muted"><c:out value="${cm.author.firstName} ${cm.author.lastName}" /> · <ui:date value="${cm.createdAt}" /><c:if test="${not empty cm.createdAt}"> à ${cm.createdAt.hour lt 10 ? '0' : ''}${cm.createdAt.hour}h${cm.createdAt.minute lt 10 ? '0' : ''}${cm.createdAt.minute}</c:if></span>
                      <p class="quote" style="white-space: pre-line"><c:out value="${cm.content}" /></p>
                    </li>
                  </c:forEach>
                </ul>
              </c:otherwise>
            </c:choose>

            <c:if test="${isAgent}">
              <form class="form" method="post" action="${ctx}/agent/claims" novalidate data-validate>
                <%@ include file="/WEB-INF/views/common/csrf.jspf" %>
                <input type="hidden" name="id" value="${claim.id}">
                <input type="hidden" name="action" value="comment">
                <label class="field ${not empty errors.content ? 'has-error' : ''}">Ajouter un commentaire
                  <textarea name="content" rows="3" maxlength="2000" required
                            <c:if test="${not empty errors.content}">aria-invalid="true" aria-describedby="e-content"</c:if>><c:out value="${param.action == 'comment' ? param.content : ''}" /></textarea>
                  <c:if test="${not empty errors.content}"><span class="field-error" id="e-content"><c:out value="${errors.content}" /></span></c:if>
                </label>
                <button type="submit" class="btn btn-primary">Publier</button>
              </form>
            </c:if>
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
  </c:when>

  <%-- ===================== Liste des sinistres ===================== --%>
  <c:otherwise>
      <header class="page-head">
        <div class="stack-sm">
          <span class="eyebrow">${isAgent ? 'Espace agent' : 'Administration'}</span>
          <h1>${isAgent ? 'Sinistres à traiter' : 'Tous les sinistres'}</h1>
        </div>
        <form class="search" method="get" action="${ctx}${base}">
          <c:if test="${not empty param.status}"><input type="hidden" name="status" value="${fn:escapeXml(param.status)}"></c:if>
          <label class="field">Rechercher
            <input type="search" name="q" value="${fn:escapeXml(param.q)}" placeholder="N° de sinistre, client, contrat…">
          </label>
          <button class="btn" type="submit">Rechercher</button>
        </form>
      </header>
      <%@ include file="/WEB-INF/views/common/flash.jspf" %>

      <%-- Sinistres qui correspondent à la recherche (avant le filtre par statut, pour les compteurs des onglets) --%>
      <jsp:useBean id="matches" class="java.util.ArrayList" />
      <c:forEach items="${claims}" var="clm">
        <c:set var="text" value="${fn:toLowerCase(clm.number)} ${fn:toLowerCase(clm.contract.number)} ${fn:toLowerCase(clm.contract.client.firstName)} ${fn:toLowerCase(clm.contract.client.lastName)}" />
        <c:if test="${empty q or fn:contains(text, q)}"><c:set var="ignore" value="${matches.add(clm)}" /></c:if>
      </c:forEach>
      <jsp:useBean id="counters" class="java.util.HashMap" />
      <c:forEach items="${matches}" var="clm">
        <c:set var="key" value="${clm.status.name()}" />
        <c:set var="ignore" value="${counters.put(key, (empty counters[key] ? 0 : counters[key]) + 1)}" />
      </c:forEach>

      <nav class="tabs" aria-label="Filtrer par statut">
        <c:url var="linkAll" value="${base}"><c:param name="q" value="${param.q}" /></c:url>
        <a href="${linkAll}" class="${empty param.status ? 'active' : ''}">Tous · ${fn:length(matches)}</a>
        <c:forEach items="DECLARE,EN_ANALYSE,ACCEPTE,REFUSE,INDEMNISE" var="st">
          <c:url var="linkStatus" value="${base}"><c:param name="status" value="${st}" /><c:param name="q" value="${param.q}" /></c:url>
          <a href="${linkStatus}" class="${param.status == st ? 'active' : ''}"><ui:status value="${st}" /> ${empty counters[st] ? 0 : counters[st]}</a>
        </c:forEach>
      </nav>

      <section class="card flush" aria-label="Liste des sinistres">
        <c:set var="shown" value="0" />
        <div class="table-wrap">
          <table class="table">
            <thead><tr><th>N° sinistre</th><th>Client</th><th>Contrat</th><th>Type</th><th>Déclaré le</th><th class="right">Estimé</th><th>Statut</th></tr></thead>
            <tbody>
              <c:forEach items="${matches}" var="clm">
                <c:if test="${empty param.status or clm.status.name() == param.status}">
                  <c:set var="shown" value="${shown + 1}" />
                  <tr>
                    <td class="strong"><a href="${ctx}${base}?id=${clm.id}"><c:out value="${clm.number}" /></a></td>
                    <td><c:out value="${clm.contract.client.firstName} ${clm.contract.client.lastName}" /></td>
                    <td class="muted"><c:out value="${clm.contract.number}" /></td>
                    <td class="muted"><ui:claimType value="${clm.type}" /></td>
                    <td class="muted"><ui:date value="${clm.declaredAt}" /></td>
                    <td class="right strong"><ui:amount value="${clm.estimatedAmount}" /></td>
                    <td><ui:status value="${clm.status}" /></td>
                  </tr>
                </c:if>
              </c:forEach>
              <c:if test="${shown == 0}"><tr><td colspan="7" class="empty">Aucun sinistre pour ce filtre.</td></tr></c:if>
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
