<%@ tag pageEncoding="UTF-8" body-content="empty" description="Badge coloré pour un statut (contrat, sinistre, paiement)" %>
<%@ attribute name="value" required="true" type="java.lang.Object" %>
<%@ attribute name="label" required="false" type="java.lang.String" %>
<%@ taglib prefix="c"  uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%-- value : un enum (ContractStatus, ClaimStatus, PaymentStatus) ou son nom.
     La classe CSS est le nom de l'enum en minuscules : ACTIF -> badge-actif, EN_ANALYSE -> badge-en_analyse.
     label (facultatif) : texte à afficher, ex. ContractStatus.label ; sinon libellé déduit du nom de l'enum. --%>
<c:set var="code" value="${fn:toUpperCase(value)}" />
<c:choose>
  <c:when test="${not empty label}"><c:set var="text" value="${fn:toUpperCase(label)}" /></c:when>
  <c:when test="${code == 'RESILIE'}"><c:set var="text" value="RÉSILIÉ" /></c:when>
  <c:when test="${code == 'EXPIRE'}"><c:set var="text" value="EXPIRÉ" /></c:when>
  <c:when test="${code == 'DECLARE'}"><c:set var="text" value="DÉCLARÉ" /></c:when>
  <c:when test="${code == 'ACCEPTE'}"><c:set var="text" value="ACCEPTÉ" /></c:when>
  <c:when test="${code == 'REFUSE'}"><c:set var="text" value="REFUSÉ" /></c:when>
  <c:when test="${code == 'INDEMNISE'}"><c:set var="text" value="INDEMNISÉ" /></c:when>
  <c:when test="${code == 'EFFECTUE'}"><c:set var="text" value="EFFECTUÉ" /></c:when>
  <c:when test="${code == 'ANNULE'}"><c:set var="text" value="ANNULÉ" /></c:when>
  <c:otherwise><c:set var="text" value="${fn:replace(code, '_', ' ')}" /></c:otherwise>
</c:choose>
<span class="badge badge-${fn:toLowerCase(code)}">${text}</span>
