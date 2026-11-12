<%@ tag pageEncoding="UTF-8" description="Champ de formulaire avec libellé, valeur ressaisie et message d'erreur venant de la servlet" %>
<%@ attribute name="nom" required="true" %>
<%@ attribute name="libelle" required="true" %>
<%@ attribute name="type" %>
<%@ attribute name="valeur" type="java.lang.Object" %>
<%@ attribute name="requis" type="java.lang.Boolean" %>
<%@ attribute name="aide" %>
<%@ attribute name="extra" description="attributs HTML supplémentaires : placeholder, min, step…" %>
<%@ taglib prefix="c"  uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%-- La servlet place une Map<String,String> "erreurs" dans la requête : erreurs.put("email", "Email invalide").
     La valeur ressaisie est cherchée dans "valeur", sinon dans le paramètre de la requête. --%>
<c:set var="err" value="${requestScope.erreurs[nom]}" />
<c:set var="v" value="${not empty valeur ? valeur : param[nom]}" />
<label class="field ${not empty err ? 'has-error' : ''}">${libelle}
  <input type="${empty type ? 'text' : type}" name="${nom}" id="f-${nom}"
         value="${type == 'password' ? '' : fn:escapeXml(v)}"
         ${requis ? 'required' : ''} ${extra}
         <c:if test="${not empty err}">aria-invalid="true" aria-describedby="e-${nom}"</c:if>>
  <c:if test="${not empty err}"><span class="field-error" id="e-${nom}"><c:out value="${err}" /></span></c:if>
  <c:if test="${not empty aide and empty err}"><span class="field-help"><c:out value="${aide}" /></span></c:if>
</label>
