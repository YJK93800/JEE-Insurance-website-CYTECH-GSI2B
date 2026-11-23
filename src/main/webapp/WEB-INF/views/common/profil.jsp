<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/taglibs.jspf" %>
<%-- Profil commun aux trois rôles (client, agent, admin) — ProfilServlet, URL /profil
     GET  /profil -> cette vue (attribut "user" : entité User connectée)
     POST /profil  (action=infos      : telephone, adresse)
                   (action=motdepasse : ancien, motDePasse, confirmation)
     Erreurs : Map<String,String> "erreurs" (clés = noms des champs).
     Identité (prénom, nom, e-mail, date de naissance) : lecture seule, corrigée par un agent / admin. --%>
<c:set var="titre" value="Mon profil" />
<c:set var="role" value="${sessionScope.utilisateur.role}" />
<!DOCTYPE html>
<html lang="fr">
<head><%@ include file="/WEB-INF/views/common/head.jspf" %></head>
<body>
<div class="app">
  <jsp:include page="/WEB-INF/views/common/sidebar.jsp"><jsp:param name="actif" value="profil" /></jsp:include>
  <main class="main">
    <div class="container">
      <header class="page-head"><div class="stack-sm">
        <span class="eyebrow">
          <c:choose>
            <c:when test="${role == 'ADMIN'}">Administration</c:when>
            <c:when test="${role == 'AGENT'}">Espace agent</c:when>
            <c:otherwise>Espace client</c:otherwise>
          </c:choose>
        </span>
        <h1>Mon profil</h1>
      </div></header>
      <%@ include file="/WEB-INF/views/common/flash.jspf" %>

      <div class="row">
        <form class="card form col-main" method="post" action="${ctx}/profil" novalidate data-validate>
          <%@ include file="/WEB-INF/views/common/csrf.jspf" %>
          <input type="hidden" name="action" value="infos">
          <h2>Informations personnelles</h2>
          <div class="grid-2">
            <label class="field">Prénom
              <input type="text" value="${fn:escapeXml(user.firstName)}" disabled>
            </label>
            <label class="field">Nom
              <input type="text" value="${fn:escapeXml(user.lastName)}" disabled>
            </label>
          </div>
          <label class="field">Adresse e-mail
            <input type="email" value="${fn:escapeXml(user.email)}" disabled>
            <span class="field-help">
              Pour corriger votre nom, prénom ou e-mail, contactez ${role == 'CLIENT' ? 'votre agent' : 'un administrateur'}.
            </span>
          </label>
          <div class="grid-2">
            <ui:champ nom="telephone" libelle="Téléphone" type="tel" valeur="${not empty param.telephone ? param.telephone : user.phone}" extra="pattern='[+0-9][0-9 .]{5,19}' maxlength='20'" />
            <label class="field">Date de naissance
              <input type="text" value="<ui:date valeur='${user.birthDate}' />" disabled>
            </label>
          </div>
          <ui:champ nom="adresse" libelle="Adresse postale" valeur="${not empty param.adresse ? param.adresse : user.address}" extra="maxlength='255'" />
          <div class="form-actions"><span></span><button type="submit" class="btn btn-primary">Enregistrer</button></div>
        </form>

        <form class="card form col-side" method="post" action="${ctx}/profil" novalidate data-validate>
          <%@ include file="/WEB-INF/views/common/csrf.jspf" %>
          <input type="hidden" name="action" value="motdepasse">
          <h2>Mot de passe</h2>
          <ui:champ nom="ancien" libelle="Mot de passe actuel" type="password" requis="true" extra="autocomplete='current-password'" />
          <ui:champ nom="motDePasse" libelle="Nouveau mot de passe" type="password" requis="true" aide="8 caractères minimum, dont un chiffre." extra="minlength='8' autocomplete='new-password'" />
          <ui:champ nom="confirmation" libelle="Confirmation" type="password" requis="true" extra="minlength='8' data-match='motDePasse' autocomplete='new-password'" />
          <button type="submit" class="btn btn-block">Changer le mot de passe</button>
        </form>
      </div>
    </div>
  </main>
</div>
<script src="${ctx}/js/app.js"></script>
</body>
</html>
