package com.assurcy2000.util;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

/**
 * Réaffiche un formulaire avec ses erreurs.
 * Les noms des attributs doivent correspondre aux JSP :
 *  - "errors"        : Map champ -> message, lue par le tag <ui:champ>
 *  - "erreurGlobale" : message général, lu par flash.jspf
 * Les valeurs saisies sont réaffichées automatiquement par <ui:champ> (via param[nom]).
 */
public final class FormView {

    public static final String ERRORS = "errors";
    public static final String GLOBAL_ERROR = "erreurGlobale";

    private FormView() {}

    /** Erreurs par champ uniquement. */
    public static void show(HttpServletRequest req, HttpServletResponse resp,
                            String view, Map<String, String> errors)
            throws ServletException, IOException {
        show(req, resp, view, errors, null);
    }

    /** Erreurs par champ + message général (globalError peut être null). */
    public static void show(HttpServletRequest req, HttpServletResponse resp,
                            String view, Map<String, String> errors, String globalError)
            throws ServletException, IOException {
        req.setAttribute(ERRORS, errors);
        if (globalError != null) {
            req.setAttribute(GLOBAL_ERROR, globalError);
        }
        req.getRequestDispatcher(view).forward(req, resp);
    }
}
