<%@ tag pageEncoding="UTF-8" body-content="empty" description="Libellé français d'un ClaimType" %>
<%@ attribute name="value" required="true" type="java.lang.Object" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- value : un ClaimType (VOL, INCENDIE, ACCIDENT, DEGATS_DES_EAUX, BRIS, MALADIE, AUTRE) ou son nom. --%>
<c:choose>
  <c:when test="${value == 'VOL'}">Vol</c:when>
  <c:when test="${value == 'INCENDIE'}">Incendie</c:when>
  <c:when test="${value == 'ACCIDENT'}">Accident</c:when>
  <c:when test="${value == 'DEGATS_DES_EAUX'}">Dégâts des eaux</c:when>
  <c:when test="${value == 'BRIS'}">Bris</c:when>
  <c:when test="${value == 'MALADIE'}">Maladie</c:when>
  <c:when test="${value == 'AUTRE'}">Autre</c:when>
  <c:when test="${empty value}">—</c:when>
  <c:otherwise><c:out value="${value}" /></c:otherwise>
</c:choose>
