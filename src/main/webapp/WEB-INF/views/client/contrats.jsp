<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/taglibs.jspf" %>
<%-- GET /client/contrats?statut=ACTIF
     Attributs : contrats List<Contrat> (déjà filtrés par la servlet selon le paramètre "statut")
                 statuts  StatutContrat[]  (StatutContrat.values()) --%>
<c:set var="titre" value="Mes contrats" />
<!DOCTYPE html>
<html lang="fr">
<head><%@ include file="/WEB-INF/views/common/head.jspf" %></head>
<body>
<div class="app">
  <jsp:include page="/WEB-INF/views/common/sidebar.jsp"><jsp:param name="actif" value="contrats" /></jsp:include>
  <main class="main">
    <div class="container">
      <header class="page-head">
        <div class="stack-sm"><span class="eyebrow">Espace client</span><h1>Mes contrats</h1></div>
      </header>
      <%@ include file="/WEB-INF/views/common/flash.jspf" %>

      <nav class="tabs" aria-label="Filtrer par statut">
        <a href="${ctx}/client/contrats" class="${empty param.statut ? 'active' : ''}">Tous</a>
        <c:forEach items="${statuts}" var="st">
          <a href="${ctx}/client/contrats?statut=${st}" class="${param.statut == st.name() ? 'active' : ''}"><ui:statut valeur="${st}" /></a>
        </c:forEach>
      </nav>

      <section class="stack" aria-label="Liste des contrats">
        <c:forEach items="${contrats}" var="ct">
          <article class="card">
            <div class="between">
              <div class="stack-sm">
                <h2><c:out value="${ct.typeAssurance.nom}" /></h2>
                <span class="small muted"><c:out value="${ct.numero}" /> · du <ui:date valeur="${ct.dateDebut}" /> au <ui:date valeur="${ct.dateFin}" /></span>
              </div>
              <div class="inline">
                <span class="amount"><ui:montant valeur="${ct.prime}" /> <small>/mois</small></span>
                <ui:statut valeur="${ct.statut}" />
              </div>
            </div>
            <div class="inline">
              <c:forEach items="${ct.garanties}" var="g"><span class="chip"><c:out value="${g.nom}" /></span></c:forEach>
            </div>
            <div class="inline">
              <a class="btn btn-sm" href="${ctx}/client/contrats/detail?id=${ct.id}">Voir le détail</a>
              <c:if test="${ct.statut == 'ACTIF'}">
                <a class="btn btn-sm" href="${ctx}/client/sinistres/declarer?contrat=${ct.id}">Déclarer un sinistre</a>
              </c:if>
            </div>
          </article>
        </c:forEach>
        <c:if test="${empty contrats}"><div class="card"><p class="empty">Aucun contrat pour ce filtre.</p></div></c:if>
      </section>
    </div>
  </main>
</div>
<script src="${ctx}/js/app.js"></script>
</body>
</html>
