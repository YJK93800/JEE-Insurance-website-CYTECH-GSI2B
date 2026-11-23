<%@ tag pageEncoding="UTF-8" body-content="empty" description="Libellé français d'un ClaimType" %>
<%@ attribute name="valeur" required="true" type="java.lang.Object" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- valeur : un ClaimType (VOL, INCENDIE, ACCIDENT, DEGATS_DES_EAUX, BRIS, MALADIE, AUTRE) ou son nom. --%>
<c:choose>
  <c:when test="${valeur == 'VOL'}">Vol</c:when>
  <c:when test="${valeur == 'INCENDIE'}">Incendie</c:when>
  <c:when test="${valeur == 'ACCIDENT'}">Accident</c:when>
  <c:when test="${valeur == 'DEGATS_DES_EAUX'}">Dégâts des eaux</c:when>
  <c:when test="${valeur == 'BRIS'}">Bris</c:when>
  <c:when test="${valeur == 'MALADIE'}">Maladie</c:when>
  <c:when test="${valeur == 'AUTRE'}">Autre</c:when>
  <c:when test="${empty valeur}">—</c:when>
  <c:otherwise><c:out value="${valeur}" /></c:otherwise>
</c:choose>
