<%@ tag pageEncoding="UTF-8" description="Champ de formulaire avec libellé, valeur ressaisie et message d'erreur venant de la servlet" %>
<%@ attribute name="name" required="true" %>
<%@ attribute name="label" required="true" %>
<%@ attribute name="type" %>
<%@ attribute name="value" type="java.lang.Object" %>
<%@ attribute name="required" type="java.lang.Boolean" %>
<%@ attribute name="help" %>
<%@ attribute name="extra" description="attributs HTML supplémentaires : placeholder, min, step…" %>
<%@ taglib prefix="c"  uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%-- La servlet place une Map<String,String> "errors" dans la requête : errors.put("email", "Email invalide").
     La valeur ressaisie est cherchée dans "value", sinon dans le paramètre de la requête. --%>
<c:set var="err" value="${requestScope.errors[name]}" />
<c:set var="v" value="${not empty value ? value : param[name]}" />
<label class="field ${not empty err ? 'has-error' : ''}">${label}
  <input type="${empty type ? 'text' : type}" name="${name}" id="f-${name}"
         value="${type == 'password' ? '' : fn:escapeXml(v)}"
         ${required ? 'required' : ''} ${extra}
         <c:if test="${not empty err}">aria-invalid="true" aria-describedby="e-${name}"</c:if>>
  <c:if test="${not empty err}"><span class="field-error" id="e-${name}"><c:out value="${err}" /></span></c:if>
  <c:if test="${not empty help and empty err}"><span class="field-help"><c:out value="${help}" /></span></c:if>
</label>
