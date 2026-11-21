/* ==========================================================================
   Assistant IA – envoie la question à la servlet, qui interroge l'API d'IA
   avec les données du client connecté. Aucune clé d'API côté navigateur.
   Requête  : POST {action du formulaire}  JSON {"message": "..."}
   Réponse  : {"reponse": "..."}  ou  {"erreur": "..."}
   ========================================================================== */
(function () {
  "use strict";

  var form = document.querySelector("[data-chat-form]");
  if (!form) return;
  var body = document.querySelector("[data-chat-body]");
  var textarea = form.querySelector("textarea");
  var bouton = form.querySelector('button[type="submit"]');
  var accueil = body.innerHTML;
  var enCours = false;

  function ajouter(texte, classe) {
    var div = document.createElement("div");
    div.className = "msg " + classe;
    div.textContent = texte; // textContent : pas d'injection HTML possible
    body.appendChild(div);
    body.scrollTop = body.scrollHeight;
    return div;
  }

  function envoyer(question) {
    question = (question || "").trim();
    if (!question || enCours) return;
    enCours = true;
    bouton.disabled = true;
    ajouter(question, "user");
    textarea.value = "";
    var attente = ajouter("L'assistant rédige sa réponse…", "bot typing");

    fetch(form.getAttribute("action"), {
      method: "POST",
      headers: { "Content-Type": "application/json", "Accept": "application/json", "X-CSRF-Token": window.CSRF_TOKEN || "" },
      credentials: "same-origin",
      body: JSON.stringify({ message: question })
    })
      .then(function (r) {
        if (r.status === 401 || r.status === 403) throw new Error("Votre session a expiré. Reconnectez-vous.");
        return r.json().catch(function () { throw new Error("Réponse illisible du serveur."); });
      })
      .then(function (data) {
        attente.remove();
        if (data.erreur) ajouter(data.erreur, "bot error");
        else ajouter(data.reponse || "Je n'ai pas de réponse à cette question.", "bot");
      })
      .catch(function (err) {
        attente.remove();
        ajouter(err.message && err.message.indexOf("Failed") !== 0 ? err.message : "L'assistant est indisponible pour le moment. Réessayez plus tard.", "bot error");
      })
      .finally(function () {
        enCours = false;
        bouton.disabled = false;
        textarea.focus();
      });
  }

  form.addEventListener("submit", function (e) { e.preventDefault(); envoyer(textarea.value); });
  textarea.addEventListener("keydown", function (e) {
    if (e.key === "Enter" && !e.shiftKey) { e.preventDefault(); envoyer(textarea.value); }
  });
  document.querySelectorAll("[data-suggestion]").forEach(function (b) {
    b.addEventListener("click", function () { envoyer(b.textContent); });
  });
  var reset = document.querySelector("[data-chat-reset]");
  if (reset) reset.addEventListener("click", function () { body.innerHTML = accueil; textarea.focus(); });

  // Question passée depuis le tableau de bord (?q=...)
  if (form.dataset.questionInitiale) envoyer(form.dataset.questionInitiale);
})();
