package com.assurcy2000.dao;

import com.assurcy2000.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class BaseDao<T, ID extends Serializable> {

    private final Class<T> entityClass;

    protected BaseDao(Class<T> entityClass) {
        this.entityClass = entityClass;
    }


    public T save(T entity) {
        runInTransaction(s -> s.persist(entity));
        return entity;
    }

    public T update(T entity) {
        return inTransaction(s -> s.merge(entity));
    }

    public void delete(T entity) {
        runInTransaction(s -> s.remove(s.contains(entity) ? entity : s.merge(entity)));
    }

    public Optional<T> findById(ID id) {
        return query(s -> Optional.ofNullable(s.find(entityClass, id)));
    }

    public List<T> findAll() {
        return query(s -> s.createQuery(
                "from " + entityClass.getSimpleName(), entityClass).getResultList());
    }

    protected <R> R query(Function<Session, R> work) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return work.apply(session);
        }
    }

    protected <R> R inTransaction(Function<Session, R> work) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            R result = work.apply(session);
            tx.commit();
            return result;
        } catch (RuntimeException e) {
            if (tx != null && tx.isActive()) tx.rollback();
            throw e;
        }
    }

    protected void runInTransaction(Consumer<Session> work) {
        inTransaction(s -> { work.accept(s); return null; });
    }
}