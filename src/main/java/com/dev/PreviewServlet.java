package com.dev;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * SERVLET DE PRÉVISUALISATION — UNIQUEMENT POUR VOIR LE FRONT-END PENDANT LE DÉVELOPPEMENT.
 *
 * Ouvrir http://localhost:8080/<contexte>/preview pour la liste des pages.
 * Elle simule un utilisateur connecté et remplit les vues avec des données fictives.
 *
 * À SUPPRIMER avant le rendu final (elle contourne l'authentification).
 */
@WebServlet("/preview")
public class PreviewServlet extends HttpServlet {

    /* ---------- Enums fictifs (les vrais seront dans le package modèle) ---------- */
    public enum Role { CLIENT, AGENT, ADMIN }
    public enum StatutContrat { EN_ATTENTE, ACTIF, SUSPENDU, EXPIRE, RESILIE }
    public enum StatutSinistre { DECLARE, EN_ANALYSE, ACCEPTE, REFUSE, INDEMNISE }
    public enum StatutPaiement { PAYE, EN_ATTENTE, EN_RETARD }
    public enum ModePaiement { PRELEVEMENT, CARTE, VIREMENT, CHEQUE }

    public enum TypeSinistre {
        DEGATS_EAUX("Dégâts des eaux", "Photos des dommages", "Constat amiable de dégât des eaux (si voisin impliqué)", "Factures de réparation ou devis", "Factures d'achat des biens endommagés"),
        INCENDIE("Incendie", "Photos des dommages", "Rapport des pompiers", "Liste et factures des biens détruits"),
        VOL("Vol", "Récépissé de dépôt de plainte", "Factures des biens volés", "Photos des traces d'effraction"),
        ACCIDENT("Accident", "Constat amiable signé", "Photos du véhicule", "Permis de conduire et carte grise", "Devis du garage");

        private final String libelle;
        private final List<String> documentsRequis;

        TypeSinistre(String libelle, String... docs) {
            this.libelle = libelle;
            this.documentsRequis = Arrays.asList(docs);
        }
        public String getLibelle() { return libelle; }
        public List<String> getDocumentsRequis() { return documentsRequis; }
    }

    /** Pages disponibles : chemin de la vue (sous /WEB-INF/views/) -> titre affiché dans l'index. */
    private static final Map<String, String> PAGES = new LinkedHashMap<>();
    static {
        PAGES.put("auth/login", "Connexion");
        PAGES.put("auth/register", "Inscription");
        PAGES.put("client/dashboard", "Client · Tableau de bord");
        PAGES.put("client/contrats", "Client · Mes contrats");
        PAGES.put("client/contrat-detail", "Client · Détail d'un contrat");
        PAGES.put("client/garanties", "Client · Mes garanties");
        PAGES.put("client/paiements", "Client · Paiements");
        PAGES.put("client/sinistres", "Client · Mes sinistres");
        PAGES.put("client/sinistre-detail", "Client · Suivi d'un sinistre");
        PAGES.put("client/sinistre-form", "Client · Déclarer un sinistre");
        PAGES.put("client/historique", "Client · Historique des demandes");
        PAGES.put("client/assistant", "Client · Assistant IA");
        PAGES.put("client/profil", "Client · Mon profil");
        PAGES.put("agent/clients", "Agent · Clients");
        PAGES.put("agent/contrats", "Agent · Contrats");
        PAGES.put("agent/contrat-form", "Agent · Modifier un contrat");
        PAGES.put("agent/sinistres", "Agent · Traitement des sinistres");
        PAGES.put("agent/garanties", "Agent · Garanties");
        PAGES.put("agent/paiements", "Agent · Paiements");
        PAGES.put("admin/stats", "Admin · Statistiques");
        PAGES.put("admin/utilisateurs", "Admin · Utilisateurs et rôles");
        PAGES.put("admin/types", "Admin · Types d'assurance");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String page = req.getParameter("page");

        // Sans paramètre (ou page inconnue) : liste des pages
        if (page == null || !PAGES.containsKey(page)) {
            afficherIndex(req, resp);
            return;
        }

        HttpSession session = req.getSession();
        session.setAttribute("csrfToken", "preview");

        // Utilisateur simulé selon l'espace demandé
        Map<String, Object> camille = m("id", 1L, "prenom", "Camille", "nom", "Martin", "email", "camille.martin@mail.fr",
                "telephone", "06 12 34 56 78", "adresse", "12 rue des Lilas, 69003 Lyon", "role", Role.CLIENT,
                "dateNaissance", LocalDate.of(1994, 5, 12), "dateInscription", LocalDate.of(2025, 11, 2), "nbContrats", 3, "actif", true);
        Map<String, Object> julien = m("id", 2L, "prenom", "Julien", "nom", "Lefèvre", "email", "j.lefevre@egide.fr", "role", Role.AGENT, "actif", true);
        Map<String, Object> sophie = m("id", 3L, "prenom", "Sophie", "nom", "Arnaud", "email", "s.arnaud@egide.fr", "role", Role.ADMIN, "actif", true);

        if (page.startsWith("auth/")) session.removeAttribute("utilisateur");
        else if (page.startsWith("client/")) session.setAttribute("utilisateur", camille);
        else if (page.startsWith("agent/")) session.setAttribute("utilisateur", julien);
        else session.setAttribute("utilisateur", sophie);

        /* ---------- Données fictives (celles de la maquette) ---------- */
        Map<String, Map<String, Object>> g = new LinkedHashMap<>();
        Object[][] gars = {
                {"Responsabilité civile", "Dommages causés à des tiers", "1000000", "0", 812},
                {"Vol", "Vol ou tentative de vol", "15000", "150", 540},
                {"Incendie", "Incendie, explosion", "300000", "200", 970},
                {"Accident", "Dommages à votre véhicule ou corporels", "20000", "300", 610},
                {"Dégâts des eaux", "Fuites, infiltrations, débordements", "50000", "150", 774},
                {"Assistance", "Dépannage, remorquage, rapatriement", "5000", "0", 0}};
        long gid = 1;
        for (Object[] x : gars) {
            g.put((String) x[0], m("id", gid++, "nom", x[0], "description", x[1], "plafond", new BigDecimal((String) x[2]),
                    "franchise", new BigDecimal((String) x[3]), "nbContrats", x[4]));
        }

        Map<String, Object> taAuto = type(1L, "Assurance automobile", "Véhicules particuliers et deux-roues", "39.90", 992,
                g.get("Responsabilité civile"), g.get("Vol"), g.get("Incendie"), g.get("Accident"), g.get("Assistance"));
        Map<String, Object> taHab = type(2L, "Assurance habitation", "Résidence principale et secondaire", "29.00", 774,
                g.get("Responsabilité civile"), g.get("Incendie"), g.get("Dégâts des eaux"), g.get("Vol"));
        Map<String, Object> taSante = type(3L, "Assurance santé", "Complémentaire santé", "45.00", 363, g.get("Assistance"));
        Map<String, Object> taVoy = type(4L, "Assurance voyage", "Séjours de moins de 90 jours", "12.50", 169, g.get("Assistance"), g.get("Accident"));
        Map<String, Object> taPro = type(5L, "Assurance professionnelle", "RC pro et locaux", "59.00", 0, g.get("Responsabilité civile"), g.get("Incendie"));
        List<Map<String, Object>> types = List.of(taAuto, taHab, taSante, taVoy, taPro);

        Map<String, Object> cAuto = contrat(418L, "CTR-2026-00418", taAuto, "2026-02-01", "2027-01-31", "42.90", StatutContrat.ACTIF, camille,
                g.get("Responsabilité civile"), g.get("Vol"), g.get("Incendie"), g.get("Accident"), g.get("Assistance"));
        Map<String, Object> cHab = contrat(127L, "CTR-2025-00127", taHab, "2025-11-15", "2026-11-14", "38.40", StatutContrat.ACTIF, camille,
                g.get("Responsabilité civile"), g.get("Incendie"), g.get("Dégâts des eaux"));
        Map<String, Object> cVoy = contrat(602L, "CTR-2026-00602", taVoy, "2026-12-20", "2027-01-10", "14.80", StatutContrat.EN_ATTENTE, camille,
                g.get("Assistance"), g.get("Accident"));
        List<Map<String, Object>> contrats = List.of(cAuto, cHab, cVoy);

        List<Map<String, Object>> paiements = List.of(
                paiement("2026-11-15", cHab, "Prélèvement", "38.40", StatutPaiement.EN_ATTENTE),
                paiement("2026-11-01", cAuto, "Prélèvement", "42.90", StatutPaiement.PAYE),
                paiement("2026-10-15", cHab, "Carte bancaire", "38.40", StatutPaiement.PAYE));

        Map<String, Object> sin93 = m("id", 93L, "numero", "SIN-2026-0093", "dateSinistre", LocalDate.of(2026, 10, 27),
                "dateDeclaration", LocalDate.of(2026, 10, 28), "typeSinistre", TypeSinistre.DEGATS_EAUX, "statut", StatutSinistre.EN_ANALYSE,
                "description", "Fuite du chauffe-eau dans la salle de bain pendant la nuit. Parquet du couloir et bas des murs endommagés.",
                "montantEstime", new BigDecimal("1600"), "contrat", cHab, "agent", julien,
                "pieces", List.of(m("id", 1L, "nomFichier", "photo_couloir.jpg"), m("id", 2L, "nomFichier", "facture_plombier.pdf")),
                "commentaires", List.of(
                        m("date", LocalDate.of(2026, 11, 3), "auteur", julien, "texte", "Constat amiable demandé au client"),
                        m("date", LocalDate.of(2026, 10, 29), "auteur", julien, "texte", "Statut passé à EN_ANALYSE")));
        List<Map<String, Object>> sinistres = new ArrayList<>();
        sinistres.add(sin93);
        sinistres.add(sinistre(91L, "SIN-2026-0091", "Karim", "Benali", taAuto, TypeSinistre.ACCIDENT, "2026-10-26", "3200"));
        sinistres.add(sinistre(88L, "SIN-2026-0088", "Léa", "Dubois", taHab, TypeSinistre.VOL, "2026-10-22", "850"));
        sinistres.add(sinistre(85L, "SIN-2026-0085", "Thomas", "Girard", taHab, TypeSinistre.INCENDIE, "2026-10-19", "12400"));

        /* ---------- Attributs selon la page ---------- */
        switch (page) {
            case "client/dashboard":
                set(req, "contrats", contrats, "nbContrats", 3, "nbContratsActifs", 2, "nbContratsEnAttente", 1,
                        "totalPrimes", new BigDecimal("81.30"), "nbSinistresEnCours", 1, "dernierSinistre", sin93,
                        "prochaineEcheance", paiements.get(0), "derniersPaiements", paiements);
                break;
            case "client/contrats":
                set(req, "contrats", contrats, "statuts", StatutContrat.values());
                break;
            case "client/contrat-detail":
                set(req, "contrat", cAuto, "paiements", paiements.subList(1, 2), "sinistres", List.of());
                break;
            case "client/garanties":
                set(req, "contrats", contrats);
                break;
            case "client/paiements":
                set(req, "paiements", paiements, "totalPaye", new BigDecimal("968.40"), "totalEnAttente", new BigDecimal("38.40"),
                        "prochaineEcheance", paiements.get(0));
                break;
            case "client/sinistres":
                set(req, "sinistres", List.of(sin93));
                break;
            case "client/sinistre-detail":
                set(req, "sinistre", sin93);
                break;
            case "client/sinistre-form":
                set(req, "contratsActifs", List.of(cHab, cAuto), "typesSinistre", TypeSinistre.values());
                break;
            case "client/historique":
                set(req, "demandes", List.of(
                        m("date", LocalDate.of(2026, 10, 28), "type", "Déclaration de sinistre", "reference", "SIN-2026-0093",
                                "lien", "/client/sinistres/detail?id=93", "statut", StatutSinistre.EN_ANALYSE),
                        m("date", LocalDate.of(2026, 9, 2), "type", "Souscription", "reference", "CTR-2026-00602", "statut", StatutContrat.EN_ATTENTE)));
                break;
            case "client/assistant":
                set(req, "nbContrats", 3, "nbGaranties", 10, "nbSinistres", 1, "nbPaiements", 24, "agentReferent", julien);
                break;
            case "client/profil":
                set(req, "client", camille);
                break;
            case "agent/clients":
                Map<String, Object> selection = new LinkedHashMap<>(camille);
                selection.put("contrats", contrats);
                selection.put("sinistres", List.of(sin93));
                set(req, "clients", List.of(camille, m("id", 5L, "prenom", "Karim", "nom", "Benali", "email", "k.benali@mail.fr",
                        "telephone", "07 11 22 33 44", "dateInscription", LocalDate.of(2026, 3, 10), "nbContrats", 1)), "selection", selection);
                break;
            case "agent/contrats":
                set(req, "contrats", contrats, "statuts", StatutContrat.values(), "typesAssurance", types);
                break;
            case "agent/contrat-form":
                set(req, "contrat", cHab, "clients", List.of(camille), "typesAssurance", types,
                        "garanties", new ArrayList<>(g.values()), "statuts", StatutContrat.values());
                break;
            case "agent/sinistres":
                set(req, "sinistres", sinistres, "statuts", StatutSinistre.values(), "selection", sin93, "nbSinistresATraiter", 7,
                        "compteurs", m("TOUS", 24, "DECLARE", 3, "EN_ANALYSE", 4, "ACCEPTE", 5, "REFUSE", 2, "INDEMNISE", 10),
                        "analyseIA", "Garantie « Dégâts des eaux » active au 27/10. Montant estimé cohérent avec les factures. Constat amiable manquant.");
                break;
            case "agent/garanties":
                set(req, "garanties", new ArrayList<>(g.values()));
                break;
            case "agent/paiements":
                set(req, "paiements", paiements, "contratsActifs", List.of(cAuto, cHab),
                        "modesPaiement", ModePaiement.values(), "statutsPaiement", StatutPaiement.values());
                break;
            case "admin/stats":
                remplirStats(req);
                break;
            case "admin/utilisateurs":
                set(req, "utilisateurs", List.of(camille, julien, sophie,
                                m("id", 8L, "prenom", "Paul", "nom", "Mercier", "email", "p.mercier@mail.fr", "role", Role.CLIENT, "actif", false)),
                        "roles", Role.values());
                break;
            case "admin/types":
                set(req, "types", types, "garanties", new ArrayList<>(g.values()));
                break;
            default:
                break;
        }

        req.getRequestDispatcher("/WEB-INF/views/" + page + ".jsp").forward(req, resp);
    }

    private void remplirStats(HttpServletRequest req) {
        String[] mois = {"déc.", "janv.", "févr.", "mars", "avr.", "mai", "juin", "juil.", "août", "sept.", "oct.", "nov."};
        int[] primes = {142, 148, 145, 151, 150, 153, 155, 152, 149, 157, 160, 162};
        int[] indem = {70, 88, 76, 64, 71, 80, 69, 74, 62, 78, 104, 61};
        int[] nb = {2890, 2910, 2935, 2950, 2972, 2990, 3010, 3025, 3040, 3060, 3082, 3105};
        List<Map<String, Object>> evolution = new ArrayList<>();
        for (int i = 0; i < mois.length; i++) {
            evolution.add(m("mois", mois[i], "primes", primes[i] * 1000, "indemnisations", indem[i] * 1000, "nbContrats", nb[i]));
        }
        set(req, "nbClients", 2418L, "nbNouveauxClients", 64L, "nbContratsActifs", 3105L, "nbContratsExpires", 212L, "nbContratsResilies", 87L,
                "chiffreAffaires", new BigDecimal("1840000"), "montantPrimes", new BigDecimal("1792000"), "montantIndemnisations", new BigDecimal("970000"),
                "ratioSinistresPrimes", 0.53, "delaiMoyenTraitement", 6.4, "tauxAcceptation", 0.78,
                "evolution", evolution,
                "clientsParType", m("Automobile", 992L, "Habitation", 774L, "Santé", 363L, "Voyage", 169L, "Professionnelle", 120L),
                "maxClientsParType", 992L,
                "contratsParStatut", m("ACTIF", 3105L, "EN_ATTENTE", 96L, "SUSPENDU", 41L, "EXPIRE", 212L, "RESILIE", 87L),
                "analyseIA", List.of(
                        "Les indemnisations d'octobre augmentent nettement, portées par les dégâts des eaux en habitation.",
                        "L'assurance automobile reste le premier type (41 % des clients).",
                        "14 contrats expirent dans les 30 jours : une relance de renouvellement est conseillée."));
    }

    private void afficherIndex(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/html;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        String ctx = req.getContextPath();
        out.println("<!DOCTYPE html><html lang='fr'><head><meta charset='UTF-8'><title>Prévisualisation</title>");
        out.println("<link rel='stylesheet' href='" + ctx + "/css/style.css'></head><body><main class='main'><div class='container'>");
        out.println("<h1>Prévisualisation du front-end</h1><p class='muted'>Données fictives · servlet à supprimer avant le rendu.</p>");
        out.println("<div class='card stack'>");
        for (Map.Entry<String, String> e : PAGES.entrySet()) {
            out.println("<a href='" + ctx + "/preview?page=" + e.getKey() + "'>" + e.getValue() + "</a>");
        }
        out.println("</div></div></main></body></html>");
    }

    /* ---------- Petits utilitaires ---------- */

    /** Crée une Map ordonnée à partir de paires clé/valeur : m("nom", "Martin", "prenom", "Camille"). */
    private static Map<String, Object> m(Object... kv) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) map.put((String) kv[i], kv[i + 1]);
        return map;
    }

    private static void set(HttpServletRequest req, Object... kv) {
        for (int i = 0; i < kv.length; i += 2) req.setAttribute((String) kv[i], kv[i + 1]);
    }

    @SafeVarargs
    private static Map<String, Object> type(Long id, String nom, String desc, String prix, int nbContrats, Map<String, Object>... gars) {
        return m("id", id, "nom", nom, "description", desc, "prixBase", new BigDecimal(prix), "nbContrats", nbContrats, "garanties", List.of(gars));
    }

    @SafeVarargs
    private static Map<String, Object> contrat(Long id, String numero, Map<String, Object> type, String debut, String fin, String prime,
                                               StatutContrat statut, Map<String, Object> client, Map<String, Object>... gars) {
        return m("id", id, "numero", numero, "typeAssurance", type, "dateDebut", LocalDate.parse(debut), "dateFin", LocalDate.parse(fin),
                "prime", new BigDecimal(prime), "statut", statut, "client", client, "garanties", List.of(gars));
    }

    private static Map<String, Object> paiement(String date, Map<String, Object> contrat, String mode, String montant, StatutPaiement statut) {
        return m("datePaiement", LocalDate.parse(date), "contrat", contrat, "modePaiement", mode, "montant", new BigDecimal(montant), "statut", statut);
    }

    private static Map<String, Object> sinistre(Long id, String numero, String prenom, String nom, Map<String, Object> type,
                                                TypeSinistre ts, String date, String montant) {
        Map<String, Object> client = m("prenom", prenom, "nom", nom);
        Map<String, Object> contrat = m("numero", "CTR-2026-00" + id, "client", client, "typeAssurance", type);
        return m("id", id, "numero", numero, "contrat", contrat, "typeSinistre", ts, "dateDeclaration", LocalDate.parse(date),
                "montantEstime", new BigDecimal(montant), "statut", StatutSinistre.EN_ANALYSE);
    }
}