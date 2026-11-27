<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/taglibs.jspf" %>
<%-- GET /client/historique (servlet : ClientHistoryServlet)
     Attributs : conversation List<ChatLine>  (fromClient, content, sentAt) : les 100 derniers messages avec l'assistant, du plus ancien au plus récent
                 requests     List<HistoryEntry> (date, type, reference, link, status) : sinistres et contrats, du plus récent au plus ancien --%>
<c:set var="title" value="Historique des demandes" />
<!DOCTYPE html>
<html lang="fr">
<head><%@ include file="/WEB-INF/views/common/head.jspf" %></head>
<body>
<div class="app">
  <jsp:include page="/WEB-INF/views/common/sidebar.jsp"><jsp:param name="active" value="history" /></jsp:include>
  <main class="main">
    <div class="container">
      <header class="page-head"><div class="stack-sm"><span class="eyebrow">Espace client</span><h1>Historique des demandes</h1></div></header>
      <section class="card">
        <h2>Conversations avec l'assistant</h2>
        <c:choose>
          <c:when test="${empty conversation}">
            <p class="empty">Aucune conversation pour le moment. <a href="${ctx}/client/assistant">Poser une question à l'assistant</a></p>
          </c:when>
          <c:otherwise>
            <div class="chat-body">
              <c:forEach items="${conversation}" var="m">
                <div class="msg ${m.fromClient ? 'user' : 'bot'}"><c:out value="${m.content}" /></div>
                <div style="font-size:12px;color:var(--muted);align-self:${m.fromClient ? 'flex-end' : 'flex-start'}">
                  <ui:date value="${m.sentAt}" /> à ${m.sentAt.hour}h${m.sentAt.minute lt 10 ? '0' : ''}${m.sentAt.minute}
                </div>
              </c:forEach>
            </div>
          </c:otherwise>
        </c:choose>
      </section>
      <section class="card flush">
        <h2 style="padding:20px 24px 0">Mes demandes</h2>
        <c:choose>
          <c:when test="${empty requests}"><p class="empty">Aucune demande pour le moment.</p></c:when>
          <c:otherwise>
            <div class="table-wrap">
              <table class="table">
                <thead><tr><th>Date</th><th>Demande</th><th>Référence</th><th>Statut</th></tr></thead>
                <tbody>
                  <c:forEach items="${requests}" var="d">
                    <tr>
                      <td><ui:date value="${d.date}" /></td>
                      <td><c:out value="${d.type}" /></td>
                      <td class="strong">
                        <c:choose>
                          <c:when test="${not empty d.link}"><a href="${ctx}${d.link}"><c:out value="${d.reference}" /></a></c:when>
                          <c:otherwise><c:out value="${d.reference}" /></c:otherwise>
                        </c:choose>
                      </td>
                      <td><c:if test="${not empty d.status}"><ui:status value="${d.status}" /></c:if></td>
                    </tr>
                  </c:forEach>
                </tbody>
              </table>
            </div>
          </c:otherwise>
        </c:choose>
      </section>
    </div>
  </main>
</div>
<script src="${ctx}/js/app.js"></script>
</body>
</html>
