<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- Page d'accueil : redirige vers l'espace correspondant au rôle, ou vers la connexion. --%>
<c:set var="u" value="${sessionScope.currentUser}" />
<c:choose>
  <c:when test="${u.role == 'ADMIN'}"><c:redirect url="/admin/stats" /></c:when>
  <c:when test="${u.role == 'AGENT'}"><c:redirect url="/agent/claims" /></c:when>
  <c:when test="${u.role == 'CLIENT'}"><c:redirect url="/client/contracts" /></c:when>
  <c:otherwise><c:redirect url="/login" /></c:otherwise>
</c:choose>
