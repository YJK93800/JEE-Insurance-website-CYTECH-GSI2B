<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/taglibs.jspf" %>
<%-- AdminUserEditServlet
     GET  /admin/users/edit?id=5                     -> cette page
     POST /admin/users/edit (id, firstName, lastName) -> UserService.updateName, retour à /admin/users
     Attributs : target  User dont on corrige le nom
                 errors  Map<String,String> (clés : firstName, lastName, user) --%>
<c:set var="title" value="Corriger le nom" />
<!DOCTYPE html>
<html lang="fr">
<head><%@ include file="/WEB-INF/views/common/head.jspf" %></head>
<body>
<div class="app">
  <jsp:include page="/WEB-INF/views/common/sidebar.jsp"><jsp:param name="active" value="users" /></jsp:include>
  <main class="main">
    <div class="container">
      <header class="stack-sm">
        <a class="back" href="${ctx}/admin/users">← Utilisateurs &amp; rôles</a>
        <h1>${title}</h1>
        <span class="muted"><c:out value="${target.email}" /> · <span class="badge">${target.role.name}</span></span>
      </header>
      <%@ include file="/WEB-INF/views/common/flash.jspf" %>

      <form class="card form" method="post" action="${ctx}/admin/users/edit" novalidate data-validate>
        <%@ include file="/WEB-INF/views/common/csrf.jspf" %>
        <input type="hidden" name="id" value="${target.id}">

        <c:if test="${not empty errors.user}"><p class="field-error"><c:out value="${errors.user}" /></p></c:if>

        <div class="grid-2">
          <ui:field name="firstName" label="Prénom *" required="true" extra="maxlength='80'"
                    value="${not empty param.firstName ? param.firstName : target.firstName}" />
          <ui:field name="lastName" label="Nom *" required="true" extra="maxlength='80'"
                    value="${not empty param.lastName ? param.lastName : target.lastName}" />
        </div>

        <div class="form-actions">
          <a class="btn" href="${ctx}/admin/users">Annuler</a>
          <button type="submit" class="btn btn-primary">Enregistrer</button>
        </div>
      </form>
    </div>
  </main>
</div>
<script src="${ctx}/js/app.js"></script>
</body>
</html>
