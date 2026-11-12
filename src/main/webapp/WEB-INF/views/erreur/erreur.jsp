<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<%@ include file="/WEB-INF/views/common/taglibs.jspf" %>
<%-- Page d'erreur unique, déclarée dans web.xml pour les codes 403, 404 et 500.
     Le détail technique de l'exception n'est jamais montré à l'utilisateur. --%>
<c:set var="code" value="${requestScope['jakarta.servlet.error.status_code']}" />
<c:set var="titre" value="Erreur ${code}" />
<!DOCTYPE html>
<html lang="fr">
<head><%@ include file="/WEB-INF/views/common/head.jspf" %></head>
<body>
<main class="error-page">
  <div class="card">
    <span class="error-code">${empty code ? 'Oups' : code}</span>
    <h1>
      <c:choose>
        <c:when test="${code == 403}">Accès refusé</c:when>
        <c:when test="${code == 404}">Page introuvable</c:when>
        <c:otherwise>Une erreur est survenue</c:otherwise>
      </c:choose>
    </h1>
    <p class="muted">
      <c:choose>
        <c:when test="${code == 403}">Vous n'avez pas les droits pour accéder à cette page.</c:when>
        <c:when test="${code == 404}">L'adresse demandée n'existe pas ou a été déplacée.</c:when>
        <c:otherwise>Réessayez dans quelques instants. Si le problème persiste, contactez votre agent.</c:otherwise>
      </c:choose>
    </p>
    <div class="inline">
      <a class="btn" href="javascript:history.back()">Retour</a>
      <a class="btn btn-primary" href="${ctx}/">Accueil</a>
    </div>
  </div>
</main>
</body>
</html>
