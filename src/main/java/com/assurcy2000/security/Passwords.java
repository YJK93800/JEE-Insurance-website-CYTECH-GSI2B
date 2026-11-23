package com.assurcy2000.security;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Hachage et vérification des mots de passe (BCrypt), au même endroit.
 * Les services n'appellent jamais BCrypt directement : pour changer d'algorithme
 * ou de coût, on ne modifie que cette classe.
 */
public final class Passwords {

    /**
     * Hash d'un mot de passe que personne ne connaît. Quand l'email n'existe pas,
     * on vérifie quand même le mot de passe contre ce hash : la réponse prend le même temps
     * que pour un vrai compte, ce qui empêche de deviner les emails inscrits en mesurant ce temps.
     */
    private static final String DUMMY_HASH = hash("mot-de-passe-factice");

    private Passwords() {}

    /** Hash BCrypt avec un sel aléatoire. Calcul volontairement lent : à faire hors transaction. */
    public static String hash(String plain) {
        return BCrypt.hashpw(plain, BCrypt.gensalt());
    }

    /**
     * Le mot de passe correspond-il au hash ?
     * hash null (compte introuvable) : la vérification est faite contre DUMMY_HASH pour le même temps
     * de calcul, et le résultat est toujours false. Un hash mal formé donne aussi false.
     */
    public static boolean matches(String plain, String hash) {
        if (plain == null) return false;
        try {
            boolean ok = BCrypt.checkpw(plain, hash == null ? DUMMY_HASH : hash);
            return ok && hash != null;
        } catch (IllegalArgumentException e) { // hash mal formé
            return false;
        }
    }
}
