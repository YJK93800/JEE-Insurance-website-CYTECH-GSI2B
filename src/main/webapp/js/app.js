/* ==========================================================================
   Égide – comportements communs à toutes les pages.
   Rappel : cette validation sert uniquement le confort de l'utilisateur.
   Les servlets revérifient TOUTES les données avant l'enregistrement en base.
   ========================================================================== */
(function () {
  "use strict";

  /* ---------- Validation des formulaires [data-validate] ---------- */

  function fieldOf(input) {
    return input.closest(".field") || input.closest("fieldset") || input.parentElement;
  }

  function setError(input, message) {
    var field = fieldOf(input);
    if (!field) return;
    // Réutilise le message renvoyé par la servlet s'il existe, pour ne jamais afficher deux erreurs
    var err = field.querySelector(".field-error");
    if (message) {
      field.classList.add("has-error");
      input.setAttribute("aria-invalid", "true");
      if (!err) {
        err = document.createElement("span");
        err.className = "field-error js";
        field.appendChild(err);
      }
      if (!err.id) err.id = "err-" + (input.name || Math.random().toString(36).slice(2));
      err.textContent = message;
      input.setAttribute("aria-describedby", err.id);
    } else {
      if (err) err.remove();
      field.classList.remove("has-error");
      input.removeAttribute("aria-invalid");
    }
  }

  function today() {
    var d = new Date();
    return d.getFullYear() + "-" + String(d.getMonth() + 1).padStart(2, "0") + "-" + String(d.getDate()).padStart(2, "0");
  }

  function checkInput(input, form) {
    if (input.disabled || input.type === "hidden") return null;
    var v = (input.value || "").trim();

    if (input.type === "radio" || input.type === "checkbox") {
      if (input.required && !form.querySelector('[name="' + input.name + '"]:checked')) return "Veuillez faire un choix.";
      return null;
    }
    if (input.type === "file") {
      var max = parseInt(input.dataset.maxSize || "0", 10);
      var maxFiles = parseInt(input.dataset.maxFiles || "0", 10);
      if (input.required && input.files.length === 0) return "Ajoutez au moins un fichier.";
      if (maxFiles && input.files.length > maxFiles) return maxFiles + " fichiers maximum.";
      for (var i = 0; i < input.files.length; i++) {
        if (max && input.files[i].size > max) return "« " + input.files[i].name + " » dépasse " + Math.round(max / 1048576) + " Mo.";
      }
      return null;
    }
    if (input.required && v === "") return "Ce champ est obligatoire.";
    if (v === "") return null;

    if (input.type === "email" && !/^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(v)) return "Adresse e-mail invalide.";
    if (input.type === "number") {
      var n = Number(v.replace(",", "."));
      if (isNaN(n)) return "Saisissez un nombre.";
      if (input.min !== "" && n < Number(input.min)) return Number(input.min) > 0 ? "Le montant doit être un nombre positif." : "La valeur doit être supérieure ou égale à " + input.min + ".";
      if (input.max !== "" && n > Number(input.max)) return "La valeur doit être inférieure ou égale à " + input.max + ".";
    }
    if (input.minLength > 0 && v.length < input.minLength) return input.minLength + " caractères minimum.";
    if (input.pattern && !new RegExp("^(?:" + input.pattern + ")$").test(v)) return "Format invalide.";
    if (input.name === "motDePasse" && input.minLength > 0 && !/\d/.test(v)) return "Le mot de passe doit contenir au moins un chiffre.";
    if (input.dataset.match) {
      var other = form.querySelector('[name="' + input.dataset.match + '"]');
      if (other && other.value !== input.value) return "Les mots de passe ne correspondent pas.";
    }
    if (input.hasAttribute("data-max-today") && v > today()) return "La date ne peut pas être dans le futur.";
    if (input.dataset.after) {
      var start = form.querySelector('[name="' + input.dataset.after + '"]');
      if (start && start.value && v <= start.value) return "Doit être postérieure à la date de début.";
    }
    return null;
  }

  document.querySelectorAll("form[data-validate]").forEach(function (form) {
    form.addEventListener("submit", function (e) {
      var first = null;
      var seenGroups = {};
      form.querySelectorAll("input, select, textarea").forEach(function (input) {
        if ((input.type === "radio" || input.type === "checkbox") && input.required) {
          if (seenGroups[input.name]) return;
          seenGroups[input.name] = true;
        }
        var msg = checkInput(input, form);
        setError(input, msg);
        if (msg && !first) first = input;
      });
      if (first) {
        e.preventDefault();
        first.focus();
      }
    });
    form.addEventListener("input", function (e) {
      if (e.target.getAttribute("aria-invalid") === "true") setError(e.target, checkInput(e.target, form));
    });
  });

  /* ---------- Confirmation avant une action destructive ---------- */
  document.querySelectorAll("form[data-confirm]").forEach(function (form) {
    form.addEventListener("submit", function (e) {
      if (!window.confirm(form.dataset.confirm)) e.preventDefault();
    });
  });

  /* ---------- Liste déroulante qui soumet son formulaire ---------- */
  document.querySelectorAll("select[data-autosubmit]").forEach(function (sel) {
    sel.addEventListener("change", function () { sel.form.submit(); });
  });

  /* ---------- Compteur de caractères ---------- */
  document.querySelectorAll("textarea[data-counter]").forEach(function (ta) {
    var out = document.getElementById(ta.dataset.counter);
    function upd() { if (out) out.textContent = ta.value.length + " / " + (ta.maxLength > 0 ? ta.maxLength : "∞") + " caractères"; }
    ta.addEventListener("input", upd);
    upd();
  });

  /* ---------- Déclaration de sinistre ---------- */
  var formSin = document.getElementById("form-sinistre");
  if (formSin) {
    var docsList = document.querySelector("[data-docs-list]");
    var recapContrat = document.querySelector('[data-recap="contrat"]');
    var recapDate = document.querySelector('[data-recap="date"]');
    var recapPieces = document.querySelector('[data-recap="pieces"]');

    var majType = function () {
      var t = formSin.querySelector('input[name="typeSinistre"]:checked');
      document.querySelectorAll("[data-type-libelle]").forEach(function (el) { el.textContent = t ? t.dataset.libelle : "—"; });
      if (docsList) {
        docsList.innerHTML = "";
        (t && t.dataset.docs ? t.dataset.docs.split("|") : []).forEach(function (d) {
          var li = document.createElement("li");
          li.textContent = d;
          docsList.appendChild(li);
        });
      }
    };
    var majContrat = function () {
      var c = formSin.querySelector('input[name="contratId"]:checked');
      if (recapContrat) recapContrat.textContent = c ? c.dataset.numero : "—";
    };
    var champDate = formSin.querySelector('input[name="dateSinistre"]');
    var majDate = function () {
      if (!recapDate || !champDate) return;
      var v = champDate.value;
      recapDate.textContent = v ? v.split("-").reverse().join("/") : "—";
    };
    if (champDate) champDate.max = today();

    formSin.addEventListener("change", function (e) {
      if (e.target.name === "typeSinistre") majType();
      if (e.target.name === "contratId") majContrat();
      if (e.target.name === "dateSinistre") majDate();
    });
    majType(); majContrat(); majDate();

    /* Pièces justificatives : liste, retrait, glisser-déposer */
    var input = formSin.querySelector('input[type="file"][name="pieces"]');
    var zone = formSin.querySelector("[data-dropzone]");
    var liste = formSin.querySelector("[data-file-list]");
    var fichiers = [];

    var taille = function (o) { return o > 1048576 ? (o / 1048576).toFixed(1).replace(".", ",") + " Mo" : Math.round(o / 1024) + " Ko"; };
    var synchroniser = function () {
      var dt = new DataTransfer();
      fichiers.forEach(function (f) { dt.items.add(f); });
      input.files = dt.files;
      liste.innerHTML = "";
      fichiers.forEach(function (f, i) {
        var li = document.createElement("li");
        var nom = document.createElement("span");
        var strong = document.createElement("strong");
        strong.textContent = f.name;
        nom.appendChild(strong);
        nom.appendChild(document.createTextNode(" · " + taille(f.size)));
        var btn = document.createElement("button");
        btn.type = "button";
        btn.className = "btn-link danger";
        btn.textContent = "Retirer";
        btn.setAttribute("aria-label", "Retirer " + f.name);
        btn.addEventListener("click", function () { fichiers.splice(i, 1); synchroniser(); });
        li.appendChild(nom);
        li.appendChild(btn);
        liste.appendChild(li);
      });
      if (recapPieces) recapPieces.textContent = fichiers.length;
      setError(input, checkInput(input, formSin));
    };
    var ajouter = function (list) {
      Array.prototype.forEach.call(list, function (f) {
        if (!fichiers.some(function (x) { return x.name === f.name && x.size === f.size; })) fichiers.push(f);
      });
      synchroniser();
    };
    if (input && liste) {
      input.addEventListener("change", function () { ajouter(input.files); });
      ["dragenter", "dragover"].forEach(function (ev) {
        zone.addEventListener(ev, function (e) { e.preventDefault(); zone.classList.add("drag"); });
      });
      ["dragleave", "drop"].forEach(function (ev) {
        zone.addEventListener(ev, function (e) { e.preventDefault(); zone.classList.remove("drag"); });
      });
      zone.addEventListener("drop", function (e) { ajouter(e.dataTransfer.files); });
    }
  }

  /* ---------- Formulaire de contrat : prix de base et garanties du type ---------- */
  var selType = document.querySelector("select[data-type-assurance]");
  if (selType) {
    var prime = document.querySelector("input[data-prime]");
    var majGaranties = function (preRemplir) {
      var opt = selType.options[selType.selectedIndex];
      var dispo = opt && opt.dataset.garanties ? opt.dataset.garanties.split(",") : [];
      var aucuneCochee = !document.querySelector('input[name="garantieIds"]:checked');
      document.querySelectorAll("[data-garantie]").forEach(function (lab) {
        var ok = dispo.indexOf(lab.dataset.garantie) !== -1;
        lab.style.opacity = dispo.length && !ok ? "0.55" : "1";
        if (preRemplir && aucuneCochee && ok) lab.querySelector("input").checked = true;
      });
      if (preRemplir && prime && !prime.value && opt && opt.dataset.prix) prime.value = opt.dataset.prix;
    };
    selType.addEventListener("change", function () { majGaranties(true); });
    majGaranties(false);
  }

  /* ---------- Enregistrement d'un paiement : montant = prime du contrat ---------- */
  var selContrat = document.querySelector("select[data-contrat-prime]");
  if (selContrat) {
    selContrat.addEventListener("change", function () {
      var cible = document.querySelector("input[data-prime-cible]");
      var opt = selContrat.options[selContrat.selectedIndex];
      if (cible && !cible.value && opt.dataset.prime) cible.value = opt.dataset.prime;
    });
  }

  /* ---------- Décision sur un sinistre : indemnisation obligatoire si INDEMNISE ---------- */
  var selStatut = document.querySelector("select[data-toggle-indemnisation]");
  if (selStatut) {
    var montant = document.querySelector("input[data-indemnisation]");
    var maj = function () { if (montant) montant.required = selStatut.value === "INDEMNISE"; };
    selStatut.addEventListener("change", maj);
    maj();
  }
})();
