package com.assurcy2000.dto.request;

import java.time.LocalDate;

/**
 * Formulaire « Informations personnelles » de profile.jsp.
 * L'utilisateur modifie lui-même son email, sa date de naissance, son téléphone et son adresse.
 * Le nom et le prénom restent en lecture seule : ils se corrigent par un agent ou un administrateur.
 * Valeurs déjà vérifiées et nettoyées par la servlet (classe Validation) :
 * email en minuscules ; phone et address valent null quand le champ est laissé vide.
 */
public record UpdateProfileRequest(
        String email,
        LocalDate birthDate,
        String phone,
        String address) {
}
