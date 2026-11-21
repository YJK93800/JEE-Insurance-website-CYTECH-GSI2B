<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/taglibs.jspf" %>
<%-- GET  /admin/utilisateurs?filtreRole=AGENT&q=texte[&id=7]
     POST /admin/utilisateurs          (id vide = création ; prenom, nom, email, role, motDePasse (obligatoire en création), actif)
     POST /admin/utilisateurs/statut   (id, actif=true|false) -> activer / désactiver un compte
     Attributs : utilisateurs List<Utilisateur>{id, prenom, nom, email, role, actif, dateInscription}
                 utilisateur Utilisateur en cours de modification (null en création)
                 roles Role[]   erreurs Map<String,String> --%>
<c:set var="titre" value="Utilisateurs et rôles" />
<c:set var="u0" value="${utilisateur}" />
<c:set var="vRole" value="${not empty param.role ? param.role : u0.role}" />
<!DOCTYPE html>
<html lang="fr">
<head><%@ include file="/WEB-INF/views/common/head.jspf" %></head>
<body>
<div class="app">
  <jsp:include page="/WEB-INF/views/common/sidebar.jsp"><jsp:param name="actif" value="utilisateurs" /></jsp:include>
  <main class="main">
    <div class="container">
      <header class="page-head">
        <div class="stack-sm"><span class="eyebrow">Administration</span><h1>Utilisateurs &amp; rôles</h1></div>
        <form class="search" method="get" action="${ctx}/admin/utilisateurs" role="search">
          <label class="field">Rôle
            <select name="filtreRole">
              <option value="">Tous</option>
              <c:forEach items="${roles}" var="r"><option value="${r}" ${param.filtreRole == r.name() ? 'selected' : ''}>${r}</option></c:forEach>
            </select>
          </label>
          <label class="field">Rechercher<input type="search" name="q" value="${fn:escapeXml(param.q)}" placeholder="Nom ou e-mail"></label>
          <button class="btn" type="submit">Filtrer</button>
        </form>
      </header>
      <%@ include file="/WEB-INF/views/common/flash.jspf" %>

      <div class="row">
        <section class="card flush col-main" aria-label="Liste des utilisateurs">
          <div class="table-wrap">
            <table class="table">
              <thead><tr><th>Nom</th><th>E-mail</th><th>Rôle</th><th>Compte</th><th class="right">Actions</th></tr></thead>
              <tbody>
                <c:forEach items="${utilisateurs}" var="us">
                  <tr class="${u0.id == us.id ? 'selected' : ''}">
                    <td class="strong"><c:out value="${us.prenom} ${us.nom}" /></td>
                    <td class="muted"><c:out value="${us.email}" /></td>
                    <td><span class="badge">${us.role}</span></td>
                    <td><span class="badge ${us.actif ? 'badge-actif' : 'badge-suspendu'}">${us.actif ? 'ACTIF' : 'DÉSACTIVÉ'}</span></td>
                    <td><div class="actions">
                      <a class="btn btn-sm" href="${ctx}/admin/utilisateurs?id=${us.id}">Modifier</a>
                      <c:if test="${us.id != sessionScope.utilisateur.id}">
                        <form method="post" action="${ctx}/admin/utilisateurs/statut">
                          <%@ include file="/WEB-INF/views/common/csrf.jspf" %>
                          <input type="hidden" name="id" value="${us.id}"><input type="hidden" name="actif" value="${not us.actif}">
                          <button class="btn-link ${us.actif ? 'danger' : ''}" type="submit">${us.actif ? 'Désactiver' : 'Activer'}</button>
                        </form>
                      </c:if>
                    </div></td>
                  </tr>
                </c:forEach>
                <c:if test="${empty utilisateurs}"><tr><td colspan="5" class="empty">Aucun utilisateur.</td></tr></c:if>
              </tbody>
            </table>
          </div>
        </section>

        <form class="card form col-side" method="post" action="${ctx}/admin/utilisateurs" novalidate data-validate>
          <%@ include file="/WEB-INF/views/common/csrf.jspf" %>
          <input type="hidden" name="id" value="${u0.id}">
          <h2>${empty u0 ? 'Nouvel utilisateur' : 'Modifier l\'utilisateur'}</h2>
          <div class="grid-2">
            <ui:champ nom="prenom" libelle="Prénom *" requis="true" valeur="${not empty param.prenom ? param.prenom : u0.prenom}" />
            <ui:champ nom="nom" libelle="Nom *" requis="true" valeur="${not empty param.nom ? param.nom : u0.nom}" />
          </div>
          <ui:champ nom="email" libelle="E-mail *" type="email" requis="true" valeur="${not empty param.email ? param.email : u0.email}" />
          <label class="field ${not empty erreurs.role ? 'has-error' : ''}">Rôle *
            <select name="role" required>
              <c:forEach items="${roles}" var="r"><option value="${r}" ${fn:trim(vRole) == r.name() ? 'selected' : ''}>${r}</option></c:forEach>
            </select>
            <c:if test="${not empty erreurs.role}"><span class="field-error"><c:out value="${erreurs.role}" /></span></c:if>
          </label>
          <ui:champ nom="motDePasse" libelle="${empty u0 ? 'Mot de passe *' : 'Nouveau mot de passe'}" type="password"
                    requis="${empty u0}" aide="${empty u0 ? '8 caractères minimum.' : 'Laisser vide pour ne pas changer.'}" extra="minlength='8' autocomplete='new-password'" />
          <div class="inline">
            <button type="submit" class="btn btn-primary">${empty u0 ? 'Créer' : 'Enregistrer'}</button>
            <c:if test="${not empty u0}"><a class="btn" href="${ctx}/admin/utilisateurs">Annuler</a></c:if>
          </div>
        </form>
      </div>
    </div>
  </main>
</div>
<script src="${ctx}/js/app.js"></script>
</body>
</html>
