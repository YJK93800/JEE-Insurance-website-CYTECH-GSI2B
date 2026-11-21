/* ==========================================================================
   Graphiques du tableau de bord (Chart.js). Les données sont écrites par la
   JSP dans <script type="application/json" id="stats-data">.
   ========================================================================== */
(function () {
  "use strict";
  var el = document.getElementById("stats-data");
  if (!el || typeof Chart === "undefined") return;

  var d;
  try { d = JSON.parse(el.textContent); } catch (e) { console.error("Données de statistiques invalides", e); return; }

  var TEAL = "#0F6E6E", ORANGE = "#E8913A", GRID = "#EDF0F3", TEXT = "#5B6675";
  var COULEURS_STATUT = { ACTIF: "#17603F", EN_ATTENTE: "#C9852B", SUSPENDU: "#6A4FC0", EXPIRE: "#8792A2", RESILIE: "#C23B3B" };
  var euros = function (v) { return Number(v).toLocaleString("fr-FR", { style: "currency", currency: "EUR", maximumFractionDigits: 0 }); };

  Chart.defaults.font.family = "Inter, system-ui, sans-serif";
  Chart.defaults.color = TEXT;
  Chart.defaults.maintainAspectRatio = false;
  var axes = function (fmt) {
    return {
      x: { grid: { display: false } },
      y: { beginAtZero: true, grid: { color: GRID }, ticks: { callback: fmt || undefined } }
    };
  };

  var cRev = document.getElementById("chart-revenus");
  if (cRev) {
    new Chart(cRev, {
      type: "bar",
      data: {
        labels: d.mois,
        datasets: [
          { label: "Primes encaissées", data: d.primes, backgroundColor: TEAL, borderRadius: 3, maxBarThickness: 18 },
          { label: "Indemnisations", data: d.indemnisations, backgroundColor: ORANGE, borderRadius: 3, maxBarThickness: 18 }
        ]
      },
      options: {
        scales: axes(euros),
        plugins: { legend: { position: "top", align: "end" }, tooltip: { callbacks: { label: function (c) { return c.dataset.label + " : " + euros(c.parsed.y); } } } }
      }
    });
  }

  var cCon = document.getElementById("chart-contrats");
  if (cCon) {
    new Chart(cCon, {
      type: "line",
      data: { labels: d.mois, datasets: [{ label: "Contrats", data: d.contrats, borderColor: TEAL, backgroundColor: "rgba(15,110,110,0.08)", fill: true, tension: 0.3, pointRadius: 3 }] },
      options: { scales: axes(), plugins: { legend: { display: false } } }
    });
  }

  var cSta = document.getElementById("chart-statuts");
  if (cSta && d.statuts) {
    var cles = Object.keys(d.statuts);
    new Chart(cSta, {
      type: "doughnut",
      data: {
        labels: cles.map(function (k) { return k === "RESILIE" ? "RÉSILIÉ" : k; }),
        datasets: [{ data: cles.map(function (k) { return d.statuts[k]; }), backgroundColor: cles.map(function (k) { return COULEURS_STATUT[k] || "#8792A2"; }), borderWidth: 2, borderColor: "#FFFFFF" }]
      },
      options: { cutout: "62%", plugins: { legend: { position: "bottom", labels: { boxWidth: 10 } } } }
    });
  }
})();
