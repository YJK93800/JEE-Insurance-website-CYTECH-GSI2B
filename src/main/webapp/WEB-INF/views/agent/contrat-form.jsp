<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/taglibs.jspf" %>
<%-- GET  /agent/contrats/edit          -> création       GET /agent/contrats/edit?id=12 -> modification
     POST /agent/contrats/edit (id vide pour une création) -> ContratServlet
     Paramètres : id, clientId, typeAssuranceId, dateDebut, dateFin, prime, statut, garantieIds (plusieurs valeurs)
     Attributs : contrat         Contrat (null en création)
                 clients         List<Client>
                 typesAssurance  List<TypeAssurance>{id, nom, prixBase, garanties List<Garantie>}
                 garanties       List<Garantie> (toutes)
                 statuts         StatutContrat[]
                 erreurs         Map<String,String>
     Le numéro de contrat est généré par le serveur. --%>
<c:set var="creation" value="${empty contrat}" />
<c:set var="titre" value="${creation ? 'Nouveau contrat' : 'Modifier le contrat'}" />
<%-- Valeurs affichées : paramètres renvoyés après une erreur, sinon valeurs du contrat --%>
<c:set var="vClient" value="${not empty param.clientId ? param.clientId : contrat.client.id}" />
<c:set var="vType" value="${not empty param.typeAssuranceId ? param.typeAssuranceId : contrat.typeAssurance.id}" />
<c:set var="vStatut" value="${not empty param.statut ? param.statut : (creation ? 'EN_ATTENTE' : contrat.statut)}" />
<!DOCTYPE html>
<html lang="fr">
<head><%@ include file="/WEB-INF/views/common/head.jspf" %></head>
<body>
<div class="app">
  <jsp:include page="/WEB-INF/views/common/sidebar.jsp"><jsp:param name="actif" value="contrats" /></jsp:include>
  <main class="main">
    <div class="container">
      <header class="stack-sm">
        <a class="back" href="${ctx}/agent/contrats">← Contrats</a>
        <h1>${titre}</h1>
        <c:if test="${not creation}"><span class="muted"><c:out value="${contrat.numero}" /></span></c:if>
      </header>
      <%@ include file="/WEB-INF/views/common/flash.jspf" %>

      <form class="card form" method="post" action="${ctx}/agent/contrats/edit" novalidate data-validate>
        <%@ include file="/WEB-INF/views/common/csrf.jspf" %>
        <input type="hidden" name="id" value="${contrat.id}">

        <div class="grid-2">
          <label class="field ${not empty erreurs.clientId ? 'has-error' : ''}">Client *
            <select name="clientId" required>
              <option value="">— Choisir un client —</option>
              <c:forEach items="${clients}" var="cl">
                <option value="${cl.id}" ${fn:trim(vClient) == fn:trim(cl.id) ? 'selected' : ''}><c:out value="${cl.nom} ${cl.prenom}" /> · <c:out value="${cl.email}" /></option>
              </c:forEach>
            </select>
            <c:if test="${not empty erreurs.clientId}"><span class="field-error"><c:out value="${erreurs.clientId}" /></span></c:if>
          </label>
          <label class="field ${not empty erreurs.typeAssuranceId ? 'has-error' : ''}">Type d'assurance *
            <select name="typeAssuranceId" required data-type-assurance>
              <option value="">— Choisir un type —</option>
              <c:forEach items="${typesAssurance}" var="ta">
                <c:set var="gIds"><c:forEach items="${ta.garanties}" var="g" varStatus="gs">${g.id}${gs.last ? '' : ','}</c:forEach></c:set>
                <option value="${ta.id}" data-prix="${ta.prixBase}" data-garanties="${gIds}" ${fn:trim(vType) == fn:trim(ta.id) ? 'selected' : ''}><c:out value="${ta.nom}" /></option>
              </c:forEach>
            </select>
            <c:if test="${not empty erreurs.typeAssuranceId}"><span class="field-error"><c:out value="${erreurs.typeAssuranceId}" /></span></c:if>
          </label>
        </div>

        <div class="grid-3">
          <ui:champ nom="dateDebut" libelle="Date de début *" type="date" requis="true" valeur="${not empty param.dateDebut ? param.dateDebut : contrat.dateDebut}" />
          <ui:champ nom="dateFin" libelle="Date de fin *" type="date" requis="true" valeur="${not empty param.dateFin ? param.dateFin : contrat.dateFin}" extra="data-after='dateDebut'" />
          <ui:champ nom="prime" libelle="Prime mensuelle (€) *" type="number" requis="true" valeur="${not empty param.prime ? param.prime : contrat.prime}"
                    extra="min='0.01' step='0.01' data-prime" aide="Pré-remplie avec le prix de base du type d'assurance." />
        </div>

        <label class="field ${not empty erreurs.statut ? 'has-error' : ''}">Statut
          <select name="statut">
            <c:forEach items="${statuts}" var="st"><option value="${st}" ${fn:trim(vStatut) == st.name() ? 'selected' : ''}>${st}</option></c:forEach>
          </select>
          <c:if test="${not empty erreurs.statut}"><span class="field-error"><c:out value="${erreurs.statut}" /></span></c:if>
        </label>

        <fieldset>
          <legend>Garanties du contrat *</legend>
          <span class="field-help">Les garanties disponibles pour le type choisi sont mises en avant.</span>
          <div class="grid-3">
            <c:forEach items="${garanties}" var="g">
              <c:set var="coche" value="false" />
              <c:forEach items="${paramValues.garantieIds}" var="pid"><c:if test="${pid == fn:trim(g.id)}"><c:set var="coche" value="true" /></c:if></c:forEach>
              <c:if test="${empty paramValues.garantieIds}">
                <c:forEach items="${contrat.garanties}" var="cg"><c:if test="${cg.id == g.id}"><c:set var="coche" value="true" /></c:if></c:forEach>
              </c:if>
              <label class="choice" data-garantie="${g.id}">
                <input type="checkbox" name="garantieIds" value="${g.id}" ${coche ? 'checked' : ''}>
                <span class="stack-sm"><span class="strong"><c:out value="${g.nom}" /></span><span class="xsmall muted"><c:out value="${g.description}" /></span></span>
              </label>
            </c:forEach>
          </div>
          <c:if test="${not empty erreurs.garanties}"><span class="field-error"><c:out value="${erreurs.garanties}" /></span></c:if>
        </fieldset>

        <div class="form-actions">
          <a class="btn" href="${ctx}/agent/contrats">Annuler</a>
          <button type="submit" class="btn btn-primary">${creation ? 'Créer le contrat' : 'Enregistrer les modifications'}</button>
        </div>
      </form>
    </div>
  </main>
</div>
<script src="${ctx}/js/app.js"></script>
</body>
</html>
