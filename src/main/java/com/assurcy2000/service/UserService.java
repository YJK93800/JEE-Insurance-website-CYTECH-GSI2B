package com.assurcy2000.service;

import com.assurcy2000.dao.UserDao;
import com.assurcy2000.dao.impl.UserDaoImpl;
import com.assurcy2000.model.User;
import com.assurcy2000.util.Tx;
import org.mindrot.jbcrypt.BCrypt;

public class UserService {
    private  final UserDao users;

    public UserService(){
        this(new UserDaoImpl());
    }
    public UserService(UserDao users) {
        this.users = users;
    }

    /** @throws IllegalStateException si l'utilisateur n'existe plus en base */
    public User getById(Long id){
        return Tx.call(() -> users.findById(id)
                .orElseThrow(() -> new IllegalStateException("Utilisateur " + id + " introuvable")));
    }

    public void updateInfos(){

    }

    /**
     * Change le mot de passe de l'utilisateur.
     * @param userId L'ID de l'utilisateur connecté
     * @param oldPassword L'ancien mot de passe de l'utilisateur, utilisé pour voir si c'est n'est pas le même entrée dans le nouveau
     * @param newPassword Le nouveau mot de passe de l'utilisateur, qui sera mis a jour dans la BDD
     * @throws FieldException si le mot de passe actuel est faux
     */
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        Tx.run(() -> {
            User user = users.findById(userId)
                    .orElseThrow(() -> new IllegalStateException("Utilisateur " + userId + " introuvable"));

            if (!BCrypt.checkpw(oldPassword, user.getPassword())) {
                throw new FieldException("oldPassword", "Mot de passe actuel incorrect.");
            }
            user.setPassword(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
        });
    }
}
