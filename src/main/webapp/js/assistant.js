/* ==========================================================================
   Assistant IA – envoie la question à la servlet, qui interroge l'API d'IA
   avec les données du client connecté. Aucune clé d'API côté navigateur.
   Requête  : POST {action du formulaire}  JSON {"message": "..."}
   Réponse  : {"reply": "..."}  ou  {"error": "..."}
   ========================================================================== */
(function () {
  "use strict";

  var form = document.querySelector("[data-chat-form]");
  if (!form) return;
  var body = document.querySelector("[data-chat-body]");
  var textarea = form.querySelector("textarea");
  var button = form.querySelector('button[type="submit"]');
  var welcome = body.innerHTML;
  var busy = false;

  function addMessage(texte, classe) {
    var div = document.createElement("div");
    div.className = "msg " + classe;
    div.textContent = texte; // textContent : pas d'injection HTML possible
    body.appendChild(div);
    body.scrollTop = body.scrollHeight;
    return div;
  }

  function send(question) {
    question = (question || "").trim();
    if (!question || busy) return;
    busy = true;
    button.disabled = true;
    addMessage(question, "user");
    textarea.value = "";
    var pending = addMessage("L'assistant rédige sa réponse…", "bot typing");

    // Filet de sécurité : le serveur coupe à 25 s, le navigateur abandonne à 35 s.
    var controller = new AbortController();
    var timer = setTimeout(function () { controller.abort(); }, 35000);

    fetch(form.getAttribute("action"), {
      method: "POST",
      signal: controller.signal,
      headers: { "Content-Type": "application/json", "Accept": "application/json", "X-CSRF-Token": window.CSRF_TOKEN || "" },
      credentials: "same-origin",
      body: JSON.stringify({ message: question })
    })
      .then(function (r) {
        if (r.status === 401 || r.status === 403) throw new Error("Votre session a expiré. Reconnectez-vous.");
        return r.json().catch(function () { throw new Error("Réponse illisible du serveur."); });
      })
      .then(function (data) {
        pending.remove();
        if (data.error) addMessage(data.error, "bot error");
        else addMessage(data.reply || "Je n'ai pas de réponse à cette question.", "bot");
      })
      .catch(function (err) {
        pending.remove();
        var msg;
        if (err.name === "AbortError") msg = "La réponse prend trop de temps. Réessayez dans un instant.";
        else if (err instanceof TypeError) msg = "Impossible de joindre le serveur. Vérifiez votre connexion.";
        else msg = err.message || "L'assistant est indisponible pour le moment. Réessayez plus tard.";
        addMessage(msg, "bot error");
      })
      .finally(function () {
        clearTimeout(timer);
        busy = false;
        button.disabled = false;
        textarea.focus();
      });
  }

  form.addEventListener("submit", function (e) { e.preventDefault(); send(textarea.value); });
  textarea.addEventListener("keydown", function (e) {
    if (e.key === "Enter" && !e.shiftKey) { e.preventDefault(); send(textarea.value); }
  });
  document.querySelectorAll("[data-suggestion]").forEach(function (b) {
    b.addEventListener("click", function () { send(b.textContent); });
  });
  var reset = document.querySelector("[data-chat-reset]");
  if (reset) reset.addEventListener("click", function () { body.innerHTML = welcome; textarea.focus(); });

  // Question passée depuis le tableau de bord (?q=...)
  if (form.dataset.initialQuestion) send(form.dataset.initialQuestion);
})();
