<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/taglibs.jspf" %>
<%-- ContractFormServlet
     GET  /agent/contracts/edit          -> création       GET /agent/contracts/edit?id=12 -> modification (à venir)
     POST /agent/contracts/edit (id vide pour une création)
     Paramètres : id, clientId, typeAssuranceId, dateDebut, dateFin, prime, statut, garantieIds (plusieurs valeurs)
     Attributs : contract        Contract (null en création)
                 clients         List<User>
                 insuranceTypes  List<InsuranceType> (actifs, avec leurs garanties)
                 guarantees      List<Guarantee> (actives)
                 statuses        ContractStatus[] (modification seulement)
                 defaultStartDate, defaultEndDate   LocalDate (création : aujourd'hui, +1 an)
                 erreurs         Map<String,String>
     Le numéro de contrat (CTR-AAAA-NNNN) et le statut initial (EN_ATTENTE) sont décidés par le serveur. --%>
<c:set var="isCreation" value="${empty contract}" />
<c:set var="title" value="${isCreation ? 'Nouveau contrat' : 'Modifier le contrat'}" />
<%-- Valeurs affichées : paramètres renvoyés après une erreur, sinon valeurs du contrat --%>
<c:set var="selectedClient" value="${not empty param.clientId ? param.clientId : contract.client.id}" />
<c:set var="selectedType" value="${not empty param.insuranceTypeId ? param.insuranceTypeId : contract.insuranceType.id}" />
<c:set var="selectedStatus" value="${not empty param.status ? param.status : (isCreation ? 'EN_ATTENTE' : contract.status)}" />
<!DOCTYPE html>
<html lang="fr">
<head><%@ include file="/WEB-INF/views/common/head.jspf" %></head>
<body>
<div class="app">
  <jsp:include page="/WEB-INF/views/common/sidebar.jsp"><jsp:param name="active" value="contracts" /></jsp:include>
  <main class="main">
    <div class="container">
      <header class="stack-sm">
        <a class="back" href="${ctx}/agent/contracts">← Contrats</a>
        <h1>${title}</h1>
        <c:if test="${not isCreation}"><span class="muted"><c:out value="${contract.number}" /></span></c:if>
      </header>
      <%@ include file="/WEB-INF/views/common/flash.jspf" %>

      <form class="card form" method="post" action="${ctx}/agent/contracts/edit" novalidate data-validate>
        <%@ include file="/WEB-INF/views/common/csrf.jspf" %>
        <input type="hidden" name="id" value="${contract.id}">

        <div class="grid-2">
          <label class="field ${not empty errors.clientId ? 'has-error' : ''}">Client *
            <select name="clientId" required>
              <option value="">— Choisir un client —</option>
              <c:forEach items="${clients}" var="cl">
                <option value="${cl.id}" ${fn:trim(selectedClient) == fn:trim(cl.id) ? 'selected' : ''}><c:out value="${cl.lastName} ${cl.firstName}" /> · <c:out value="${cl.email}" /></option>
              </c:forEach>
            </select>
            <c:if test="${not empty errors.clientId}"><span class="field-error"><c:out value="${errors.clientId}" /></span></c:if>
          </label>
          <label class="field ${not empty errors.insuranceTypeId ? 'has-error' : ''}">Type d'assurance *
            <select name="insuranceTypeId" required data-insurance-type>
              <option value="">— Choisir un type —</option>
              <c:forEach items="${insuranceTypes}" var="it">
                <c:set var="gIds"><c:forEach items="${it.guarantees}" var="g" varStatus="gs">${g.id}${gs.last ? '' : ','}</c:forEach></c:set>
                <option value="${it.id}" data-price="${it.basePrice}" data-guarantees="${gIds}" ${fn:trim(selectedType) == fn:trim(it.id) ? 'selected' : ''}><c:out value="${it.name}" /></option>
              </c:forEach>
            </select>
            <c:if test="${not empty errors.insuranceTypeId}"><span class="field-error"><c:out value="${errors.insuranceTypeId}" /></span></c:if>
          </label>
        </div>

        <div class="grid-3">
          <ui:field name="startDate" label="Date de début *" type="date" required="true" value="${not empty param.startDate ? param.startDate : (isCreation ? defaultStartDate : contract.startDate)}" />
          <ui:field name="endDate" label="Date de fin *" type="date" required="true" value="${not empty param.endDate ? param.endDate : (isCreation ? defaultEndDate : contract.endDate)}" extra="data-after='startDate' data-auto-end" />
          <ui:field name="premium" label="Prime mensuelle (€) *" type="number" required="true" value="${not empty param.premium ? param.premium : contract.premium}"
                    extra="min='0.01' step='0.01' data-premium" help="Pré-remplie avec le prix de base du type d'assurance (utilisé aussi si laissée vide)." />
        </div>

        <%-- En création, le contrat démarre toujours EN_ATTENTE : pas de choix du statut --%>
        <c:if test="${not isCreation}">
        <label class="field ${not empty errors.status ? 'has-error' : ''}">Statut
          <select name="status">
            <c:forEach items="${statuses}" var="st"><option value="${st}" ${fn:trim(selectedStatus) == st.name() ? 'selected' : ''}>${st.label}</option></c:forEach>
          </select>
          <c:if test="${not empty errors.status}"><span class="field-error"><c:out value="${errors.status}" /></span></c:if>
        </label>
        </c:if>

        <fieldset>
          <legend>Garanties du contrat *</legend>
          <span class="field-help">Les garanties disponibles pour le type choisi sont mises en avant.</span>
          <div class="grid-3">
            <c:forEach items="${guarantees}" var="g">
              <c:set var="checked" value="false" />
              <c:forEach items="${paramValues.guaranteeIds}" var="pid"><c:if test="${pid == fn:trim(g.id)}"><c:set var="checked" value="true" /></c:if></c:forEach>
              <c:if test="${empty paramValues.guaranteeIds}">
                <c:forEach items="${contract.guarantees}" var="cg"><c:if test="${cg.id == g.id}"><c:set var="checked" value="true" /></c:if></c:forEach>
              </c:if>
              <label class="choice" data-guarantee="${g.id}">
                <input type="checkbox" name="guaranteeIds" value="${g.id}" ${checked ? 'checked' : ''}>
                <span class="stack-sm"><span class="strong"><c:out value="${g.name}" /></span><span class="xsmall muted"><c:out value="${g.description}" /></span></span>
              </label>
            </c:forEach>
          </div>
          <c:if test="${not empty errors.guaranteeIds}"><span class="field-error"><c:out value="${errors.guaranteeIds}" /></span></c:if>
        </fieldset>

        <div class="form-actions">
          <a class="btn" href="${ctx}/agent/contracts">Annuler</a>
          <button type="submit" class="btn btn-primary">${isCreation ? 'Créer le contrat' : 'Enregistrer les modifications'}</button>
        </div>
      </form>
    </div>
  </main>
</div>
<script src="${ctx}/js/app.js"></script>
</body>
</html>
