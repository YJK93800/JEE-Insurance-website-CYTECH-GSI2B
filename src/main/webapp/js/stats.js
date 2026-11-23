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
  var STATUS_COLORS = { ACTIF: "#17603F", EN_ATTENTE: "#C9852B", SUSPENDU: "#6A4FC0", EXPIRE: "#8792A2", RESILIE: "#C23B3B" };
  var formatEuros = function (v) { return Number(v).toLocaleString("fr-FR", { style: "currency", currency: "EUR", maximumFractionDigits: 0 }); };

  Chart.defaults.font.family = "Inter, system-ui, sans-serif";
  Chart.defaults.color = TEXT;
  Chart.defaults.maintainAspectRatio = false;
  var axes = function (fmt) {
    return {
      x: { grid: { display: false } },
      y: { beginAtZero: true, grid: { color: GRID }, ticks: { callback: fmt || undefined } }
    };
  };

  var cRev = document.getElementById("chart-revenue");
  if (cRev) {
    new Chart(cRev, {
      type: "bar",
      data: {
        labels: d.months,
        datasets: [
          { label: "Primes encaissées", data: d.premiums, backgroundColor: TEAL, borderRadius: 3, maxBarThickness: 18 },
          { label: "Indemnisations", data: d.indemnities, backgroundColor: ORANGE, borderRadius: 3, maxBarThickness: 18 }
        ]
      },
      options: {
        scales: axes(formatEuros),
        plugins: { legend: { position: "top", align: "end" }, tooltip: { callbacks: { label: function (c) { return c.dataset.label + " : " + formatEuros(c.parsed.y); } } } }
      }
    });
  }

  var cCon = document.getElementById("chart-contracts");
  if (cCon) {
    new Chart(cCon, {
      type: "line",
      data: { labels: d.months, datasets: [{ label: "Contrats", data: d.contracts, borderColor: TEAL, backgroundColor: "rgba(15,110,110,0.08)", fill: true, tension: 0.3, pointRadius: 3 }] },
      options: { scales: axes(), plugins: { legend: { display: false } } }
    });
  }

  var cSta = document.getElementById("chart-statuses");
  if (cSta && d.statuses) {
    var keys = Object.keys(d.statuses);
    new Chart(cSta, {
      type: "doughnut",
      data: {
        labels: keys.map(function (k) { return k === "RESILIE" ? "RÉSILIÉ" : k; }),
        datasets: [{ data: keys.map(function (k) { return d.statuses[k]; }), backgroundColor: keys.map(function (k) { return STATUS_COLORS[k] || "#8792A2"; }), borderWidth: 2, borderColor: "#FFFFFF" }]
      },
      options: { cutout: "62%", plugins: { legend: { position: "bottom", labels: { boxWidth: 10 } } } }
    });
  }
})();
