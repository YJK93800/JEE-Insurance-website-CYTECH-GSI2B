<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/taglibs.jspf" %>
<%-- Profil commun aux trois rôles (client, agent, admin) — ProfileServlet, URL /profile
     GET  /profile -> cette vue (attribut "user" : entité User connectée)
     POST /profile (action=details  : email, birthDate (aaaa-mm-jj), phone, address)
                   (action=password : oldPassword, password, confirmation)
     Erreurs : Map<String,String> "errors" (clés = noms des champs ci-dessus).
     Après une erreur, les valeurs ressaisies sont réaffichées via param.xxx, sinon celles de "user".
     Prénom et nom : lecture seule, corrigés par un agent / admin. --%>
<c:set var="title" value="Mon profil" />
<c:set var="role" value="${sessionScope.currentUser.role}" />
<!DOCTYPE html>
<html lang="fr">
<head><%@ include file="/WEB-INF/views/common/head.jspf" %></head>
<body>
<div class="app">
  <jsp:include page="/WEB-INF/views/common/sidebar.jsp"><jsp:param name="active" value="profile" /></jsp:include>
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
        <form class="card form col-main" method="post" action="${ctx}/profile" novalidate data-validate>
          <%@ include file="/WEB-INF/views/common/csrf.jspf" %>
          <input type="hidden" name="action" value="details">
          <h2>Informations personnelles</h2>
          <%-- Champs disabled sans name : non envoyés, la servlet ne les lit pas --%>
          <div class="grid-2">
            <label class="field">Prénom
              <input type="text" value="${fn:escapeXml(user.firstName)}" disabled>
            </label>
            <label class="field">Nom
              <input type="text" value="${fn:escapeXml(user.lastName)}" disabled>
            </label>
          </div>
          <p class="field-help">
            Pour corriger votre nom ou prénom, contactez ${role == 'CLIENT' ? 'votre agent' : 'un administrateur'}.
          </p>
          <ui:field name="email" label="Adresse e-mail" type="email" required="true" value="${not empty param.email ? param.email : user.email}" extra="maxlength='150' autocomplete='email'" />
          <div class="grid-2">
            <ui:field name="phone" label="Téléphone" type="tel" value="${not empty param.phone ? param.phone : user.phone}" extra="pattern='[+0-9][0-9 .]{5,19}' maxlength='20' autocomplete='tel'" />
            <%-- type="date" : le navigateur envoie et attend aaaa-mm-jj (= LocalDate.toString()) --%>
            <ui:field name="birthDate" label="Date de naissance" type="date" required="true" value="${not empty param.birthDate ? param.birthDate : user.birthDate}" />
          </div>
          <ui:field name="address" label="Adresse postale" value="${not empty param.address ? param.address : user.address}" extra="maxlength='255' autocomplete='street-address'" />
          <div class="form-actions"><span></span><button type="submit" class="btn btn-primary">Enregistrer</button></div>
        </form>

        <form class="card form col-side" method="post" action="${ctx}/profile" novalidate data-validate>
          <%@ include file="/WEB-INF/views/common/csrf.jspf" %>
          <input type="hidden" name="action" value="password">
          <h2>Mot de passe</h2>
          <ui:field name="oldPassword" label="Mot de passe actuel" type="password" required="true" extra="autocomplete='current-password'" />
          <ui:field name="password" label="Nouveau mot de passe" type="password" required="true" help="8 caractères minimum, dont un chiffre." extra="minlength='8' autocomplete='new-password'" />
          <ui:field name="confirmation" label="Confirmation" type="password" required="true" extra="minlength='8' data-match='password' autocomplete='new-password'" />
          <button type="submit" class="btn btn-block">Changer le mot de passe</button>
        </form>
      </div>
    </div>
  </main>
</div>
<script src="${ctx}/js/app.js"></script>
</body>
</html>
