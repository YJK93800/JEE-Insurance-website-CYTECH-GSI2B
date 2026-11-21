<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/taglibs.jspf" %>
<%-- Barre latérale unique : le menu dépend du rôle de l'utilisateur en session.
     Utilisation : <jsp:include page="/WEB-INF/views/common/sidebar.jsp"><jsp:param name="actif" value="contrats"/></jsp:include>
     Attendu en session : "utilisateur" (prenom, nom, role = CLIENT | AGENT | ADMIN).
     Optionnel en requête : "nbSinistresATraiter" (badge du menu agent). --%>
<c:set var="u" value="${sessionScope.utilisateur}" />
<c:set var="actif" value="${param.actif}" />
<nav class="sidebar" aria-label="Navigation principale">
  <div class="brand">
    <%@ include file="/WEB-INF/views/common/logo.jspf" %>
    <span class="brand-name">Égide</span>
    <c:if test="${u.role == 'AGENT'}"><span class="role-tag">AGENT</span></c:if>
    <c:if test="${u.role == 'ADMIN'}"><span class="role-tag admin">ADMIN</span></c:if>
  </div>

  <div class="nav">
    <c:choose>
      <c:when test="${u.role == 'AGENT'}">
        <a href="${ctx}/agent/clients"   class="${actif == 'clients'   ? 'active' : ''}">Clients</a>
        <a href="${ctx}/agent/contrats"  class="${actif == 'contrats'  ? 'active' : ''}">Contrats</a>
        <a href="${ctx}/agent/sinistres" class="${actif == 'sinistres' ? 'active' : ''}">Sinistres
          <c:if test="${nbSinistresATraiter > 0}"><span class="count">${nbSinistresATraiter}</span></c:if></a>
        <a href="${ctx}/agent/garanties" class="${actif == 'garanties' ? 'active' : ''}">Garanties</a>
        <a href="${ctx}/agent/paiements" class="${actif == 'paiements' ? 'active' : ''}">Paiements</a>
        <a href="${ctx}/agent/stats"     class="${actif == 'stats'     ? 'active' : ''}">Statistiques</a>
      </c:when>
      <c:when test="${u.role == 'ADMIN'}">
        <a href="${ctx}/admin/stats"        class="${actif == 'stats'        ? 'active' : ''}">Statistiques</a>
        <a href="${ctx}/admin/utilisateurs" class="${actif == 'utilisateurs' ? 'active' : ''}">Utilisateurs &amp; rôles</a>
        <a href="${ctx}/admin/types"        class="${actif == 'types'        ? 'active' : ''}">Types d'assurance</a>
        <a href="${ctx}/admin/contrats"     class="${actif == 'contrats'     ? 'active' : ''}">Tous les contrats</a>
        <a href="${ctx}/admin/sinistres"    class="${actif == 'sinistres'    ? 'active' : ''}">Tous les sinistres</a>
        <a href="${ctx}/admin/stats#indicateurs">Indicateurs de performance</a>
      </c:when>
      <c:otherwise>
        <a href="${ctx}/client/dashboard"  class="${actif == 'dashboard'  ? 'active' : ''}">Tableau de bord</a>
        <a href="${ctx}/client/contrats"   class="${actif == 'contrats'   ? 'active' : ''}">Mes contrats</a>
        <a href="${ctx}/client/garanties"  class="${actif == 'garanties'  ? 'active' : ''}">Mes garanties</a>
        <a href="${ctx}/client/paiements"  class="${actif == 'paiements'  ? 'active' : ''}">Paiements</a>
        <a href="${ctx}/client/sinistres"  class="${actif == 'sinistres'  ? 'active' : ''}">Sinistres</a>
        <a href="${ctx}/client/historique" class="${actif == 'historique' ? 'active' : ''}">Historique des demandes</a>
        <a href="${ctx}/client/assistant"  class="${actif == 'assistant'  ? 'active' : ''}">Assistant IA</a>
      </c:otherwise>
    </c:choose>
  </div>

  <div class="user-box">
    <div class="avatar ${fn:toLowerCase(u.role)}" aria-hidden="true">${fn:toUpperCase(fn:substring(u.prenom, 0, 1))}${fn:toUpperCase(fn:substring(u.nom, 0, 1))}</div>
    <div class="stack-sm">
      <span class="user-name"><c:out value="${u.prenom} ${u.nom}" /></span>
      <span class="user-links">
        <c:if test="${u.role == 'CLIENT'}"><a href="${ctx}/client/profil">Mon profil</a> · </c:if>
        <a href="${ctx}/logout">Déconnexion</a>
      </span>
    </div>
  </div>
</nav>
