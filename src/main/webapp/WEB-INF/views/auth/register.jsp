<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/taglibs.jspf" %>
<%-- GET /register -> cette vue. POST /register -> RegisterServlet.
     Paramètres : prenom, nom, email, telephone, adresse, dateNaissance (aaaa-mm-jj), motDePasse, confirmation.
     Erreurs : Map<String,String> "errors" (clé = nom du champ). Les valeurs saisies sont réaffichées automatiquement. --%>
<c:set var="title" value="Créer un compte" />
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
        <%@ include file="/WEB-INF/views/common/csrf.jspf" %>
        <div class="grid-2">
          <ui:field name="firstName" label="Prénom" required="true" extra="maxlength='80' autocomplete='given-name'" />
          <ui:field name="lastName" label="Nom" required="true" extra="maxlength='80' autocomplete='family-name'" />
        </div>
        <ui:field name="email" label="Adresse e-mail" type="email" required="true" extra="maxlength='150' autocomplete='email'" />
        <div class="grid-2">
          <ui:field name="phone" label="Téléphone" type="tel" extra="pattern='[+0-9][0-9 .]{5,19}' maxlength='20' autocomplete='tel'" />
          <ui:field name="birthDate" label="Date de naissance" type="date" required="true" />
        </div>
        <ui:field name="address" label="Adresse postale" extra="maxlength='255' autocomplete='street-address'" />
        <ui:field name="password" label="Mot de passe" type="password" required="true"
                  help="8 caractères minimum, dont un chiffre." extra="minlength='8' autocomplete='new-password'" />
        <ui:field name="confirmation" label="Confirmer le mot de passe" type="password" required="true"
                  extra="minlength='8' data-match='password' autocomplete='new-password'" />
        <button type="submit" class="btn btn-primary btn-block">Créer mon compte</button>
      </form>
    </div>
  </main>
</div>
<script src="${ctx}/js/app.js"></script>
</body>
</html>
