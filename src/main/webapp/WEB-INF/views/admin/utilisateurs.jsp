<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/taglibs.jspf" %>
<%-- AdminUsersServlet — GET  /admin/users[?filtreRole=AGENT&q=texte]
                         POST /admin/users  id, action=active, value=true|false  -> UserService.setActive
                         POST /admin/users  id, action=role, role=CLIENT|AGENT|ADMIN -> UserService.changeRole
     Attributs : users   List<User>{id, firstName, lastName, email, role{name}, active, createdAt}
                 erreurs Map<String,String> en cas d'échec (clés : role, user, action)
     Le filtre par rôle et la recherche sont faits ici, sur la liste complète renvoyée par le service.
     On ne peut pas modifier son propre compte (le service le refuse) : les actions sont masquées sur sa ligne. --%>
<c:set var="titre" value="Utilisateurs et rôles" />
<c:set var="q" value="${fn:toLowerCase(fn:trim(param.q))}" />
<c:set var="roles" value="CLIENT,AGENT,ADMIN" />
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
        <form class="search" method="get" action="${ctx}/admin/users" role="search">
          <label class="field">Rôle
            <select name="filtreRole">
              <option value="">Tous</option>
              <c:forEach items="${roles}" var="r"><option value="${r}" ${param.filtreRole == r ? 'selected' : ''}>${r}</option></c:forEach>
            </select>
          </label>
          <label class="field">Rechercher<input type="search" name="q" value="${fn:escapeXml(param.q)}" placeholder="Nom ou e-mail"></label>
          <button class="btn" type="submit">Filtrer</button>
        </form>
      </header>
      <%@ include file="/WEB-INF/views/common/flash.jspf" %>
      <c:if test="${not empty erreurs}">
        <div class="alert alert-error" role="alert"><span><c:forEach items="${erreurs}" var="e"><c:out value="${e.value}" /> </c:forEach></span></div>
      </c:if>

      <section class="card flush" aria-label="Liste des utilisateurs">
        <c:set var="affiches" value="0" />
        <div class="table-wrap">
          <table class="table">
            <thead><tr><th>Nom</th><th>E-mail</th><th>Inscrit le</th><th>Rôle</th><th>Compte</th><th class="right">Actions</th></tr></thead>
            <tbody>
              <c:forEach items="${users}" var="us">
                <c:set var="texte" value="${fn:toLowerCase(us.firstName)} ${fn:toLowerCase(us.lastName)} ${fn:toLowerCase(us.email)}" />
                <c:if test="${(empty param.filtreRole or us.role.name == param.filtreRole) and (empty q or fn:contains(texte, q))}">
                  <c:set var="affiches" value="${affiches + 1}" />
                  <c:set var="estMoi" value="${us.id == sessionScope.utilisateur.id}" />
                  <tr>
                    <td class="strong"><c:out value="${us.firstName} ${us.lastName}" />${estMoi ? ' (vous)' : ''}</td>
                    <td class="muted"><c:out value="${us.email}" /></td>
                    <td class="muted small"><ui:date valeur="${us.createdAt}" /></td>
                    <td>
                      <c:choose>
                        <c:when test="${estMoi}"><span class="badge">${us.role.name}</span></c:when>
                        <c:otherwise>
                          <form class="inline" method="post" action="${ctx}/admin/users">
                            <%@ include file="/WEB-INF/views/common/csrf.jspf" %>
                            <input type="hidden" name="id" value="${us.id}"><input type="hidden" name="action" value="role">
                            <select name="role" aria-label="Rôle de ${fn:escapeXml(us.firstName)} ${fn:escapeXml(us.lastName)}">
                              <c:forEach items="${roles}" var="r"><option value="${r}" ${us.role.name == r ? 'selected' : ''}>${r}</option></c:forEach>
                            </select>
                            <button class="btn btn-sm" type="submit">Changer</button>
                          </form>
                        </c:otherwise>
                      </c:choose>
                    </td>
                    <td><span class="badge ${us.active ? 'badge-actif' : 'badge-suspendu'}">${us.active ? 'ACTIF' : 'DÉSACTIVÉ'}</span></td>
                    <td><div class="actions">
                      <c:if test="${not estMoi}">
                        <form method="post" action="${ctx}/admin/users"
                              <c:if test="${us.active}">data-confirm="Désactiver le compte de ${fn:escapeXml(us.email)} ?"</c:if>>
                          <%@ include file="/WEB-INF/views/common/csrf.jspf" %>
                          <input type="hidden" name="id" value="${us.id}"><input type="hidden" name="action" value="active">
                          <input type="hidden" name="value" value="${not us.active}">
                          <button class="btn-link ${us.active ? 'danger' : ''}" type="submit">${us.active ? 'Désactiver' : 'Activer'}</button>
                        </form>
                      </c:if>
                    </div></td>
                  </tr>
                </c:if>
              </c:forEach>
              <c:if test="${affiches == 0}"><tr><td colspan="6" class="empty">Aucun utilisateur.</td></tr></c:if>
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
