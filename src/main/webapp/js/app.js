/* ==========================================================================
   AssurCY2000 – comportements communs à toutes les pages.
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
  var claimForm = document.getElementById("claim-form");
  if (claimForm) {
    var docsList = document.querySelector("[data-docs-list]");
    var recapContract = document.querySelector('[data-recap="contract"]');
    var recapDate = document.querySelector('[data-recap="date"]');
    var recapFiles = document.querySelector('[data-recap="files"]');

    var updateType = function () {
      var t = claimForm.querySelector('input[name="type"]:checked');
      document.querySelectorAll("[data-type-label]").forEach(function (el) { el.textContent = t ? t.dataset.label : "—"; });
      if (docsList) {
        docsList.innerHTML = "";
        (t && t.dataset.docs ? t.dataset.docs.split("|") : []).forEach(function (d) {
          var li = document.createElement("li");
          li.textContent = d;
          docsList.appendChild(li);
        });
      }
    };
    var updateContract = function () {
      var c = claimForm.querySelector('input[name="contractId"]:checked');
      if (recapContract) recapContract.textContent = c ? c.dataset.number : "—";
    };
    var dateInput = claimForm.querySelector('input[name="claimDate"]');
    var updateDate = function () {
      if (!recapDate || !dateInput) return;
      var v = dateInput.value;
      recapDate.textContent = v ? v.split("-").reverse().join("/") : "—";
    };
    if (dateInput) dateInput.max = today();

    claimForm.addEventListener("change", function (e) {
      if (e.target.name === "type") updateType();
      if (e.target.name === "contractId") updateContract();
      if (e.target.name === "claimDate") updateDate();
    });
    updateType(); updateContract(); updateDate();

    /* Pièces justificatives : liste, retrait, glisser-déposer */
    var input = claimForm.querySelector('input[type="file"][name="attachments"]');
    var dropZone = claimForm.querySelector("[data-dropzone]");
    var fileList = claimForm.querySelector("[data-file-list]");
    var files = [];

    var formatSize = function (o) { return o > 1048576 ? (o / 1048576).toFixed(1).replace(".", ",") + " Mo" : Math.round(o / 1024) + " Ko"; };
    var syncFiles = function () {
      var dt = new DataTransfer();
      files.forEach(function (f) { dt.items.add(f); });
      input.files = dt.files;
      fileList.innerHTML = "";
      files.forEach(function (f, i) {
        var li = document.createElement("li");
        var nameEl = document.createElement("span");
        var strong = document.createElement("strong");
        strong.textContent = f.name;
        nameEl.appendChild(strong);
        nameEl.appendChild(document.createTextNode(" · " + formatSize(f.size)));
        var btn = document.createElement("button");
        btn.type = "button";
        btn.className = "btn-link danger";
        btn.textContent = "Retirer";
        btn.setAttribute("aria-label", "Retirer " + f.name);
        btn.addEventListener("click", function () { files.splice(i, 1); syncFiles(); });
        li.appendChild(nameEl);
        li.appendChild(btn);
        fileList.appendChild(li);
      });
      if (recapFiles) recapFiles.textContent = files.length;
      setError(input, checkInput(input, claimForm));
    };
    var addFiles = function (list) {
      Array.prototype.forEach.call(list, function (f) {
        if (!files.some(function (x) { return x.name === f.name && x.size === f.size; })) files.push(f);
      });
      syncFiles();
    };
    if (input && fileList) {
      input.addEventListener("change", function () { addFiles(input.files); });
      ["dragenter", "dragover"].forEach(function (ev) {
        dropZone.addEventListener(ev, function (e) { e.preventDefault(); dropZone.classList.add("drag"); });
      });
      ["dragleave", "drop"].forEach(function (ev) {
        dropZone.addEventListener(ev, function (e) { e.preventDefault(); dropZone.classList.remove("drag"); });
      });
      dropZone.addEventListener("drop", function (e) { addFiles(e.dataTransfer.files); });
    }
  }

  /* ---------- Formulaire de contrat : prix de base et garanties du type ---------- */
  var selType = document.querySelector("select[data-insurance-type]");
  if (selType) {
    var premium = document.querySelector("input[data-premium]");
    var updateGuarantees = function (prefill) {
      var opt = selType.options[selType.selectedIndex];
      var available = opt && opt.dataset.guarantees ? opt.dataset.guarantees.split(",") : [];
      var noneChecked = !document.querySelector('input[name="guaranteeIds"]:checked');
      document.querySelectorAll("[data-guarantee]").forEach(function (lab) {
        var ok = available.indexOf(lab.dataset.guarantee) !== -1;
        lab.style.opacity = available.length && !ok ? "0.55" : "1";
        if (prefill && noneChecked && ok) lab.querySelector("input").checked = true;
      });
      if (prefill && premium && !premium.value && opt && opt.dataset.price) premium.value = opt.dataset.price;
    };
    selType.addEventListener("change", function () { updateGuarantees(true); });
    updateGuarantees(false);
  }

  /* ---------- Formulaire de contrat : date de fin = début + 1 an - 1 jour ---------- */
  /* Mise à jour tant que l'agent n'a pas modifié la date de fin lui-même. */
  var autoEnd = document.querySelector("input[data-auto-end]");
  var startInput = document.querySelector('input[name="startDate"]');
  if (autoEnd && startInput) {
    var endTouched = false;
    autoEnd.addEventListener("input", function () { endTouched = true; });
    startInput.addEventListener("change", function () {
      if (endTouched || !startInput.value) return;
      var d = new Date(startInput.value + "T00:00:00");
      d.setFullYear(d.getFullYear() + 1);
      d.setDate(d.getDate() - 1);
      var p = function (n) { return (n < 10 ? "0" : "") + n; };
      autoEnd.value = d.getFullYear() + "-" + p(d.getMonth() + 1) + "-" + p(d.getDate());
    });
  }

  /* ---------- Décision sur un sinistre : indemnisation obligatoire si INDEMNISE ---------- */
  var selStatus = document.querySelector("select[data-toggle-indemnity]");
  if (selStatus) {
    var amount = document.querySelector("input[data-indemnity]");
    var update = function () { if (amount) amount.required = selStatus.value === "INDEMNISE"; };
    selStatus.addEventListener("change", update);
    update();
  }
})();
