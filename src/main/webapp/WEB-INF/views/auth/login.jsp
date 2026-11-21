<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/taglibs.jspf" %>
<%-- GET /login -> cette vue. POST /login (email, motDePasse) -> LoginServlet.
     En cas d'échec : request.setAttribute("erreurGlobale", "Email ou mot de passe incorrect.") puis forward ici. --%>
<c:set var="titre" value="Connexion" />
<!DOCTYPE html>
<html lang="fr">
<head><%@ include file="/WEB-INF/views/common/head.jspf" %></head>
<body>
<div class="auth">
  <aside class="auth-aside">
    <div class="brand"><%@ include file="/WEB-INF/views/common/logo.jspf" %><span class="brand-name">Égide</span></div>
    <div class="stack">
      <h1>Vos contrats, vos sinistres, au même endroit.</h1>
      <p>Consultez vos garanties, déclarez un sinistre en quelques minutes et suivez son traitement en temps réel.</p>
    </div>
    <span class="small muted">Espace client, agent et administrateur</span>
  </aside>

  <main class="auth-main">
    <div class="auth-card">
      <div class="stack-sm">
        <h1>Connexion</h1>
        <p class="muted">Pas encore de compte ? <a href="${ctx}/register">Créer un compte client</a></p>
      </div>

      <%@ include file="/WEB-INF/views/common/flash.jspf" %>

      <form class="form" method="post" action="${ctx}/login" novalidate data-validate>
        <ui:champ nom="email" libelle="Adresse e-mail" type="email" requis="true" extra="autocomplete='username'" />
        <ui:champ nom="motDePasse" libelle="Mot de passe" type="password" requis="true" extra="autocomplete='current-password'" />
        <button type="submit" class="btn btn-primary btn-block">Se connecter</button>
      </form>
    </div>
  </main>
</div>
<script src="${ctx}/js/app.js"></script>
</body>
</html>
