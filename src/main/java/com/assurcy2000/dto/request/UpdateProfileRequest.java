package com.assurcy2000.dto.request;

/**
 * Formulaire « Informations personnelles » de profile.jsp.
 * Seules les coordonnées sont modifiables par l'utilisateur : l'identité
 * (nom, prénom, email, date de naissance) figure sur les contrats et sinistres,
 * elle se corrige par un agent ou un administrateur.
 * phone et address valent null quand le champ est laissé vide.
 */
public record UpdateProfileRequest(
        String phone,
        String address) {
}
