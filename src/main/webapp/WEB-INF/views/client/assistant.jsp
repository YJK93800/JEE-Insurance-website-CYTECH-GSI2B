<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/taglibs.jspf" %>
<%-- GET  /client/assistant[?q=question]  -> cette vue
     POST /client/assistant/message (JSON {"message": "..."} ) -> AssistantServlet
          répond {"reponse": "..."} ou {"erreur": "..."}.
          C'est la servlet qui appelle l'API d'IA avec les données du client : la clé d'API reste côté serveur.
     Attributs : nbContrats, nbGaranties, nbSinistres, nbPaiements (int)  agentReferent Utilisateur (peut être null) --%>
<c:set var="titre" value="Assistant IA" />
<!DOCTYPE html>
<html lang="fr">
<head><%@ include file="/WEB-INF/views/common/head.jspf" %></head>
<body>
<div class="app">
  <jsp:include page="/WEB-INF/views/common/sidebar.jsp"><jsp:param name="actif" value="assistant" /></jsp:include>
  <main class="main">
    <div class="container">
      <div class="row">
        <section class="card chat col-main" aria-labelledby="h-chat">
          <header class="chat-head between">
            <div class="stack-sm">
              <h1 id="h-chat">Assistant Égide</h1>
              <span class="small muted">Répond à partir de vos contrats, garanties, paiements et sinistres</span>
            </div>
            <button type="button" class="btn btn-sm" data-chat-reset>Nouvelle conversation</button>
          </header>

          <div class="chat-body" data-chat-body aria-live="polite">
            <div class="msg bot">Bonjour <c:out value="${sessionScope.utilisateur.prenom}" />, je peux vous renseigner sur vos contrats, vos garanties, vos paiements et le suivi de vos sinistres. Que souhaitez-vous savoir ?</div>
          </div>

          <div class="chat-foot">
            <div class="inline">
              <c:forEach items="${['Quels sont mes contrats actifs ?', 'Quelles garanties possède mon assurance automobile ?', 'Comment déclarer un sinistre ?', 'Quels documents dois-je fournir pour mon sinistre ?', 'Quelle est la différence entre vol et incendie ?', 'Où en est mon dernier sinistre ?']}" var="sugg">
                <button type="button" class="suggestion" data-suggestion>${sugg}</button>
              </c:forEach>
            </div>
            <form class="chat-form" data-chat-form action="${ctx}/client/assistant/message" data-question-initiale="${fn:escapeXml(param.q)}">
              <label class="field">Votre question
                <textarea name="message" rows="2" maxlength="500" required placeholder="Ex. : quels documents dois-je fournir pour mon sinistre ?"></textarea>
              </label>
              <button type="submit" class="btn btn-primary">Envoyer</button>
            </form>
            <span class="xsmall muted">Réponses générées par IA à partir de vos données. Pour une décision sur un dossier, seul votre agent fait foi.</span>
          </div>
        </section>

        <aside class="col-side">
          <section class="card" aria-labelledby="h-ctx">
            <h2 id="h-ctx">Ce que l'assistant consulte</h2>
            <p class="small muted">Uniquement les données de votre compte, envoyées avec votre question.</p>
            <div class="stack small">
              <div class="between"><span>Contrats</span><span class="strong">${nbContrats}</span></div>
              <div class="between"><span>Garanties</span><span class="strong">${nbGaranties}</span></div>
              <div class="between"><span>Sinistres</span><span class="strong">${nbSinistres}</span></div>
              <div class="between"><span>Paiements</span><span class="strong">${nbPaiements}</span></div>
            </div>
          </section>
          <c:if test="${not empty agentReferent}">
            <section class="card dark" aria-labelledby="h-help">
              <h2 id="h-help">Besoin d'un humain ?</h2>
              <p class="small muted">Votre agent <c:out value="${agentReferent.prenom} ${agentReferent.nom}" /> répond sous 48 h ouvrées.</p>
              <a class="btn btn-primary" href="mailto:${fn:escapeXml(agentReferent.email)}">Écrire à mon agent</a>
            </section>
          </c:if>
        </aside>
      </div>
    </div>
  </main>
</div>
<script>window.CSRF_TOKEN = "${fn:escapeXml(sessionScope.csrfToken)}";</script>
<script src="${ctx}/js/app.js"></script>
<script src="${ctx}/js/assistant.js"></script>
</body>
</html>
