package com.assurcy2000.dao;

import com.assurcy2000.persistence.HibernateUtil;
import org.hibernate.Session;

import java.io.Serializable;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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

    /**
     * Nombre d'entités pour chaque valeur d'un champ enum (ex. combien de contrats ACTIF, EXPIRE…).
     * Toutes les valeurs de l'enum sont présentes dans le résultat, à 0 si aucune entité ne l'a.
     * field : nom du champ Java (ex. "status"). Il est concaténé dans la requête HQL : n'appeler
     * cette méthode qu'avec une constante écrite dans le code, jamais avec une saisie utilisateur.
     */
    protected <E extends Enum<E>> Map<E, Long> countByEnum(Class<E> enumType, String field) {
        return query(s -> {
            Map<E, Long> result = new EnumMap<>(enumType);
            for (E value : enumType.getEnumConstants()) {
                result.put(value, 0L);
            }
            s.createQuery("select e." + field + ", count(e) from " + entityClass.getSimpleName()
                            + " e group by e." + field, Object[].class)
                    .getResultList()
                    .forEach(row -> result.put(enumType.cast(row[0]), (Long) row[1]));
            return result;
        });
    }

    /**
     * Ajoute (add = true) ou retire un élément d'une collection many-to-many de l'entité,
     * ex. les garanties d'un contrat. Les deux objets sont chargés dans la transaction en cours ;
     * Hibernate écrit le changement dans la table de liaison à la validation.
     */
    protected <O> void changeLink(ID ownerId, Class<O> otherType, Object otherId,
                                  Function<T, Set<O>> collection, boolean add) {
        execute(s -> {
            T owner = s.find(entityClass, ownerId);
            O other = s.find(otherType, otherId);
            if (owner == null || other == null) {
                throw new IllegalArgumentException(entityClass.getSimpleName() + " " + ownerId + " ou "
                        + otherType.getSimpleName() + " " + otherId + " introuvable");
            }
            if (add) collection.apply(owner).add(other);
            else collection.apply(owner).remove(other);
        });
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
