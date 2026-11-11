package fr.cytech.assurance.dao.impl;

import fr.cytech.assurance.dao.BaseDao;
import fr.cytech.assurance.util.HibernateUtil;
import org.hibernate.Session;

import java.util.List;
import java.util.Optional;

public abstract class HibernateDao<T, ID> implements BaseDao<T, ID> {
    private final Class<T> type;
    protected HibernateDao(Class<T> type) {
        this.type = type;
    }

    protected Session session() {
        return HibernateUtil.getSessionFactory().getCurrentSession();
    }

    public T save(T e) {
        session().persist(e);
        return  e;
    }

    public Optional<T> findById(ID id){
        return Optional.ofNullable(session().find(type, id));
    }

    public List<T> findAll(){
        return session().createSelectionQuery("from " + type.getSimpleName(), type).getResultList();
    }

    public T update(T e){
        return session().merge(e);
    }

    public void delete(T e){
        session().remove(session().contains(e) ? e : session().merge(e));
    }
}
