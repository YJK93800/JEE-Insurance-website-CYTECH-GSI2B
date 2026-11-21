<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/taglibs.jspf" %>
<%-- GET /register -> cette vue. POST /register -> RegisterServlet.
     Paramètres : prenom, nom, email, telephone, adresse, dateNaissance (aaaa-mm-jj), motDePasse, confirmation.
     Erreurs : Map<String,String> "erreurs" (clé = nom du champ). Les valeurs saisies sont réaffichées automatiquement. --%>
<c:set var="titre" value="Créer un compte" />
<!DOCTYPE html>
<html lang="fr">
<head><%@ include file="/WEB-INF/views/common/head.jspf" %></head>
<body>
<div class="auth">
  <aside class="auth-aside">
    <div class="brand"><%@ include file="/WEB-INF/views/common/logo.jspf" %><span class="brand-name">Égide</span></div>
    <div class="stack">
      <h1>Créez votre espace client.</h1>
      <p>Une fois votre compte créé, votre agent pourra y rattacher vos contrats d'assurance.</p>
    </div>
    <span class="small muted">Vos données ne sont utilisées que pour la gestion de vos contrats.</span>
  </aside>

  <main class="auth-main">
    <div class="auth-card">
      <div class="stack-sm">
        <h1>Créer un compte</h1>
        <p class="muted">Déjà inscrit ? <a href="${ctx}/login">Se connecter</a></p>
      </div>

      <%@ include file="/WEB-INF/views/common/flash.jspf" %>

      <form class="form" method="post" action="${ctx}/register" novalidate data-validate>
        <div class="grid-2">
          <ui:champ nom="prenom" libelle="Prénom" requis="true" extra="maxlength='60' autocomplete='given-name'" />
          <ui:champ nom="nom" libelle="Nom" requis="true" extra="maxlength='60' autocomplete='family-name'" />
        </div>
        <ui:champ nom="email" libelle="Adresse e-mail" type="email" requis="true" extra="autocomplete='email'" />
        <div class="grid-2">
          <ui:champ nom="telephone" libelle="Téléphone" type="tel" extra="pattern='[0-9 +]{10,15}' autocomplete='tel'" />
          <ui:champ nom="dateNaissance" libelle="Date de naissance" type="date" requis="true" />
        </div>
        <ui:champ nom="adresse" libelle="Adresse postale" extra="maxlength='200' autocomplete='street-address'" />
        <ui:champ nom="motDePasse" libelle="Mot de passe" type="password" requis="true"
                  aide="8 caractères minimum, dont un chiffre." extra="minlength='8' autocomplete='new-password'" />
        <ui:champ nom="confirmation" libelle="Confirmer le mot de passe" type="password" requis="true"
                  extra="minlength='8' data-match='motDePasse' autocomplete='new-password'" />
        <button type="submit" class="btn btn-primary btn-block">Créer mon compte</button>
      </form>
    </div>
  </main>
</div>
<script src="${ctx}/js/app.js"></script>
</body>
</html>
