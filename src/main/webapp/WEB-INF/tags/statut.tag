<%@ tag pageEncoding="UTF-8" body-content="empty" description="Badge coloré pour un statut (contrat, sinistre, paiement)" %>
<%@ attribute name="valeur" required="true" type="java.lang.Object" %>
<%@ attribute name="libelle" required="false" type="java.lang.String" %>
<%@ taglib prefix="c"  uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%-- valeur : un enum (ContractStatus, ClaimStatus, PaymentStatus) ou son nom.
     La classe CSS est le nom de l'enum en minuscules : ACTIF -> badge-actif, EN_ANALYSE -> badge-en_analyse.
     libelle (facultatif) : texte à afficher, ex. ContractStatus.label ; sinon libellé déduit du nom de l'enum. --%>
<c:set var="code" value="${fn:toUpperCase(valeur)}" />
<c:choose>
  <c:when test="${not empty libelle}"><c:set var="texte" value="${fn:toUpperCase(libelle)}" /></c:when>
  <c:when test="${code == 'RESILIE'}"><c:set var="texte" value="RÉSILIÉ" /></c:when>
  <c:when test="${code == 'EXPIRE'}"><c:set var="texte" value="EXPIRÉ" /></c:when>
  <c:when test="${code == 'DECLARE'}"><c:set var="texte" value="DÉCLARÉ" /></c:when>
  <c:when test="${code == 'ACCEPTE'}"><c:set var="texte" value="ACCEPTÉ" /></c:when>
  <c:when test="${code == 'REFUSE'}"><c:set var="texte" value="REFUSÉ" /></c:when>
  <c:when test="${code == 'INDEMNISE'}"><c:set var="texte" value="INDEMNISÉ" /></c:when>
  <c:when test="${code == 'EFFECTUE'}"><c:set var="texte" value="EFFECTUÉ" /></c:when>
  <c:when test="${code == 'ANNULE'}"><c:set var="texte" value="ANNULÉ" /></c:when>
  <c:otherwise><c:set var="texte" value="${fn:replace(code, '_', ' ')}" /></c:otherwise>
</c:choose>
<span class="badge badge-${fn:toLowerCase(code)}">${texte}</span>
