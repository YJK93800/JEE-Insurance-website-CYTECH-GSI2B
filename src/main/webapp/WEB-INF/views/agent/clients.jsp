<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/taglibs.jspf" %>
<%-- GET /agent/clients?q=texte[&id=5]
     Attributs : clients List<Client>{id, prenom, nom, email, telephone, dateInscription, nbContrats}
                 selection Client (peut être null) avec contrats List<Contrat> et sinistres List<Sinistre> --%>
<c:set var="titre" value="Clients" />
<!DOCTYPE html>
<html lang="fr">
<head><%@ include file="/WEB-INF/views/common/head.jspf" %></head>
<body>
<div class="app">
  <jsp:include page="/WEB-INF/views/common/sidebar.jsp"><jsp:param name="actif" value="clients" /></jsp:include>
  <main class="main">
    <div class="container">
      <header class="page-head">
        <div class="stack-sm"><span class="eyebrow">Espace agent</span><h1>Clients</h1></div>
        <form class="search" method="get" action="${ctx}/agent/clients" role="search">
          <label class="field">Rechercher
            <input type="search" name="q" value="${fn:escapeXml(param.q)}" placeholder="Nom, e-mail, téléphone…">
          </label>
          <button class="btn" type="submit">Rechercher</button>
        </form>
      </header>

      <div class="row">
        <section class="card flush col-main" aria-label="Liste des clients">
          <c:choose>
            <c:when test="${empty clients}"><p class="empty">Aucun client trouvé.</p></c:when>
            <c:otherwise>
              <div class="table-wrap">
                <table class="table">
                  <thead><tr><th>Client</th><th>E-mail</th><th>Téléphone</th><th>Inscrit le</th><th class="right">Contrats</th></tr></thead>
                  <tbody>
                    <c:forEach items="${clients}" var="cl">
                      <c:url var="lienCl" value="/agent/clients"><c:param name="q" value="${param.q}" /><c:param name="id" value="${cl.id}" /></c:url>
                      <tr class="${selection.id == cl.id ? 'selected' : ''}">
                        <td class="strong"><a href="${lienCl}"><c:out value="${cl.prenom} ${cl.nom}" /></a></td>
                        <td class="muted"><c:out value="${cl.email}" /></td>
                        <td class="muted"><c:out value="${cl.telephone}" /></td>
                        <td class="muted"><ui:date valeur="${cl.dateInscription}" /></td>
                        <td class="right strong">${cl.nbContrats}</td>
                      </tr>
                    </c:forEach>
                  </tbody>
                </table>
              </div>
            </c:otherwise>
          </c:choose>
        </section>

        <c:if test="${not empty selection}">
          <aside class="card col-side" aria-labelledby="h-cl">
            <div class="stack-sm">
              <h2 id="h-cl"><c:out value="${selection.prenom} ${selection.nom}" /></h2>
              <span class="small muted"><c:out value="${selection.email}" /> · <c:out value="${selection.telephone}" /></span>
              <span class="small muted"><c:out value="${selection.adresse}" /></span>
            </div>
            <a class="btn btn-primary btn-sm" href="${ctx}/agent/contrats/edit?clientId=${selection.id}">+ Nouveau contrat pour ce client</a>
            <div class="stack">
              <span class="small strong">Contrats</span>
              <c:forEach items="${selection.contrats}" var="ct">
                <div class="between small"><a href="${ctx}/agent/contrats/edit?id=${ct.id}"><c:out value="${ct.numero}" /> · <c:out value="${ct.typeAssurance.nom}" /></a><ui:statut valeur="${ct.statut}" /></div>
              </c:forEach>
              <c:if test="${empty selection.contrats}"><span class="small muted">Aucun contrat.</span></c:if>
            </div>
            <div class="stack">
              <span class="small strong">Sinistres</span>
              <c:forEach items="${selection.sinistres}" var="si">
                <div class="between small"><a href="${ctx}/agent/sinistres?id=${si.id}"><c:out value="${si.numero}" /></a><ui:statut valeur="${si.statut}" /></div>
              </c:forEach>
              <c:if test="${empty selection.sinistres}"><span class="small muted">Aucun sinistre.</span></c:if>
            </div>
          </aside>
        </c:if>
      </div>
    </div>
  </main>
</div>
<script src="${ctx}/js/app.js"></script>
</body>
</html>
