<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/taglibs.jspf" %>
<%-- AdminUsersServlet — GET  /admin/users[?roleFilter=AGENT&q=texte]
                         POST /admin/users  id, action=active, value=true|false  -> UserService.setActive
                         POST /admin/users  id, action=role, role=CLIENT|AGENT|ADMIN -> UserService.changeRole
     Lien « Corriger le nom » -> AdminUserEditServlet (GET /admin/users/edit?id=…)
     Attributs : users   List<User>{id, firstName, lastName, email, role{name}, active, createdAt}
                 errors Map<String,String> en cas d'échec (clés : role, user, action)
     Le filtre par rôle et la recherche sont faits ici, sur la liste complète renvoyée par le service.
     On ne peut pas modifier son propre compte (le service le refuse) : les actions sont masquées sur sa ligne. --%>
<c:set var="title" value="Utilisateurs et rôles" />
<c:set var="q" value="${fn:toLowerCase(fn:trim(param.q))}" />
<c:set var="roles" value="CLIENT,AGENT,ADMIN" />
<!DOCTYPE html>
<html lang="fr">
<head><%@ include file="/WEB-INF/views/common/head.jspf" %></head>
<body>
<div class="app">
  <jsp:include page="/WEB-INF/views/common/sidebar.jsp"><jsp:param name="active" value="users" /></jsp:include>
  <main class="main">
    <div class="container">
      <header class="page-head">
        <div class="stack-sm"><span class="eyebrow">Administration</span><h1>Utilisateurs &amp; rôles</h1></div>
        <form class="search" method="get" action="${ctx}/admin/users" role="search">
          <label class="field">Rôle
            <select name="roleFilter">
              <option value="">Tous</option>
              <c:forEach items="${roles}" var="r"><option value="${r}" ${param.roleFilter == r ? 'selected' : ''}>${r}</option></c:forEach>
            </select>
          </label>
          <label class="field">Rechercher<input type="search" name="q" value="${fn:escapeXml(param.q)}" placeholder="Nom ou e-mail"></label>
          <button class="btn" type="submit">Filtrer</button>
        </form>
      </header>
      <%@ include file="/WEB-INF/views/common/flash.jspf" %>
      <c:if test="${not empty errors}">
        <div class="alert alert-error" role="alert"><span><c:forEach items="${errors}" var="e"><c:out value="${e.value}" /> </c:forEach></span></div>
      </c:if>

      <section class="card flush" aria-label="Liste des utilisateurs">
        <c:set var="shown" value="0" />
        <div class="table-wrap">
          <table class="table">
            <thead><tr><th>Nom</th><th>E-mail</th><th>Inscrit le</th><th>Rôle</th><th>Compte</th><th class="right">Actions</th></tr></thead>
            <tbody>
              <c:forEach items="${users}" var="usr">
                <c:set var="text" value="${fn:toLowerCase(usr.firstName)} ${fn:toLowerCase(usr.lastName)} ${fn:toLowerCase(usr.email)}" />
                <c:if test="${(empty param.roleFilter or usr.role.name == param.roleFilter) and (empty q or fn:contains(text, q))}">
                  <c:set var="shown" value="${shown + 1}" />
                  <c:set var="isMe" value="${usr.id == sessionScope.currentUser.id}" />
                  <tr>
                    <td class="strong"><c:out value="${usr.firstName} ${usr.lastName}" />${isMe ? ' (vous)' : ''}</td>
                    <td class="muted"><c:out value="${usr.email}" /></td>
                    <td class="muted small"><ui:date value="${usr.createdAt}" /></td>
                    <td>
                      <c:choose>
                        <c:when test="${isMe}"><span class="badge">${usr.role.name}</span></c:when>
                        <c:otherwise>
                          <form class="inline" method="post" action="${ctx}/admin/users">
                            <%@ include file="/WEB-INF/views/common/csrf.jspf" %>
                            <input type="hidden" name="id" value="${usr.id}"><input type="hidden" name="action" value="role">
                            <select name="role" aria-label="Rôle de ${fn:escapeXml(usr.firstName)} ${fn:escapeXml(usr.lastName)}">
                              <c:forEach items="${roles}" var="r"><option value="${r}" ${usr.role.name == r ? 'selected' : ''}>${r}</option></c:forEach>
                            </select>
                            <button class="btn btn-sm" type="submit">Changer</button>
                          </form>
                        </c:otherwise>
                      </c:choose>
                    </td>
                    <td><span class="badge ${usr.active ? 'badge-actif' : 'badge-suspendu'}">${usr.active ? 'ACTIF' : 'DÉSACTIVÉ'}</span></td>
                    <td><div class="actions">
                      <c:if test="${not isMe}">
                        <a class="btn-link" href="${ctx}/admin/users/edit?id=${usr.id}">Corriger le nom</a>
                        <form method="post" action="${ctx}/admin/users"
                              <c:if test="${usr.active}">data-confirm="Désactiver le compte de ${fn:escapeXml(usr.email)} ?"</c:if>>
                          <%@ include file="/WEB-INF/views/common/csrf.jspf" %>
                          <input type="hidden" name="id" value="${usr.id}"><input type="hidden" name="action" value="active">
                          <input type="hidden" name="value" value="${not usr.active}">
                          <button class="btn-link ${usr.active ? 'danger' : ''}" type="submit">${usr.active ? 'Désactiver' : 'Activer'}</button>
                        </form>
                      </c:if>
                    </div></td>
                  </tr>
                </c:if>
              </c:forEach>
              <c:if test="${shown == 0}"><tr><td colspan="6" class="empty">Aucun utilisateur.</td></tr></c:if>
            </tbody>
          </table>
        </div>
      </section>
    </div>
  </main>
</div>
<script src="${ctx}/js/app.js"></script>
</body>
</html>
