package com.assurcy2000.dao;

import com.assurcy2000.persistence.HibernateUtil;
import org.hibernate.Session;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * CRUD commun à tous les DAO Hibernate.
 * Aucun DAO n'ouvre de transaction : ils travaillent sur la session courante,
 * ouverte par le service via TransactionRunner. Appeler un DAO hors transaction
 * lève une erreur Hibernate.
 */
public abstract class BaseDao<T, ID extends Serializable> {

    private final Class<T> entityClass;

    protected BaseDao(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    public T save(T entity) {
        session().persist(entity);
        return entity;
    }

    public T update(T entity) {
        return session().merge(entity);
    }

    public void delete(T entity) {
        Session s = session();
        s.remove(s.contains(entity) ? entity : s.merge(entity));
    }

    public Optional<T> findById(ID id) {
        return Optional.ofNullable(session().find(entityClass, id));
    }

    /**
     * Référence vers une entité dont on connaît l'id, sans la charger : aucune requête SQL.
     * Sert à remplir une clé étrangère (ex. contract.setAgent(...)) quand on sait que la ligne existe,
     * comme l'utilisateur connecté. Seul getId() est utilisable sans transaction ; lire un autre champ
     * déclenche le chargement, donc uniquement dans la transaction en cours.
     */
    public T getReference(ID id) {
        return session().getReference(entityClass, id);
    }

    public List<T> findAll() {
        return session().createQuery("from " + entityClass.getSimpleName(), entityClass).getResultList();
    }

    /** Session de la transaction en cours (celle ouverte par le service). */
    protected Session session() {
        return HibernateUtil.getSessionFactory().getCurrentSession();
    }

    /** Raccourci pour les requêtes des sous-classes. */
    protected <R> R query(Function<Session, R> work) {
        return work.apply(session());
    }

    /** Raccourci pour les modifications des sous-classes. */
    protected void execute(Consumer<Session> work) {
        work.accept(session());
    }
}
