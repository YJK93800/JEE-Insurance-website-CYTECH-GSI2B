<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/taglibs.jspf" %>
<%-- Barre latérale unique : le menu dépend du rôle de l'utilisateur en session.
     Utilisation : <jsp:include page="/WEB-INF/views/common/sidebar.jsp"><jsp:param name="active" value="contracts"/></jsp:include>
     Attendu en session : "currentUser" (firstName, lastName, role = CLIENT | AGENT | ADMIN).
     Optionnel en requête : "pendingClaimCount" (badge du menu agent). --%>
<c:set var="u" value="${sessionScope.currentUser}" />
<c:set var="active" value="${param.active}" />
<nav class="sidebar" aria-label="Navigation principale">
  <div class="brand">
    <%@ include file="/WEB-INF/views/common/logo.jspf" %>
    <span class="brand-name">AssurCY2000</span>
    <c:if test="${u.role == 'AGENT'}"><span class="role-tag">AGENT</span></c:if>
    <c:if test="${u.role == 'ADMIN'}"><span class="role-tag admin">ADMIN</span></c:if>
  </div>

  <div class="nav">
    <c:choose>
      <c:when test="${u.role == 'AGENT'}">
        <a href="${ctx}/agent/clients"   class="${active == 'clients'   ? 'active' : ''}">Clients</a>
        <a href="${ctx}/agent/contracts"  class="${active == 'contracts'  ? 'active' : ''}">Contrats</a>
        <a href="${ctx}/agent/claims" class="${active == 'claims' ? 'active' : ''}">Sinistres
          <c:if test="${pendingClaimCount > 0}"><span class="count">${pendingClaimCount}</span></c:if></a>
        <a href="${ctx}/agent/garanties" class="${active == 'guarantees' ? 'active' : ''}">Garanties</a>
        <a href="${ctx}/agent/payments"  class="${active == 'payments'  ? 'active' : ''}">Paiements</a>
        <a href="${ctx}/agent/stats"     class="${active == 'stats'     ? 'active' : ''}">Statistiques</a>
      </c:when>
      <c:when test="${u.role == 'ADMIN'}">
        <a href="${ctx}/admin/stats"        class="${active == 'stats'        ? 'active' : ''}">Statistiques</a>
        <a href="${ctx}/admin/users" class="${active == 'users' ? 'active' : ''}">Utilisateurs &amp; rôles</a>
        <a href="${ctx}/admin/types"        class="${active == 'types'        ? 'active' : ''}">Types d'assurance</a>
        <a href="${ctx}/admin/contracts"     class="${active == 'contracts'     ? 'active' : ''}">Tous les contrats</a>
        <a href="${ctx}/admin/claims"    class="${active == 'claims'    ? 'active' : ''}">Tous les sinistres</a>
        <a href="${ctx}/admin/payments"  class="${active == 'payments'  ? 'active' : ''}">Tous les paiements</a>
        <a href="${ctx}/admin/stats#indicateurs">Indicateurs de performance</a>
      </c:when>
      <c:otherwise>
        <a href="${ctx}/client/dashboard"  class="${active == 'dashboard'  ? 'active' : ''}">Tableau de bord</a>
        <a href="${ctx}/client/contracts"   class="${active == 'contracts'   ? 'active' : ''}">Mes contrats</a>
        <a href="${ctx}/client/garanties"  class="${active == 'guarantees'  ? 'active' : ''}">Mes garanties</a>
        <a href="${ctx}/client/payments"  class="${active == 'payments'  ? 'active' : ''}">Paiements</a>
        <a href="${ctx}/client/claims"  class="${active == 'claims'  ? 'active' : ''}">Sinistres</a>
        <a href="${ctx}/client/historique" class="${active == 'history' ? 'active' : ''}">Historique des demandes</a>
        <a href="${ctx}/client/assistant"  class="${active == 'assistant'  ? 'active' : ''}">Assistant IA</a>
      </c:otherwise>
    </c:choose>
  </div>

  <div class="user-box">
    <div class="avatar ${fn:toLowerCase(u.role)}" aria-hidden="true">${fn:toUpperCase(fn:substring(u.firstName, 0, 1))}${fn:toUpperCase(fn:substring(u.lastName, 0, 1))}</div>
    <div class="stack-sm">
      <span class="user-name"><c:out value="${u.firstName} ${u.lastName}" /></span>
      <span class="user-links">
        <a href="${ctx}/profile" class="${active == 'profile' ? 'active' : ''}">Mon profil</a> ·
        <form method="post" action="${ctx}/logout" class="logout-form">
          <%@ include file="/WEB-INF/views/common/csrf.jspf" %>
          <button type="submit" class="link-button">Déconnexion</button>
        </form>
      </span>
    </div>
  </div>
</nav>
