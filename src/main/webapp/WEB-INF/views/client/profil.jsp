<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/taglibs.jspf" %>
<%-- GET  /client/profil -> cette vue (attribut "client" : Client connecté)
     POST /client/profil  (action=infos : prenom, nom, telephone, adresse)
                          (action=motdepasse : ancien, motDePasse, confirmation)
     Erreurs : Map<String,String> "erreurs". L'e-mail n'est pas modifiable ici. --%>
<c:set var="titre" value="Mon profil" />
<!DOCTYPE html>
<html lang="fr">
<head><%@ include file="/WEB-INF/views/common/head.jspf" %></head>
<body>
<div class="app">
  <jsp:include page="/WEB-INF/views/common/sidebar.jsp"><jsp:param name="actif" value="profil" /></jsp:include>
  <main class="main">
    <div class="container">
      <header class="page-head"><div class="stack-sm"><span class="eyebrow">Espace client</span><h1>Mon profil</h1></div></header>
      <%@ include file="/WEB-INF/views/common/flash.jspf" %>

      <div class="row">
        <form class="card form col-main" method="post" action="${ctx}/client/profil" novalidate data-validate>
          <%@ include file="/WEB-INF/views/common/csrf.jspf" %>
          <input type="hidden" name="action" value="infos">
          <h2>Informations personnelles</h2>
          <div class="grid-2">
            <ui:champ nom="prenom" libelle="Prénom" requis="true" valeur="${not empty param.prenom ? param.prenom : client.prenom}" extra="maxlength='60'" />
            <ui:champ nom="nom" libelle="Nom" requis="true" valeur="${not empty param.nom ? param.nom : client.nom}" extra="maxlength='60'" />
          </div>
          <label class="field">Adresse e-mail
            <input type="email" value="${fn:escapeXml(client.email)}" disabled>
            <span class="field-help">Pour changer d'adresse e-mail, contactez votre agent.</span>
          </label>
          <div class="grid-2">
            <ui:champ nom="telephone" libelle="Téléphone" type="tel" valeur="${not empty param.telephone ? param.telephone : client.telephone}" extra="pattern='[0-9 +]{10,15}'" />
            <label class="field">Date de naissance
              <input type="text" value="<ui:date valeur='${client.dateNaissance}' />" disabled>
            </label>
          </div>
          <ui:champ nom="adresse" libelle="Adresse postale" valeur="${not empty param.adresse ? param.adresse : client.adresse}" extra="maxlength='200'" />
          <div class="form-actions"><span></span><button type="submit" class="btn btn-primary">Enregistrer</button></div>
        </form>

        <form class="card form col-side" method="post" action="${ctx}/client/profil" novalidate data-validate>
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
