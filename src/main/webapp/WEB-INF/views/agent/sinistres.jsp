<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/taglibs.jspf" %>
<%-- GET  /agent/sinistres?statut=EN_ANALYSE&q=texte&id=93
     POST /agent/sinistres/traiter (sinistreId, statut, montantIndemnisation, commentaire, visibleClient) -> redirection vers ?id=...
     Attributs : sinistres   List<Sinistre> (filtrés par statut et recherche)
                 compteurs   Map<String,Long> nombre par statut (clé = nom de l'enum) + "TOUS"
                 statuts     StatutSinistre[]
                 selection   Sinistre affiché à droite (peut être null)
                 analyseIA   String  pré-analyse générée par l'API d'IA (peut être null)
     Aussi utilisée par l'admin (/admin/sinistres) : le formulaire de décision n'est alors pas affiché. --%>
<c:set var="titre" value="Sinistres" />
<c:set var="estAgent" value="${sessionScope.utilisateur.role == 'AGENT'}" />
<c:set var="base" value="${estAgent ? '/agent/sinistres' : '/admin/sinistres'}" />
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

      <nav class="tabs" aria-label="Filtrer par statut">
        <c:url var="lienTous" value="${base}"><c:param name="q" value="${param.q}" /></c:url>
        <a href="${lienTous}" class="${empty param.statut ? 'active' : ''}">Tous · ${compteurs['TOUS']}</a>
        <c:forEach items="${statuts}" var="st">
          <c:url var="lienSt" value="${base}"><c:param name="statut" value="${st}" /><c:param name="q" value="${param.q}" /></c:url>
          <a href="${lienSt}" class="${param.statut == st.name() ? 'active' : ''}">${st} · ${empty compteurs[st.name()] ? 0 : compteurs[st.name()]}</a>
        </c:forEach>
      </nav>

      <div class="row">
        <section class="card flush col-main" aria-label="Liste des sinistres">
          <c:choose>
            <c:when test="${empty sinistres}"><p class="empty">Aucun sinistre pour ce filtre.</p></c:when>
            <c:otherwise>
              <div class="table-wrap">
                <table class="table">
                  <thead><tr><th>N° sinistre</th><th>Client</th><th>Type</th><th>Déclaré le</th><th class="right">Estimé</th><th>Statut</th></tr></thead>
                  <tbody>
                    <c:forEach items="${sinistres}" var="si">
                      <c:url var="lienSi" value="${base}"><c:param name="statut" value="${param.statut}" /><c:param name="q" value="${param.q}" /><c:param name="id" value="${si.id}" /></c:url>
                      <tr class="${selection.id == si.id ? 'selected' : ''}">
                        <td class="strong"><a href="${lienSi}"><c:out value="${si.numero}" /></a></td>
                        <td><c:out value="${si.contrat.client.prenom} ${si.contrat.client.nom}" /></td>
                        <td class="muted"><c:out value="${si.typeSinistre.libelle}" /></td>
                        <td class="muted"><ui:date valeur="${si.dateDeclaration}" /></td>
                        <td class="right strong"><ui:montant valeur="${si.montantEstime}" /></td>
                        <td><ui:statut valeur="${si.statut}" /></td>
                      </tr>
                    </c:forEach>
                  </tbody>
                </table>
              </div>
            </c:otherwise>
          </c:choose>
        </section>

        <c:if test="${not empty selection}">
          <c:set var="sin" value="${selection}" />
          <aside class="card col-side" aria-labelledby="h-detail">
            <div class="stack-sm">
              <div class="between"><h2 id="h-detail"><c:out value="${sin.numero}" /></h2><ui:statut valeur="${sin.statut}" /></div>
              <span class="small muted"><c:out value="${sin.contrat.client.prenom} ${sin.contrat.client.nom}" /> · <c:out value="${sin.contrat.typeAssurance.nom}" /> <c:out value="${sin.contrat.numero}" /> · <c:out value="${sin.typeSinistre.libelle}" /></span>
            </div>
            <div class="grid-2 small">
              <div class="stack-sm"><span class="muted">Survenu le</span><span class="strong"><ui:date valeur="${sin.dateSinistre}" /></span></div>
              <div class="stack-sm"><span class="muted">Estimé</span><span class="strong"><ui:montant valeur="${sin.montantEstime}" /></span></div>
            </div>
            <p class="quote">« <c:out value="${sin.description}" /> »</p>

            <div class="stack">
              <span class="small strong">Pièces justificatives (${fn:length(sin.pieces)})</span>
              <div class="inline">
                <c:forEach items="${sin.pieces}" var="pj"><a class="btn btn-sm" href="${ctx}/fichiers/piece?id=${pj.id}"><c:out value="${pj.nomFichier}" /></a></c:forEach>
                <c:if test="${empty sin.pieces}"><span class="small muted">Aucune pièce fournie.</span></c:if>
              </div>
            </div>

            <c:if test="${not empty analyseIA}">
              <div class="ai-box"><span class="badge-ai">PRÉ-ANALYSE IA</span><span><c:out value="${analyseIA}" /></span></div>
            </c:if>

            <c:if test="${estAgent}">
              <form class="form" method="post" action="${ctx}/agent/sinistres/traiter" novalidate data-validate>
                <%@ include file="/WEB-INF/views/common/csrf.jspf" %>
                <input type="hidden" name="sinistreId" value="${sin.id}">
                <label class="field ${not empty erreurs.statut ? 'has-error' : ''}">Nouveau statut
                  <select name="statut" required data-toggle-indemnisation>
                    <c:forEach items="${statuts}" var="st">
                      <option value="${st}" ${sin.statut == st ? 'selected' : ''}>${st}</option>
                    </c:forEach>
                  </select>
                  <c:if test="${not empty erreurs.statut}"><span class="field-error"><c:out value="${erreurs.statut}" /></span></c:if>
                </label>
                <ui:champ nom="montantIndemnisation" libelle="Montant d'indemnisation (€)" type="number"
                          valeur="${sin.montantIndemnisation}" extra="min='0' step='0.01' data-indemnisation"
                          aide="Obligatoire pour passer au statut INDEMNISE." />
                <label class="field">Commentaire
                  <textarea name="commentaire" rows="3" maxlength="1000"><c:out value="${param.commentaire}" /></textarea>
                </label>
                <label class="choice small"><input type="checkbox" name="visibleClient" value="true" checked> Visible par le client</label>
                <button type="submit" class="btn btn-primary btn-block">Enregistrer la décision</button>
              </form>
            </c:if>

            <div class="stack">
              <span class="small strong">Historique</span>
              <div class="history">
                <c:forEach items="${sin.commentaires}" var="cm">
                  <span><strong><ui:date valeur="${cm.date}" /></strong> · <c:out value="${cm.auteur.prenom} ${cm.auteur.nom}" /> : <c:out value="${cm.texte}" /></span>
                </c:forEach>
                <span><strong><ui:date valeur="${sin.dateDeclaration}" /></strong> · Déclaration reçue (DECLARE)</span>
              </div>
            </div>
          </aside>
        </c:if>
      </div>
    </div>
  </main>
</div>
<script src="${ctx}/js/app.js"></script>
</body>
</html>
