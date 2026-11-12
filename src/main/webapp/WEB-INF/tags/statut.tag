<%@ tag pageEncoding="UTF-8" body-content="empty" description="Badge coloré pour un statut (contrat, sinistre, paiement)" %>
<%@ attribute name="valeur" required="true" type="java.lang.Object" %>
<%@ taglib prefix="c"  uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%-- La classe CSS est le nom de l'enum en minuscules : ACTIF -> badge-actif, EN_ANALYSE -> badge-en_analyse --%>
<c:set var="code" value="${fn:toUpperCase(valeur)}" />
<c:choose>
  <c:when test="${code == 'RESILIE'}"><c:set var="libelle" value="RÉSILIÉ" /></c:when>
  <c:when test="${code == 'PAYE'}"><c:set var="libelle" value="PAYÉ" /></c:when>
  <c:when test="${code == 'A_VENIR'}"><c:set var="libelle" value="À VENIR" /></c:when>
  <c:otherwise><c:set var="libelle" value="${code}" /></c:otherwise>
</c:choose>
<span class="badge badge-${fn:toLowerCase(code)}">${libelle}</span>
