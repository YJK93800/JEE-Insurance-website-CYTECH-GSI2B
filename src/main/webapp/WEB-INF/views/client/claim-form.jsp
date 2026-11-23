<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/taglibs.jspf" %>
<%-- ClientClaimsServlet — GET  /client/claims?new=1[&contractId=12] -> cette vue
                           POST /client/claims                       -> ClaimService.declare
     Paramètres envoyés : contractId, type (ClaimType), claimDate (aaaa-mm-jj), estimatedAmount, description
     Attributs : contracts  List<Contract>  (contrats ACTIF du client, insuranceType chargé)
                 claimTypes ClaimType[]
                 errors     Map<String,String> en cas d'échec (clés = noms des paramètres ci-dessus)
     Après une erreur, la page est réaffichée par forward : les valeurs saisies sont relues dans param.* --%>
<c:set var="title" value="Déclarer un sinistre" />
<c:set var="selectedContract" value="${param.contractId}" />
<!DOCTYPE html>
<html lang="fr">
<head><%@ include file="/WEB-INF/views/common/head.jspf" %></head>
<body>
<div class="app">
  <jsp:include page="/WEB-INF/views/common/sidebar.jsp"><jsp:param name="active" value="claims" /></jsp:include>
  <main class="main">
    <div class="container">
      <header class="stack-sm">
        <a class="back" href="${ctx}/client/claims">← Mes sinistres</a>
        <h1>Déclarer un sinistre</h1>
        <p class="muted">Environ 5 minutes. Les champs marqués * sont obligatoires.</p>
      </header>

      <%@ include file="/WEB-INF/views/common/flash.jspf" %>
      <c:if test="${not empty errors}">
        <div class="alert alert-error" role="alert"><span>Le formulaire contient ${fn:length(errors)} erreur(s). Corrigez les champs signalés.</span></div>
      </c:if>

      <c:choose>
        <c:when test="${empty contracts}">
          <div class="card"><p class="empty">Vous n'avez aucun contrat actif : il n'est pas possible de déclarer un sinistre pour le moment.</p></div>
        </c:when>
        <c:otherwise>
      <div class="row">
        <form class="card form col-main" method="post" action="${ctx}/client/claims" novalidate data-validate id="claim-form">
          <%@ include file="/WEB-INF/views/common/csrf.jspf" %>

          <fieldset class="${not empty errors.contractId ? 'has-error' : ''}">
            <legend>Contrat concerné *</legend>
            <c:forEach items="${contracts}" var="ct" varStatus="i">
              <label class="choice">
                <input type="radio" name="contractId" value="${ct.id}" required
                       data-number="${fn:escapeXml(ct.number)}"
                       ${selectedContract == fn:trim(ct.id) or (empty selectedContract and i.first) ? 'checked' : ''}>
                <span class="stack-sm">
                  <span class="strong"><c:out value="${ct.insuranceType.name}" /></span>
                  <span class="small muted"><c:out value="${ct.number}" /> · du <ui:date value="${ct.startDate}" /> au <ui:date value="${ct.endDate}" /></span>
                </span>
              </label>
            </c:forEach>
            <c:if test="${not empty errors.contractId}"><span class="field-error"><c:out value="${errors.contractId}" /></span></c:if>
            <span class="field-help">Seuls vos contrats actifs sont proposés.</span>
          </fieldset>

          <fieldset class="${not empty errors.type ? 'has-error' : ''}">
            <legend>Type de sinistre *</legend>
            <div class="pills" role="radiogroup">
              <c:forEach items="${claimTypes}" var="t" varStatus="i">
                <c:set var="typeLabel"><ui:claimType value="${t}" /></c:set>
                <%-- Documents conseillés, affichés à droite par app.js (séparés par |) --%>
                <c:choose>
                  <c:when test="${t == 'VOL'}"><c:set var="docs" value="Récépissé de dépôt de plainte|Factures ou photos des biens volés" /></c:when>
                  <c:when test="${t == 'INCENDIE'}"><c:set var="docs" value="Photos des dégâts|Rapport des pompiers si intervention|Factures des biens endommagés" /></c:when>
                  <c:when test="${t == 'ACCIDENT'}"><c:set var="docs" value="Constat amiable|Photos des dégâts|Coordonnées des témoins" /></c:when>
                  <c:when test="${t == 'DEGATS_DES_EAUX'}"><c:set var="docs" value="Photos des dégâts|Constat dégât des eaux|Factures de réparation" /></c:when>
                  <c:when test="${t == 'BRIS'}"><c:set var="docs" value="Photos du bien endommagé|Facture d'achat" /></c:when>
                  <c:when test="${t == 'MALADIE'}"><c:set var="docs" value="Certificat médical|Justificatifs des frais engagés" /></c:when>
                  <c:otherwise><c:set var="docs" value="Tout justificatif utile à l'analyse du dossier" /></c:otherwise>
                </c:choose>
                <label class="pill">
                  <input type="radio" name="type" value="${t}" required
                         data-label="${fn:escapeXml(fn:trim(typeLabel))}"
                         data-docs="${fn:escapeXml(docs)}"
                         ${param.type == t.name() or (empty param.type and i.first) ? 'checked' : ''}>
                  <span>${fn:trim(typeLabel)}</span>
                </label>
              </c:forEach>
            </div>
            <c:if test="${not empty errors.type}"><span class="field-error"><c:out value="${errors.type}" /></span></c:if>
          </fieldset>

          <div class="grid-2">
            <ui:field name="claimDate" label="Date du sinistre *" type="date" required="true" extra="data-max-today" />
            <ui:field name="estimatedAmount" label="Montant estimé (€) *" type="number" required="true"
                      extra="min='0.01' step='0.01' inputmode='decimal'" />
          </div>

          <label class="field ${not empty errors.description ? 'has-error' : ''}">Description de l'événement *
            <textarea name="description" rows="5" minlength="10" maxlength="2000" required data-counter="desc-counter"
                      placeholder="Que s'est-il passé ? Où ? Quels biens sont endommagés ?"><c:out value="${param.description}" /></textarea>
            <c:if test="${not empty errors.description}"><span class="field-error"><c:out value="${errors.description}" /></span></c:if>
            <span class="field-help" id="desc-counter">0 / 2000 caractères (10 minimum)</span>
          </label>

          <div class="form-actions">
            <a class="btn" href="${ctx}/client/claims">Annuler</a>
            <button type="submit" class="btn btn-primary">Envoyer la déclaration</button>
          </div>
        </form>

        <aside class="col-side">
          <section class="card" aria-labelledby="h-docs">
            <span><span class="badge badge-ai">CONSEIL</span></span>
            <h2 id="h-docs">Documents à préparer · <span data-type-label></span></h2>
            <ul class="stack small" data-docs-list></ul>
            <span class="xsmall muted">Votre agent vous les demandera pendant l'analyse du dossier.</span>
          </section>

          <section class="card" aria-labelledby="h-recap">
            <h2 id="h-recap">Votre déclaration</h2>
            <div class="between small"><span class="muted">Contrat</span><span class="strong" data-recap="contract">—</span></div>
            <div class="between small"><span class="muted">Type</span><span class="strong" data-type-label>—</span></div>
            <div class="between small"><span class="muted">Date</span><span class="strong" data-recap="date">—</span></div>
            <div class="between small"><span class="muted">Statut à l'envoi</span><ui:status value="DECLARE" /></div>
          </section>
        </aside>
      </div>
        </c:otherwise>
      </c:choose>
    </div>
  </main>
</div>
<script src="${ctx}/js/app.js"></script>
</body>
</html>
