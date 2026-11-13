<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/taglibs.jspf" %>
<%-- GET  /profil -> cette vue (attribut "user" : entité User de l'utilisateur connecté, tous rôles)
     POST /profil (action=infos : firstName, name, phoneNumber, address)
                  (action=password : oldPassword, password, confirmation)
     Erreurs : Map<String,String> "errors". L'e-mail et la date de naissance ne sont pas modifiables ici. --%>
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
        <span class="eyebrow">${role == 'ADMIN' ? 'Administration' : role == 'AGENT' ? 'Espace agent' : 'Espace client'}</span>
        <h1>Mon profil</h1>
      </div></header>
      <%@ include file="/WEB-INF/views/common/flash.jspf" %>

      <div class="row">
        <form class="card form col-main" method="post" action="${ctx}/profil" novalidate data-validate>
          <%@ include file="/WEB-INF/views/common/csrf.jspf" %>
          <input type="hidden" name="action" value="infos">
          <h2>Informations personnelles</h2>
          <div class="grid-2">
            <ui:champ nom="firstName" libelle="Prénom" requis="true" valeur="${not empty param.firstName ? param.firstName : user.firstName}" extra="maxlength='60'" />
            <ui:champ nom="name" libelle="Nom" requis="true" valeur="${not empty param.name ? param.name : user.name}" extra="maxlength='60'" />
          </div>
          <label class="field">Adresse e-mail
            <input type="email" value="${fn:escapeXml(user.email)}" disabled>
            <span class="field-help">Pour changer d'adresse e-mail, contactez un administrateur.</span>
          </label>
          <div class="grid-2">
            <ui:champ nom="phoneNumber" libelle="Téléphone" type="tel" valeur="${not empty param.phoneNumber ? param.phoneNumber : user.phoneNumber}" extra="pattern='[0-9 +]{10,15}'" />
            <label class="field">Date de naissance
              <input type="text" value="<ui:date valeur='${user.birthDate}' />" disabled>
            </label>
          </div>
          <ui:champ nom="address" libelle="Adresse postale" valeur="${not empty param.address ? param.address : user.address}" extra="maxlength='200'" />
          <div class="form-actions"><span></span><button type="submit" class="btn btn-primary">Enregistrer</button></div>
        </form>

        <form class="card form col-side" method="post" action="${ctx}/profil" novalidate data-validate>
          <%@ include file="/WEB-INF/views/common/csrf.jspf" %>
          <input type="hidden" name="action" value="password">
          <h2>Mot de passe</h2>
          <ui:champ nom="oldPassword" libelle="Mot de passe actuel" type="password" requis="true" extra="autocomplete='current-password'" />
          <ui:champ nom="password" libelle="Nouveau mot de passe" type="password" requis="true" aide="8 caractères minimum, dont un chiffre." extra="minlength='8' autocomplete='new-password'" />
          <ui:champ nom="confirmation" libelle="Confirmation" type="password" requis="true" extra="minlength='8' data-match='password' autocomplete='new-password'" />
          <button type="submit" class="btn btn-block">Changer le mot de passe</button>
        </form>
      </div>
    </div>
  </main>
</div>
<script src="${ctx}/js/app.js"></script>
</body>
</html>